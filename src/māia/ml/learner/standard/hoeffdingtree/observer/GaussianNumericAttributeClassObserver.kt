package māia.ml.learner.standard.hoeffdingtree.observer

import māia.ml.dataset.type.DataTypeWithMissingValues
import māia.ml.dataset.type.Nominal
import māia.ml.dataset.type.Numeric
import māia.ml.dataset.type.WithFiniteMissingValues
import māia.ml.dataset.util.convertToExternalUnchecked
import māia.ml.dataset.util.isMissingInternalUnchecked
import māia.ml.learner.standard.hoeffdingtree.split.criterion.SplitCriterion
import māia.ml.learner.standard.hoeffdingtree.split.test.NumericAttributeBinaryTest
import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
import māia.util.GaussianEstimator
import māia.util.size
import java.util.*
import kotlin.collections.HashMap

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class GaussianNumericAttributeClassObserver(
    dataType: DataTypeWithMissingValues<*, Double, Numeric<*>, *, *>,
    classDataType: Nominal<*>,
    val numBins: Int = 10
): NumericAttributeClassObserver(
    dataType,
    classDataType
) {
    constructor(
        dataType: WithFiniteMissingValues<*, Double, Numeric<*>, *, *>,
        classDataType: Nominal<*>
    ): this(dataType, classDataType, 10)

    private val classEstimators: MutableMap<Int, GaussianEstimator> = HashMap()

    override fun observeAttributeForClass(
        value : Any?,
        classIndex : Int,
        weight : Double
    ) {
        if (attributeDataType.isMissingInternalUnchecked(value)) return

        val estimator = classEstimators[classIndex]
            ?: GaussianEstimator().also { classEstimators[classIndex] = it }

        val numericValue = attributeDataType.base.convertToExternalUnchecked(value)

        estimator.observe(numericValue, weight)
    }

    override fun probabilityOfAttributeValueGivenClass(
        value : Any?,
        classIndex : Int
    ) : Double {
        val estimator = classEstimators[classIndex] ?: return 0.0
        val numericValue = attributeDataType.base.convertToExternalUnchecked(value)
        return estimator.probabilityDensity(numericValue)
    }

    override fun getBestEvaluatedSplitSuggestion(
        criterion : SplitCriterion,
        preSplitDistribution : ObservedClassDistribution,
        attributeIndex : Int,
        binaryOnly : Boolean
    ) : SplitSuggestion? {
        var suggestion: SplitSuggestion? = null

        val suggestedSplitValues = getSplitPointSuggestions()

        for (splitValue in suggestedSplitValues) {
            val postSplitDistributions = getClassDistsResultingFromBinarySplit(splitValue)

            val merit = criterion.getMeritOfSplit(
                preSplitDistribution,
                postSplitDistributions
            )

            if (suggestion === null || merit > suggestion.merit) {
                suggestion = SplitSuggestion(
                    NumericAttributeBinaryTest(attributeIndex, splitValue, true),
                    postSplitDistributions,
                    merit
                )
            }
        }

        return suggestion
    }

    override fun observeAttributeTarget(attVal : Double, target : Double) {
        TODO("Not yet implemented")
    }

    fun getSplitPointSuggestions(): Set<Double> {
        val suggestedSplitValues = TreeSet<Double>()

        val minValue = classEstimators.values.minOf { it.minObserved }
        val maxValue = classEstimators.values.maxOf { it.maxObserved }

        val range = minValue..maxValue

        for (bin in 0 until numBins) {
            val splitValue = range.size() / (numBins + 1) * (bin + 1) + minValue
            if (splitValue in range) suggestedSplitValues.add(splitValue)
        }

        return suggestedSplitValues
    }

    fun getClassDistsResultingFromBinarySplit(
        splitValue: Double
    ): Array<ObservedClassDistribution> {
        val lhsDist = ObservedClassDistribution(numTargetClasses)
        val rhsDist = ObservedClassDistribution(numTargetClasses)

        classEstimators.forEach { i, estimator ->
            if (splitValue < estimator.minObserved)
                rhsDist[i] = estimator.weightSum
            else if (splitValue >= estimator.maxObserved)
                lhsDist[i] = estimator.weightSum
            else {
                val weightDist = estimator.estimatedWeightsComparedToValue(splitValue)
                lhsDist[i] = weightDist.lessThanValue + weightDist.equalToValue
                rhsDist[i] = weightDist.greaterThenValue
            }
        }

        return arrayOf(lhsDist, rhsDist)
    }
}
