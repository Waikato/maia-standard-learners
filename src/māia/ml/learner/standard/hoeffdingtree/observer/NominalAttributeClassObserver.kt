package māia.ml.learner.standard.hoeffdingtree.observer

import māia.ml.dataset.type.DataTypeWithMissingValues
import māia.ml.dataset.type.Nominal
import māia.ml.dataset.util.convertNotMissingToBaseUnchecked
import māia.ml.dataset.util.indexOfInternalUnchecked
import māia.ml.dataset.util.isMissingInternalUnchecked
import māia.ml.learner.standard.hoeffdingtree.split.criterion.SplitCriterion
import māia.ml.learner.standard.hoeffdingtree.split.test.NominalAttributeBinaryTest
import māia.ml.learner.standard.hoeffdingtree.split.test.NominalAttributeMultiwayTest
import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
import māia.util.mapInPlaceIndexed

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
open class NominalAttributeClassObserver(
    dataType: DataTypeWithMissingValues<*, String, Nominal<*>, *, *>,
    classDataType: Nominal<*>
): AttributeClassObserver<DataTypeWithMissingValues<*, String, Nominal<*>, *, *>>(
    dataType,
    classDataType
) {

    private var totalWeightObserved = 0.0
    private var missingWeightObserved = 0.0

    private val attributeDistributionPerClass = Array(classDataType.numCategories) {
        ObservedClassDistribution(dataType.base.numCategories)
    }

    val numAttributeClasses: Int
        get() = attributeDataType.base.numCategories

    override fun observeAttributeForClass(
        value : Any?,
        classIndex : Int,
        weight : Double
    ) {
        totalWeightObserved += weight

        if (attributeDataType.isMissingInternalUnchecked(value))
            missingWeightObserved += weight
        else {
            val attrClass = attributeDataType.internalConverter.convertNotMissingToBaseUnchecked(value)
            val attrClassIndex = attributeDataType.base.indexOfInternalUnchecked(attrClass)
            attributeDistributionPerClass[classIndex].array[attrClassIndex] += weight
        }
    }

    override fun probabilityOfAttributeValueGivenClass(
        value : Any?,
        classIndex : Int
    ) : Double {
        val obs = attributeDistributionPerClass[classIndex].array
        val attrClass = attributeDataType.internalConverter.convertNotMissingToBaseUnchecked(value)
        val attrClassIndex = attributeDataType.base.indexOfInternalUnchecked(attrClass)
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
