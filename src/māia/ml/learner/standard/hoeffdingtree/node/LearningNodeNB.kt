package māia.ml.learner.standard.hoeffdingtree.node

import māia.ml.dataset.DataRow
import māia.ml.dataset.util.ifNotMissing
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
                        row.ifNotMissing(attributeIndex) { _, value ->
                            result *= observer.probabilityOfAttributeValueGivenClass(value, i)
                        }
                }
                result
            }
        }

        return result
    }

    override fun disableAttribute(index : Int) {}
}
