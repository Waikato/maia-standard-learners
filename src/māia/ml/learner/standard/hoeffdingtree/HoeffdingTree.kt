package māia.ml.learner.standard.hoeffdingtree

import māia.ml.dataset.DataRow
import māia.ml.learner.standard.hoeffdingtree.node.Node
import māia.ml.learner.standard.hoeffdingtree.observer.GaussianNumericAttributeClassObserver
import māia.ml.learner.standard.hoeffdingtree.observer.NominalAttributeClassObserver
import māia.ml.learner.standard.hoeffdingtree.observer.NumericAttributeClassObserver
import māia.ml.dataset.DataStream
import māia.ml.dataset.WithColumns
import māia.ml.dataset.headers.DataColumnHeaders
import māia.ml.dataset.headers.DataColumnHeadersView
import māia.ml.dataset.headers.ensureOwnership
import māia.ml.dataset.type.DataRepresentation
import māia.ml.dataset.type.standard.Nominal
import māia.ml.dataset.type.standard.Numeric
import māia.ml.dataset.util.allColumnsExcept
import māia.ml.dataset.view.readOnlyViewColumns
import māia.ml.learner.AbstractLearner
import māia.ml.learner.standard.hoeffdingtree.node.FoundNode
import māia.ml.learner.standard.hoeffdingtree.node.LearningNode
import māia.ml.learner.standard.hoeffdingtree.node.LearningNodeNB
import māia.ml.learner.standard.hoeffdingtree.node.LearningNodeNBAdaptive
import māia.ml.learner.standard.hoeffdingtree.node.ParentBranch
import māia.ml.learner.standard.hoeffdingtree.node.SplitNode
import māia.ml.learner.standard.hoeffdingtree.split.criterion.InfoGainSplitCriterion
import māia.ml.learner.standard.hoeffdingtree.split.criterion.SplitCriterion
import māia.ml.learner.standard.hoeffdingtree.split.test.SplitTest
import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
import māia.ml.learner.standard.hoeffdingtree.util.hoeffdingBound
import māia.ml.learner.type.Classifier
import māia.ml.learner.type.LearnerType
import māia.ml.learner.type.NoMissingTargets
import māia.ml.learner.type.SingleTarget
import māia.ml.learner.type.classLearnerType
import māia.ml.learner.type.intersectionOf
import māia.util.asIterable
import māia.util.datastructure.OrderedHashSet
import māia.util.enumerate
import māia.util.property.classlevel.override


typealias NominalEstimatorFactory = (
    Nominal<*, *, *, *>,
    Nominal<*, *, *, *>
) -> NominalAttributeClassObserver

typealias NumericEstimatorFactory = (
    Numeric<*, *>,
    Nominal<*, *, *, *>
) -> NumericAttributeClassObserver

typealias SplitCriterionFactory = () -> SplitCriterion

val HOEFFDING_TREE_LEARNER_TYPE = intersectionOf(SingleTarget, Classifier, NoMissingTargets)

enum class LeafPredictor {
    MAJORITY_CLASS,
    NAIVE_BAYES,
    NAIVE_BAYES_ADAPTIVE
}

open class HoeffdingTree(
    public val maxBytesSize: Int = 33554432,
    public val gracePeriod: Int = 200,
    private val splitCriterion: SplitCriterion = InfoGainSplitCriterion(),
    public val splitConfidence: Double = 0.0000001,
    public val tieThreshold: Float = 0.05f,
    public val binarySplitsOnly: Boolean = false,
    public val removePoorAttributes: Boolean = false,
    public val noPrePrune: Boolean = false,
    nominalEstimatorFactory: NominalEstimatorFactory? = null,
    numericEstimatorFactory: NumericEstimatorFactory? = null,
    public val leafPredictor : LeafPredictor = LeafPredictor.NAIVE_BAYES_ADAPTIVE,
    public val nbThreshold: Int = 0
) : AbstractLearner<DataStream<*>>(
    HOEFFDING_TREE_LEARNER_TYPE,
    DataStream::class
) {
    private var decisionNodeCount: Int = 0
    private var activeLeafNodeCount: Int = 1
    private var inactiveLeafNodeCount: Int = 0
    private var growthAllowed: Boolean = true

    val nodeCount: Int
        get() = decisionNodeCount + activeLeafNodeCount + inactiveLeafNodeCount

    lateinit var treeRoot: Node
        protected set

    internal var classColumnIndex: Int = -1
        private set
    internal lateinit var classType: Nominal<*, *, *, *>
        private set

    val nominalEstimatorFactory: NominalEstimatorFactory =
        nominalEstimatorFactory ?: ::NominalAttributeClassObserver

    val numericEstimatorFactory: NumericEstimatorFactory =
        numericEstimatorFactory ?: ::GaussianNumericAttributeClassObserver

    override fun performInitialisation(
        headers : DataColumnHeaders
    ) : Triple<DataColumnHeaders, DataColumnHeaders, LearnerType> {

        val (classIndex, classHeader) = headers
            .iterator()
            .enumerate()
            .asIterable()
            .first { it.second.type is Nominal<*, *, *, *> }

        classColumnIndex = classIndex
        classType = classHeader.type as Nominal<*, *, *, *>

        treeRoot = newLearningNode()

        return Triple(
            DataColumnHeadersView(headers, headers.allColumnsExcept(classIndex)),
            DataColumnHeadersView(headers, OrderedHashSet(classIndex)),
            HOEFFDING_TREE_LEARNER_TYPE
        )
    }

    protected fun trainOnRow(
        row: DataRow
    ) {
        val foundNode = treeRoot.filterRowToLeaf(row, null)
        val leafNode = foundNode.node ?: newLearningNode().also {
            foundNode.parentBranch!!.setChild(it)
            activeLeafNodeCount++
        }

        if (leafNode !is LearningNode) return

        leafNode.learnFromRow(row)

        if (growthAllowed && leafNode.isActive) {
            leafNode.evaluate { current, last ->
                if (current - last >= gracePeriod) {
                    attemptToSplit(
                        FoundNode(
                            leafNode,
                            foundNode.parentBranch
                        )
                    )
                    true
                } else
                    false
            }
        }
    }

    override fun performTrain(
        trainingDataset : DataStream<*>
    ) {
        trainingDataset.rowIterator().forEachRemaining {
            trainOnRow(it)
        }
    }

    override fun performPredict(
        row : DataRow
    ) : DataRow {
        val foundNode = treeRoot.filterRowToLeaf(row, null)

        val leafNode = foundNode.node ?: foundNode.parentBranch!!.parent

        val predictions = Array(predictOutputHeaders.numColumns) {
            leafNode.getClassVotes(row).maxClassIndex.coerceAtLeast(0)
        }

        return object : DataRow {
            override val headers : DataColumnHeaders = predictOutputHeaders
            override fun <T> getValue(
                representation : DataRepresentation<*, *, out T>
            ) : T = headers.ensureOwnership(representation) {
                return convert(predictions[columnIndex], classType.indexRepresentation)
            }
        }
    }

    fun activateLearningNode(
        toActivate: LearningNode
    ) {
        if (toActivate.isActive) return
        toActivate.isActive = true
        activeLeafNodeCount++
        inactiveLeafNodeCount--
    }

    fun attemptToSplit(foundNode: FoundNode) {
        val node = foundNode.node

        if (node !is LearningNode || node.observedClassDistribution.isPure) return

        val bestSplitSuggestions = node.getBestSplitSuggestions(
            splitCriterion,
            noPrePrune,
            binarySplitsOnly
        )

        var shouldSplit = false

        if (bestSplitSuggestions.size < 2)
            shouldSplit = bestSplitSuggestions.isNotEmpty()
        else {
            val hoeffdingBound = hoeffdingBound(
                splitCriterion.getRangeOfMerit(node.observedClassDistribution),
                splitConfidence,
                node.observedClassDistribution.totalWeightSeen
            )

            val bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.size - 1]
            val secondBestSuggestion = bestSplitSuggestions[bestSplitSuggestions.size - 2]

            if ((bestSuggestion.merit - secondBestSuggestion.merit > hoeffdingBound) ||
                (hoeffdingBound < tieThreshold)) {
                shouldSplit = true
            }

            if (removePoorAttributes) {
                val poorAttributes = HashSet<Int>()
                for (suggestion in bestSplitSuggestions) {
                    if (suggestion.splitTest === null) continue
                    val splitAtts = suggestion.splitTest.attributesTestDependsOn
                    if (splitAtts.size == 1) {
                        if (bestSuggestion.merit - suggestion.merit > hoeffdingBound) {
                            poorAttributes.add(splitAtts[0])
                        }
                    }
                }

                for (poorAttribute in poorAttributes) node.disableAttribute(poorAttribute)
            }
        }

        if (!shouldSplit) return

        val splitDecision = bestSplitSuggestions.last()

        if (splitDecision.splitTest === null) {
            deactivateLearningNode(node)
        } else {
            val newSplitNode = newSplitNode(
                splitDecision.splitTest,
                foundNode.node.observedClassDistribution,
                splitDecision.numSplits
            )

            for (i in 0 until splitDecision.numSplits) {
                newSplitNode[i] = newLearningNode(
                    splitDecision.resultingClassDistributionForSplit(i)
                )
            }

            activeLeafNodeCount += splitDecision.numSplits - 1
            decisionNodeCount++

            if (foundNode.parentBranch === null) {
                treeRoot = newSplitNode
            } else {
                foundNode.parentBranch.setChild(newSplitNode)
            }
        }
    }

    fun deactivateAllLeaves() {
        TODO("Not yet implemented")
    }

    fun newSplitNode(
        splitTest: SplitTest,
        classObservations: ObservedClassDistribution,
        size: Int
    ): SplitNode {
        return SplitNode(this, classObservations, splitTest, size)
    }

    fun deactivateLearningNode(
        toDeactivate: LearningNode
    ) {
        if (!toDeactivate.isActive) return
        toDeactivate.isActive = false
        activeLeafNodeCount--
        inactiveLeafNodeCount++
    }

    val learningNodes: Array<FoundNode>
        get() = ArrayList<FoundNode>().also {
            findLearningNodes(FoundNode(treeRoot, null), it)
        }.toTypedArray()

    fun findLearningNodes(
        foundNode: FoundNode,
        found: MutableList<FoundNode>
    ) {
        if (foundNode.node is LearningNode) found.add(foundNode)
        if (foundNode.node is SplitNode) {
            for ((index, child) in foundNode.node.children.enumerate())
                findLearningNodes(
                    FoundNode(child, ParentBranch(foundNode.node, index)),
                    found
                )
        }
    }

    fun newLearningNode(
        initialClassObservations: ObservedClassDistribution? = null
    ): LearningNode {
        val observations = initialClassObservations ?: ObservedClassDistribution(classType.numCategories)

        return when (leafPredictor) {
            LeafPredictor.MAJORITY_CLASS -> LearningNode(this, observations, true)
            LeafPredictor.NAIVE_BAYES -> LearningNodeNB(this, observations, true)
            LeafPredictor.NAIVE_BAYES_ADAPTIVE -> LearningNodeNBAdaptive(this, observations, true)
        }
    }

    fun getModelDescription(out: StringBuilder, indent: Int) {
        treeRoot.describeSubtree(out, indent)
    }

    companion object {
        init {
            HoeffdingTree::classLearnerType.override(
                HOEFFDING_TREE_LEARNER_TYPE
            )
        }
    }
}
