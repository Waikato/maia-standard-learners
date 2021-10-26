package māia.ml.learner.standard.hoeffdingtree.node

import māia.ml.dataset.DataRow
import māia.ml.dataset.error.MissingValue
import māia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
import māia.util.inlineRangeForLoop
import māia.util.mapInPlaceIndexed

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
open class LearningNodeNB(
    owner: HoeffdingTree,
    observations: ObservedClassDistribution,
    isActive: Boolean
): LearningNode(owner, observations, isActive) {

    override fun getClassVotes(row : DataRow) : ObservedClassDistribution {
        if (observedClassDistribution.totalWeightSeen < owner.nbThreshold)
            return super.getClassVotes(row)

        return doNaiveBayesPrediction(row)
    }

    fun doNaiveBayesPrediction(row : DataRow): ObservedClassDistribution {
        initAttributeObservers()
        val totalWeight = observedClassDistribution.totalWeightSeen
        val result = ObservedClassDistribution(owner.classType.numCategories)

        if (totalWeight != 0.0) {
            result.array.mapInPlaceIndexed { i, _ ->
                var result = observedClassDistribution[i] / totalWeight
                inlineRangeForLoop(attributeObservers.size) {
                    val attributeIndex = attributeIndices[it]
                    val observer = attributeObservers[it]
                    if (observer.observedWeight != 0.0)
                        try {
                            result *= observer.probabilityOfAttributeValueGivenClass(row, i)
                        } catch (e: MissingValue) {
                            // Do nothing for missing values
                        }
                }
                result
            }
        }

        return result
    }

    override fun disableAttribute(index : Int) {}
}
