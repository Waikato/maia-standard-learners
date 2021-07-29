package māia.ml.learner.standard.hoeffdingtree.node

import māia.ml.dataset.DataRow
import māia.ml.dataset.type.DataTypeWithMissingValues
import māia.ml.dataset.type.Nominal
import māia.ml.dataset.type.Numeric
import māia.ml.dataset.util.ensureMissingValues
import māia.ml.dataset.util.indexOfInternalUnchecked
import māia.ml.dataset.util.isPossiblyMissing
import māia.ml.dataset.util.weight
import māia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import māia.ml.learner.standard.hoeffdingtree.observer.AttributeClassObserver
import māia.ml.learner.standard.hoeffdingtree.observer.NullAttributeClassObserver
import māia.ml.learner.standard.hoeffdingtree.observer.SplitSuggestion
import māia.ml.learner.standard.hoeffdingtree.split.criterion.SplitCriterion
import māia.ml.learner.standard.hoeffdingtree.split.test.SplitTest
import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
import māia.util.Absent
import māia.util.Optional
import māia.util.asIterable
import māia.util.enumerate
import māia.util.inlineRangeForLoop
import māia.util.map
import māia.util.maxReducer
import māia.util.times
import java.text.DecimalFormat

sealed class Node(
    protected val owner: HoeffdingTree,
    val observedClassDistribution: ObservedClassDistribution
) {
    constructor(
        owner: HoeffdingTree,
        size: Int
    ): this(owner, ObservedClassDistribution(size))

    abstract val isLeaf: Boolean

    abstract fun filterRowToLeaf(
        row: DataRow,
        parentBranch: ParentBranch?
    ): FoundNode

    abstract fun observedClassDistributionAtLeavesReachableThroughThisNode(
        initial: ObservedClassDistribution?
    ): ObservedClassDistribution

    abstract fun getClassVotes(
        row: DataRow
    ): ObservedClassDistribution

    abstract val subTreeDepth: Int

    abstract fun describeSubtree(out: StringBuilder, indent: Int)
}


open class SplitNode(
    owner: HoeffdingTree,
    observedClassDistribution : ObservedClassDistribution,
    val splitTest: SplitTest,
    numChildren: Int = 0
): Node(owner, observedClassDistribution) {

    constructor(
        owner: HoeffdingTree,
        size: Int,
        splitTest: SplitTest,
        numChildren: Int = 0
    ): this(
        owner,
        ObservedClassDistribution(size),
        splitTest,
        numChildren
    )

    private val _children: MutableMap<Int, Node> = HashMap(numChildren)

    val children: Iterator<Node>
        get() = _children.values.iterator()

    operator fun get(index: Int): Node? = this._children[index]

    internal operator fun set(index : Int, child: Node) {
        val maxBranches = splitTest.maxBranches
        if (maxBranches !is Absent && index >= maxBranches.get())
            throw IndexOutOfBoundsException(index)
        _children[index] = child
    }

    override val isLeaf : Boolean = false

    override fun filterRowToLeaf(
        row : DataRow,
        parentBranch : ParentBranch?
    ) : FoundNode {
        val childIndex = rowChildIndex(row);

        if (childIndex is Absent) return FoundNode(this, parentBranch)

        val i = childIndex.get()
        val nextParentBranch = ParentBranch(this, i)

        return this._children[i]?.filterRowToLeaf(row, nextParentBranch)
            ?: FoundNode(null, nextParentBranch)
    }

    override fun observedClassDistributionAtLeavesReachableThroughThisNode(
        initial: ObservedClassDistribution?
    ) : ObservedClassDistribution {
        val result = initial ?: observedClassDistribution.clone()
        if (initial != null) result += observedClassDistribution
        children.forEach {
            it.observedClassDistributionAtLeavesReachableThroughThisNode(result)
        }
        return result
    }

    override fun getClassVotes(
        row : DataRow
    ) : ObservedClassDistribution {
        return observedClassDistribution
    }

    override val subTreeDepth : Int
        get() = this.children.map {
            it.subTreeDepth
        }.asIterable().reduce(maxReducer<Int>()::reduce) + 1

    override fun describeSubtree(out : StringBuilder, indent : Int) {
        for ((branch, child) in children.enumerate()) {
            out
                .append(" " * indent, "if ")
                .append(splitTest.describeConditionForBranch(branch, owner))
                .append(": \n")
            child.describeSubtree(out, indent + 2)
        }
    }

    fun rowChildIndex(row: DataRow): Optional<Int> {
        return this.splitTest.branchForRow(row)
    }
}

open class LearningNode(
    owner: HoeffdingTree,
    observedClassDistribution: ObservedClassDistribution,
    isActive: Boolean
) : Node(
    owner,
    observedClassDistribution
) {
    constructor(
        owner: HoeffdingTree,
        size: Int,
        isActive: Boolean
    ): this(owner, ObservedClassDistribution(size), isActive)

    protected lateinit var attributeIndices: IntArray
    protected lateinit var attributeObservers: Array<AttributeClassObserver<*>>

    var isActive: Boolean = isActive
        internal set

    private var weightSeenAtLastEvaluation: Double = observedClassDistribution.totalWeightSeen

    fun evaluate(block: (Double, Double) -> Boolean) {
        val currentWeightSeen = observedClassDistribution.totalWeightSeen
        val evaluationPassed = block(currentWeightSeen, weightSeenAtLastEvaluation)
        if (evaluationPassed) weightSeenAtLastEvaluation = currentWeightSeen
    }

    protected fun initAttributeObservers() {
        if (this::attributeObservers.isInitialized) return
        attributeIndices = IntArray(owner.predictInputHeaders.numColumns)
        attributeObservers = Array(owner.predictInputHeaders.numColumns) {
            val attributeIndex = if (it < owner.classColumnIndex) it else it + 1
            val type = owner.predictInputHeaders.getColumnHeader(it).type
            val observer = if (isPossiblyMissing<Nominal<*>>(type)) {
                owner.nominalEstimatorFactory(
                    type.ensureMissingValues() as DataTypeWithMissingValues<*, String, Nominal<*>, *, *>,
                    owner.classType
                )
            } else if (isPossiblyMissing<Numeric<*>>(type)) {
                owner.numericEstimatorFactory(
                    type.ensureMissingValues() as DataTypeWithMissingValues<*, Double, Numeric<*>, *, *>,
                    owner.classType
                )
            } else {
                throw Exception("Attribute type must be numeric or nominal")
            }
            attributeIndices[it] = attributeIndex
            observer
        }
    }

    open fun learnFromRow(
        row : DataRow
    ) {
        initAttributeObservers()

        val classValue = row.getColumn(owner.classColumnIndex)
        val classIndex = owner.classType.indexOfInternalUnchecked(classValue)

        val rowWeight = row.weight

        observedClassDistribution[classIndex] += rowWeight

        inlineRangeForLoop(attributeObservers.size) {
            val attributeIndex = attributeIndices[it]
            val observer = attributeObservers[it]
            val value = row.getColumn(attributeIndex)
            observer.observe(
                value,
                classIndex,
                rowWeight
            )
        }
    }

    fun getBestSplitSuggestions(
        criterion : SplitCriterion,
        noPrePrune: Boolean,
        binarySplitsOnly: Boolean
    ): Array<SplitSuggestion> {
        val bestSuggestions = ArrayList<SplitSuggestion>()
        val preSplitDist = observedClassDistribution

        if (!noPrePrune) {
            bestSuggestions += SplitSuggestion(
                null,
                arrayOf(),
                criterion.getMeritOfSplit(preSplitDist, arrayOf(preSplitDist))
            )
        }

        inlineRangeForLoop(attributeObservers.size) {
            val attributeIndex = attributeIndices[it]
            val observer = attributeObservers[it]
            val bestSuggestion: SplitSuggestion? = observer.getBestEvaluatedSplitSuggestion(
                criterion,
                preSplitDist,
                attributeIndex,
                binarySplitsOnly
            )

            if (bestSuggestion !== null) bestSuggestions.add(bestSuggestion)
        }

        return bestSuggestions.toTypedArray().apply { sort() }
    }

    override val isLeaf : Boolean = true

    override fun filterRowToLeaf(
        row : DataRow,
        parentBranch : ParentBranch?
    ) : FoundNode {
        return FoundNode(this, parentBranch)
    }

    override fun observedClassDistributionAtLeavesReachableThroughThisNode(
        initial : ObservedClassDistribution?
    ) : ObservedClassDistribution {
        val result = initial ?: observedClassDistribution.clone()
        if (initial != null) result += observedClassDistribution
        return result
    }

    override fun getClassVotes(
        row : DataRow
    ) : ObservedClassDistribution {
        return observedClassDistribution
    }

    override val subTreeDepth : Int = 0

    fun observerIndex(attributeIndex: Int): Int {
        return if (attributeIndices[attributeIndex] != attributeIndex)
            attributeIndex - 1
        else
            attributeIndex
    }

    open fun disableAttribute(index: Int) {
        attributeObservers[observerIndex(index)] = NullAttributeClassObserver(owner.classType)
    }

    override fun describeSubtree(out : StringBuilder, indent : Int) {
        val maxClassIndex = observedClassDistribution.maxClassIndex
        out
            .append(" " * indent, "Leaf [")
            .append(owner.predictOutputHeaders.getColumnHeader(0).name)
            .append(":class] = <")
            .append(owner.classType[maxClassIndex])
            .append(":class ${maxClassIndex + 1}> weights: ")
            .append(
                observedClassDistribution.array.take(owner.treeRoot.observedClassDistribution.nonZeroLength).joinToString(
                    "|", "{", "}\n"
                ) {
                    FORMATTER.format(it)
                }
            )
    }

    companion object {
        val FORMATTER = DecimalFormat().apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 3
        }
    }
}
