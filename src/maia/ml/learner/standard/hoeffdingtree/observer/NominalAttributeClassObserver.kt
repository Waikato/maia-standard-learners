package maia.ml.learner.standard.hoeffdingtree.observer

import maia.ml.dataset.DataRow
import maia.ml.dataset.error.MissingValue
import maia.ml.dataset.type.standard.Nominal
import maia.ml.learner.standard.hoeffdingtree.split.criterion.SplitCriterion
import maia.ml.learner.standard.hoeffdingtree.split.test.NominalAttributeBinaryTest
import maia.ml.learner.standard.hoeffdingtree.split.test.NominalAttributeMultiwayTest
import maia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
import maia.util.mapInPlaceIndexed

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
open class NominalAttributeClassObserver(
    dataType: Nominal<*, *, *, *, *>,
    classDataType: Nominal<*, *, *, *, *>
): AttributeClassObserver<Nominal<*, *, *, *, *>>(
    dataType,
    classDataType
) {

    private var totalWeightObserved = 0.0
    private var missingWeightObserved = 0.0

    private val attributeDistributionPerClass = Array(classDataType.numCategories) {
        ObservedClassDistribution(dataType.numCategories)
    }

    val numAttributeClasses: Int
        get() = attributeDataType.numCategories

    override fun observeAttributeForClass(
        row : DataRow,
        classIndex : Int,
        weight : Double
    ) {
        totalWeightObserved += weight

        try {
            val attrClassIndex = row.getValue(attributeDataType.indexRepresentation)
            attributeDistributionPerClass[classIndex].array[attrClassIndex] += weight
        } catch (e: MissingValue) {
            missingWeightObserved += weight
        }
    }

    override fun probabilityOfAttributeValueGivenClass(
        row : DataRow,
        classIndex : Int
    ) : Double {
        val obs = attributeDistributionPerClass[classIndex].array
        val attrClassIndex = row.getValue(attributeDataType.indexRepresentation)
        val prob = (obs[attrClassIndex] + 1) / (obs.sum() + obs.size)
        return prob
    }

    override fun getBestEvaluatedSplitSuggestion(
        criterion : SplitCriterion,
        preSplitDistribution : ObservedClassDistribution,
        attributeIndex : Int,
        binaryOnly : Boolean
    ) : SplitSuggestion? {
        var suggestion: SplitSuggestion? = null

        if (!binaryOnly) {
            val postSplitDistributions = getClassDistributionsResultingFromMultiwaySplit()

            val merit = criterion.getMeritOfSplit(
                preSplitDistribution,
                postSplitDistributions
            )

            suggestion = SplitSuggestion(
                NominalAttributeMultiwayTest(attributeIndex),
                postSplitDistributions,
                merit
            )
        }

        for (index in 0 until numAttributeClasses) {
            val postSplitDists = getClassDistsResultingFromBinarySplit(index)
            val merit = criterion.getMeritOfSplit(preSplitDistribution, postSplitDists)
            if (suggestion === null || merit > suggestion.merit)
                suggestion = SplitSuggestion(
                    NominalAttributeBinaryTest(attributeIndex, index),
                    postSplitDists,
                    merit
                )
        }

        return suggestion
    }

    override fun observeAttributeTarget(attVal : Double, target : Double) {
        TODO("Not yet implemented")
    }

    fun getClassDistributionsResultingFromMultiwaySplit(): Array<ObservedClassDistribution> {
        return Array(numAttributeClasses) { attrClassIndex ->
            ObservedClassDistribution(numTargetClasses).also {
                it.array.mapInPlaceIndexed { classIndex ->
                    attributeDistributionPerClass[classIndex].array[attrClassIndex]
                }
            }
        }
    }

    fun getClassDistsResultingFromBinarySplit(
        valIndex: Int
    ): Array<ObservedClassDistribution> {
        return arrayOf(
            ObservedClassDistribution(numTargetClasses).apply {
                array.mapInPlaceIndexed { i, _ ->
                    attributeDistributionPerClass[i].array[valIndex]
                }
            },
            ObservedClassDistribution(numTargetClasses).apply {
                array.mapInPlaceIndexed { i, _ ->
                    val array = attributeDistributionPerClass[i].array
                    array.sum() - array[valIndex]
                }
            },
        )
    }
}
