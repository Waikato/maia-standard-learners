//package maia.ml.learner.standard.hoeffdingtree
//
//import moa.classifiers.trees.RandomHoeffdingTree
//import maia.ml.dataset.DataRow
//import maia.ml.dataset.DataStream
//import maia.ml.dataset.headers.DataColumnHeaders
//import maia.ml.dataset.headers.DataColumnHeadersView
//import maia.ml.dataset.headers.ensureOwnership
//import maia.ml.dataset.type.DataRepresentation
//import maia.ml.learner.AbstractLearner
//import maia.ml.dataset.type.DataType
//import maia.ml.dataset.util.*
//import maia.ml.learner.standard.Estimator
//import maia.ml.learner.type.*
//import maia.util.asIterable
//import maia.util.datastructure.OrderedHashSet
//import maia.util.enumerate
//import java.util.*
//import kotlin.collections.ArrayList
//import kotlin.math.max
//import maia.ml.dataset.type.standard.Nominal
//import maia.ml.dataset.type.standard.Numeric
//
//val RANDOM_HOEFFDING_TREE_LEARNER_TYPE = intersectionOf(SingleTarget, Classifier, NoMissingTargets)
//
//open class Ozabag(
//    public val ensembleSize: Int = 10,
//    val targetIndex : Int,
//    var ensemble: ArrayList<RandomHoeffdingTree> = arrayListOf()
//) : AbstractLearner<DataStream<*>>(
//    RANDOM_HOEFFDING_TREE_LEARNER_TYPE,
//    DataStream::class
//)
//{
//    private var numClasses : Int = 0
//    private lateinit var distributions : Array<Array<Estimator>>
//    private lateinit var classDistribution : Estimator
//
//    private lateinit var targetType : Nominal<*, *, *, *>
//
//    internal lateinit var classType: Nominal<*, *, *, *>
//        private set
//
//    internal var classColumnIndex: Int = -1
//        private set
//
//
//    override fun performInitialisation(headers: DataColumnHeaders): Triple<DataColumnHeaders, DataColumnHeaders, LearnerType> {
//        val (classIndex, classHeader) = headers
//            .iterator()
//            .enumerate()
//            .asIterable()
//            .first { it.second.type is maia.ml.dataset.type.standard.Nominal<*, *, *, *> }
//
//        for(i in 0..ensembleSize)
//        {
//            val learner = RandomHoeffdingTree()
//            learner.initialise(headers)
//            ensemble.add(learner)
//        }
//        val targetType = headers[targetIndex].type
//
//        if (targetType !is Nominal<*, *, *, *>)
//            throw Exception("OzaBag only supports nominal classes")
//
//        this.targetType = targetType
//
//        classColumnIndex = classIndex
//        classType = classHeader.type as Nominal<*, *, *, *>
//
//        return Triple(
//            DataColumnHeadersView(headers, headers.allColumnsExcept(targetIndex)),
//            DataColumnHeadersView(headers, OrderedHashSet(targetIndex)),
//            HOEFFDING_TREE_LEARNER_TYPE
//        )
//    }
//
//
////    override fun performEval(trainingDataset: DataStream<*>): Double {
////        var numberSamplesCorrect: Double = 0.0
////        var numberSamples: Double = 0.0
////
////        for (row in trainingDataset.rowIterator()) {
////            if (row.getValueExternal(classColumnIndex) as String == performPredict(row).formatStringSimple()
////                    .dropLast(1)
////            ) {
////                numberSamplesCorrect += 1
////            }
////
////            numberSamples += 1
////        }
////
////        return numberSamplesCorrect / numberSamples
////
////    }
//
//    protected open fun trainOnRow(
//        row : DataRow
//    ) {
//
//        for(i in 0..this.ensemble.size)
//        {
//            val k: Int? = this.classifierRandom?.let { MiscUtils.poisson(1.0, it) }
//            if (k != null) {
//                if(k > 0 ) {
////                    val weightedInst2: MutableDataRow = MutableDataRow.clone(row)
//                    val weightedInst: DataRow = row as DataRow     //Can I do this?
//
//                    weightedInst.weight = row.weight * k    //Unable to change weight as it is defined in constructor
//                    this.ensemble[i].predict(weightedInst)
//
//                }
//            }
//
//        }
//    }
//
//    //Get votes for instance
//    override fun performPredict(
//        row : DataRow
//    ) : DataRow {
//
////        val combinedVote = DoubleVector()
//        val combinedVote = ArrayList<DoubleArray>()
//        var max = 0.0
//        var maxIndex = 0
//
//        for (i in 0 until this.ensemble.size) {
//            val vote = distributionForInstance(this.ensemble[i].predict(row))
//
//            if(vote.sum() > 0.0)
//            {
//                normalise(vote)
//                combinedVote.add(vote)
//            }
//            for (i in combinedVote.indices) {
//                if (vote[i] > max) {
//                    maxIndex = i
//                    max = vote[i]
//                }
//            }
//        }
//        val selection = if (max > 0) targetType[maxIndex] else targetType.random()
//
//        return object : DataRow {
//            override val headers : DataColumnHeaders = predictOutputHeaders
//            override fun <T> getValue(
//                representation : DataRepresentation<*, *, out T>
//            ) : T = headers.ensureOwnership(representation) {
//                return convert(selection, targetType.canonicalRepresentation)
//            }
//        }
//    }
//
//    private fun getAttributeDoubleValue(attributeType : DataType<*, *>, row: DataRow) : Double {
//        return when (attributeType) {
//            is Numeric<*, *> -> row.getValue(attributeType.canonicalRepresentation)
//            is Nominal<*, *, *, *> -> row.getValue(attributeType.indexRepresentation).toDouble()
//            else -> throw Exception("All attributes must be numeric or nominal")
//        }
//    }
//
//    fun distributionForInstance(row : DataRow) : DoubleArray {
//
//        val probs = DoubleArray(numClasses) { classDistribution.getProbability(it.toDouble()) }
//
//        for (attIndex in 0 until trainHeaders.numColumns - 1) {
//            val nonClassAttributeIndex = if (attIndex < targetIndex) attIndex else attIndex + 1
//            val attr = trainHeaders.get(nonClassAttributeIndex)
//            var temp : Double
//            var max = 0.0
//
//            for (j in 0 until numClasses) {
//                temp = max(1e-75, distributions[attIndex][j].getProbability(getAttributeDoubleValue(attr.type, row)))
//                probs[j] *= temp
//                if (probs[j] > max) max = probs[j]
//                if (probs[j].isNaN()) throw Exception("NaN returned from estimator for attribute ${attr.name}:\n" +
//                        "${distributions[attIndex][j]}")
//            }
//
//            if (max > 0.0 && max < 1e-75) for (j in 0 until numClasses) probs[j] *= 1e75
//        }
//
////        normalise(probs)
//        return probs
//    }
//
//    fun normalise(doubles : DoubleArray) {
//        var sum = 0.0
//        for (d in doubles) sum += d
//        if (sum.isNaN()) throw Exception("Can't normalise array. Sum is NaN")
//        if (sum == 0.0) throw Exception("Can't normalise array. Sum is zero")
//        for (i in doubles.indices) doubles[i] /= sum
//    }
//
//
//    override fun performTrain(
//        trainingDataset : DataStream<*>
//    ) {
//        trainingDataset.rowIterator().forEachRemaining {
//            trainOnRow(it)
//        }
//    }
//
//}
