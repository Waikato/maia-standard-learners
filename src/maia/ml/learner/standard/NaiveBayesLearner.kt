package maia.ml.learner.standard

import maia.configure.Configurable
import maia.configure.Configuration
import maia.configure.ConfigurationElement
import maia.configure.ConfigurationItem
import maia.configure.asReconfigureBlock
import maia.ml.dataset.DataBatch
import maia.ml.dataset.DataColumn
import maia.ml.dataset.DataRow
import maia.ml.dataset.headers.DataColumnHeaders
import maia.ml.dataset.headers.DataColumnHeadersView
import maia.ml.dataset.headers.ensureOwnership
import maia.ml.dataset.headers.viewColumns
import maia.ml.dataset.type.DataRepresentation
import maia.ml.dataset.type.DataType
import maia.ml.dataset.type.standard.Nominal
import maia.ml.dataset.type.standard.Numeric
import maia.ml.dataset.util.allColumnsExcept
import maia.ml.learner.AbstractLearner
import maia.ml.learner.factory.ConfigurableLearnerFactory
import maia.ml.learner.type.Classifier
import maia.ml.learner.type.LearnerType
import maia.ml.learner.type.classLearnerType
import maia.util.datastructure.OrderedHashSet
import maia.util.normalProbability
import maia.util.property.classlevel.override
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class NaiveBayesLearner(
        val targetIndex : Int,
        val useKernelEstimator : Boolean,
        val useDiscretization : Boolean
) : AbstractLearner<DataBatch<*>>(
        Classifier,
        DataBatch::class
){

    val DEFAULT_NUM_PRECISION = 0.01

    private var numClasses : Int = 0

    private lateinit var distributions : Array<Array<Estimator>>

    private lateinit var classDistribution : Estimator

    private lateinit var targetType : Nominal<*, *, *, *>

    override fun performInitialisation(
        headers : DataColumnHeaders
    ) : Triple<DataColumnHeaders, DataColumnHeaders, LearnerType> {
        classDistribution = DiscreteEstimator(numClasses, true)
        distributions = emptyArray()

        val targetType = headers[targetIndex].type

        if (targetType !is Nominal<*, *, *, *>)
            throw Exception("NaiveBayes only supports nominal classes")

        this.targetType = targetType

        this.numClasses = targetType.numCategories

        return Triple(
            DataColumnHeadersView(headers, headers.allColumnsExcept(targetIndex)),
            DataColumnHeadersView(headers, OrderedHashSet(targetIndex)),
            Classifier
        )
    }

    override fun performTrain(trainingDataset : DataBatch<*>) {
        // TODO: Implement support
        if (useDiscretization)
            throw NotImplementedError("Using discretization with NaiveBayes is not yet supported")

        classDistribution = DiscreteEstimator(numClasses, true)
        distributions = Array(trainHeaders.numColumns - 1) {attIndex ->
            val nonClassAttributeIndex = if (attIndex < targetIndex) attIndex else attIndex + 1
            val attribute = trainHeaders[nonClassAttributeIndex]
            val attributeType = attribute.type

            var numPrecision = DEFAULT_NUM_PRECISION
            if (trainingDataset.numRows > 0 && attributeType is Numeric<*, *>) {
                val column = trainingDataset.getColumn(attributeType.canonicalRepresentation)
                val sorted = DoubleArray(column.numRows) {
                    column.getRow(it)
                }
                sorted.sort()
                var lastVal = sorted[0]
                var deltaSum = 0.0
                var distinct = 0
                for (currentVal in sorted) {
                    if (currentVal != lastVal) {
                        deltaSum += currentVal - lastVal
                        lastVal = currentVal
                        distinct++
                    }
                }
                if (distinct > 0) numPrecision = deltaSum / distinct
            }

            Array(numClasses) {
                when (attributeType) {
                    is Numeric -> if (useKernelEstimator) KernelEstimator(numPrecision) else NormalEstimator(numPrecision)
                    is Nominal<*, *, *, *> -> DiscreteEstimator(attributeType.numCategories, true)
                    else -> throw Exception("Attribute type unknown to NaiveBayes")
                }
            }
        }

        for (row in trainingDataset.rowIterator()) updateClassifier(row)
    }

    private fun updateClassifier(row : DataRow) {
        for (attIndex in 0 until trainHeaders.numColumns - 1) {
            val nonClassAttributeIndex = if (attIndex < targetIndex) attIndex else attIndex + 1
            val attribute = trainHeaders[nonClassAttributeIndex]
            val attributeType = attribute.type
            val classValue = row.getValue(targetType.indexRepresentation)
            val doubleValue : Double = getAttributeDoubleValue(attributeType, row)
            distributions[attIndex][classValue].addValue(doubleValue)
        }
    }

    private fun getAttributeDoubleValue(attributeType : DataType<*, *>, row: DataRow) : Double {
        return when (attributeType) {
            is Numeric<*, *> -> row.getValue(attributeType.canonicalRepresentation)
            is Nominal<*, *, *, *> -> row.getValue(attributeType.indexRepresentation).toDouble()
            else -> throw Exception("All attributes must be numeric or nominal")
        }
    }

    override fun performPredict(row : DataRow) : DataRow {
        val dist = distributionForInstance(row)

        var max = 0.0
        var maxIndex = 0

        for (i in dist.indices) {
            if (dist[i] > max) {
                maxIndex = i
                max = dist[i]
            }
        }

        val selection = if (max > 0) targetType[maxIndex] else targetType.random()

        return object : DataRow {
            override val headers : DataColumnHeaders = predictOutputHeaders
            override fun <T> getValue(
                representation : DataRepresentation<*, *, out T>
            ) : T = headers.ensureOwnership(representation) {
                return convert(selection, targetType.canonicalRepresentation)
            }
        }
    }

    fun distributionForInstance(row : DataRow) : Array<Double> {
        // TODO: Implement support
        if (useDiscretization)
            throw NotImplementedError("Using discretization with NaiveBayes is not yet supported")

        val probs = Array(numClasses) { classDistribution.getProbability(it.toDouble()) }

        if (distributions.count() != 0) {
            for (attIndex in 0 until trainHeaders.numColumns - 1) {
                val nonClassAttributeIndex = if (attIndex < targetIndex) attIndex else attIndex + 1
                val attr = trainHeaders[nonClassAttributeIndex]
                var temp : Double
                var max = 0.0

                for (j in 0 until numClasses) {
                    temp = max(
                        1e-75,
                        distributions[attIndex][j].getProbability(getAttributeDoubleValue(attr.type, row))
                    )
                    probs[j] *= temp
                    if (probs[j] > max) max = probs[j]
                    if (probs[j].isNaN()) throw Exception("NaN returned from estimator for attribute ${attr.name}:\n" +
                            "${distributions[attIndex][j]}")
                }

                if (max > 0.0 && max < 1e-75) for (j in 0 until numClasses) probs[j] *= 1e75
            }
            normalise(probs)
        }

        return probs
    }

    fun normalise(doubles : Array<Double>) {
        var sum = 0.0
        for (d in doubles) sum += d
        if (sum.isNaN()) throw Exception("Can't normalise array. Sum is NaN")
        if (sum == 0.0) throw Exception("Can't normalise array. Sum is zero")
        for (i in doubles.indices) doubles[i] /= sum
    }

    companion object {
        init {
            NaiveBayesLearner::classLearnerType.override(Classifier)
        }
    }
}

class ConfigurableNaiveBayesLearnerFactory : ConfigurableLearnerFactory<NaiveBayesLearner, NaiveBayesConfiguration> {

    @Configurable.Register<ConfigurableNaiveBayesLearnerFactory, NaiveBayesConfiguration>(
        ConfigurableNaiveBayesLearnerFactory::class,
        NaiveBayesConfiguration::class
    )
    constructor(block : NaiveBayesConfiguration.() -> Unit = {}) : super(block)

    constructor(config : NaiveBayesConfiguration) : this(config.asReconfigureBlock())

    override fun create() : NaiveBayesLearner {
        return NaiveBayesLearner(
                configuration.targetIndex,
                configuration.useKernelEstimator,
                configuration.useDiscretization
        )
    }

}

class NaiveBayesConfiguration : Configuration() {

    @ConfigurationElement.WithMetadata("The index of the attribute to classify")
    var targetIndex by ConfigurationItem { -1 }

    @ConfigurationElement.WithMetadata("Whether to use a kernel estimator")
    var useKernelEstimator by ConfigurationItem { false }

    @ConfigurationElement.WithMetadata("Whether to use discretization")
    var useDiscretization by ConfigurationItem { false }

}

sealed class Estimator : Serializable {

    abstract fun addValue(value : Double)

    abstract fun getProbability(data : Double) : Double

    fun round(value : Double, precision: Double) : Double {
        return kotlin.math.round(value / precision) * precision
    }
}

class DiscreteEstimator(numSymbols : Int, laplace : Boolean) : Estimator() {

    val counts = Array(numSymbols) { 0.0 }

    var sumOfCounts = 0.0

    var fPrior = 0.0

    init {
        if (laplace) {
            fPrior = 1.0
            for (i in 0 until numSymbols) {
                counts[i] = 1.0
            }
            sumOfCounts = numSymbols.toDouble()
        }
    }

    override fun addValue(value: Double) {
        counts[value.toInt()] += 1.0
        sumOfCounts += 1.0
    }

    override fun getProbability(data: Double): Double {
        return if (sumOfCounts == 0.0)
            0.0
        else
            counts[data.toInt()] / sumOfCounts
    }

}

class NormalEstimator(val precision : Double) : Estimator() {

    var standardDev = precision / 6

    var sumOfWeights  = 0.0

    var sumOfValues = 0.0

    var sumOfValuesSq = 0.0

    var mean = 0.0

    override fun addValue(value: Double) {
        val rounded = round(value, precision)
        sumOfWeights += 1.0
        sumOfValues += rounded
        sumOfValuesSq += rounded * rounded

        computeParameters()
    }

    override fun getProbability(data: Double): Double {
        val rounded = round(data, precision)
        val zLower = (rounded - mean - precision / 2) / standardDev
        val zUpper = (rounded - mean + precision / 2) / standardDev
        val pLower = normalProbability(zLower)
        val pUpper = normalProbability(zUpper)
        return (pUpper - pLower) / precision
    }

    fun computeParameters() {
        if (sumOfWeights == 0.0) return

        mean = sumOfValues / sumOfWeights
        val stdDev = sqrt(abs(sumOfValuesSq - mean * sumOfValues) / sumOfWeights)
        if (stdDev > 1e-10) {
            standardDev = max(precision / 6, stdDev)
        }
    }
}

class KernelEstimator(precision : Double) : Estimator() {

    var values = Array(50) { 0.0 }

    var weights = Array(50) { 0.0 }

    var numValues = 0

    var sumOfWeights = 0.0

    var allWeightsOne = true

    val precision = max(precision, 1e-6)

    var standardDev = this.precision / 6

    override fun addValue(value: Double) {

        val rounded = round(value, precision)
        val insertIndex = findNearestValue(rounded)
        if (numValues <= insertIndex || values[insertIndex] != rounded) {
            if (numValues < values.size) {
                values.copyInto(values, insertIndex + 1, insertIndex, numValues)
                weights.copyInto(weights, insertIndex + 1, insertIndex, numValues)

                values[insertIndex] = rounded
                weights[insertIndex] = 1.0
                numValues++
            } else {
                val newValues = Array(values.size * 2) { 0.0 }
                val newWeights = Array(weights.size * 2) { 0.0 }
                values.copyInto(newValues, 0, 0, insertIndex)
                weights.copyInto(newWeights, 0, 0, insertIndex)
                newValues[insertIndex] = rounded
                newWeights[insertIndex] = 1.0
                values.copyInto(newValues, insertIndex + 1, insertIndex, numValues)
                weights.copyInto(newWeights, insertIndex + 1, insertIndex, numValues)
                numValues++
                values = newValues
                weights = newWeights
            }
        } else {
            weights[insertIndex] += 1.0
            allWeightsOne = false
        }
        sumOfWeights += 1.0
        val range = values[numValues - 1] - values[0]
        if (range > 0) {
            standardDev = max(range / sqrt(sumOfWeights), precision / 6)
        }
    }

    override fun getProbability(data: Double): Double {
        val rounded = round(data, precision)

        var delta : Double
        var sum = 0.0
        var currentProb : Double
        var zLower : Double
        var zUpper : Double

        if (numValues == 0) {
            zLower = (rounded - precision / 2) / standardDev
            zUpper = (rounded + precision / 2) / standardDev
            return normalProbability(zUpper) - normalProbability(zLower)
        }
        var weightSum = 0.0
        val start = findNearestValue(rounded)
        for (i in start until numValues) {
            delta = values[i] - rounded
            zLower = (delta - precision / 2) / standardDev
            zUpper = (delta + precision / 2) / standardDev
            currentProb = normalProbability(zUpper) - normalProbability(zLower)
            sum += currentProb * weights[i]
            /*
       * System.out.print("zL" + (i + 1) + ": " + zLower + " ");
       * System.out.print("zU" + (i + 1) + ": " + zUpper + " ");
       * System.out.print("P" + (i + 1) + ": " + currentProb + " ");
       * System.out.println("total: " + (currentProb * m_Weights[i]) + " ");
       */weightSum += weights[i]
            if (currentProb * (sumOfWeights - weightSum) < sum * MAX_ERROR) {
                break
            }
        }
        for (i in start - 1 downTo 0) {
            delta = values[i] - rounded
            zLower = (delta - precision / 2) / standardDev
            zUpper = (delta + precision / 2) / standardDev
            currentProb = normalProbability(zUpper) - normalProbability(zLower)
            sum += currentProb * weights[i]
            weightSum += weights[i]
            if (currentProb * (sumOfWeights - weightSum) < sum * MAX_ERROR) {
                break
            }
        }
        return sum / (sumOfWeights * precision)

    }

    fun findNearestValue(key : Double) : Int {
        var low = 0
        var high = numValues
        var middle : Int
        while (low < high) {
            middle = (low + high) / 2
            val current = values[middle]
            if (current == key) return middle
            if (current > key)
                high = middle
            else if (current < key)
                low = middle + 1
        }
        return low
    }

    companion object {

        const val MAX_ERROR = 0.01
    }

}
