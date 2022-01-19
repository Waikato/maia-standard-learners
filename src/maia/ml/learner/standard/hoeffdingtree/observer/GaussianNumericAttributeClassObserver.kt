package maia.ml.learner.standard.hoeffdingtree.observer

import maia.ml.dataset.DataRow
import maia.ml.dataset.error.MissingValue
import maia.ml.dataset.type.standard.Nominal
import maia.ml.dataset.type.standard.Numeric
import maia.ml.learner.standard.hoeffdingtree.split.criterion.SplitCriterion
import maia.ml.learner.standard.hoeffdingtree.split.test.NumericAttributeBinaryTest
import maia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
import maia.util.GaussianEstimator
import maia.util.size
import java.util.*
import kotlin.collections.HashMap

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class GaussianNumericAttributeClassObserver(
    dataType: Numeric<*, *>,
    classDataType: Nominal<*, *, *, *>,
    val numBins: Int
): NumericAttributeClassObserver(
    dataType,
    classDataType
) {
    constructor(
        dataType: Numeric<*, *>,
        classDataType: Nominal<*, *, *, *>,
    ): this(dataType, classDataType, 10)

    private val classEstimators: MutableMap<Int, GaussianEstimator> = HashMap()

    override fun observeAttributeForClass(
        row : DataRow,
        classIndex : Int,
        weight : Double
    ) {
        try {
            val estimator = classEstimators[classIndex]
                ?: GaussianEstimator().also { classEstimators[classIndex] = it }
            val numericValue = row.getValue(attributeDataType.canonicalRepresentation)
            estimator.observe(numericValue, weight)
        } catch (e: MissingValue) {
            return
        }
    }

    override fun probabilityOfAttributeValueGivenClass(
        row : DataRow,
        classIndex : Int
    ) : Double {
        val estimator = classEstimators[classIndex] ?: return 0.0
        val numericValue = row.getValue(attributeDataType.canonicalRepresentation)
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
