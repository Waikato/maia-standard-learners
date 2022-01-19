package maia.ml.learner.standard.hoeffdingtree.node

import maia.ml.dataset.DataRow
import maia.ml.dataset.util.weight
import maia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import maia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class LearningNodeNBAdaptive(
    owner: HoeffdingTree,
    observations: ObservedClassDistribution,
    isActive: Boolean = true
): LearningNodeNB(owner, observations, isActive) {
    private var mcCorrectWeight = 0.0
    private var nbCorrectWeight = 0.0

    override fun learnFromRow(row : DataRow) {
        val trueClassIndex = row.getValue(owner.classType.indexRepresentation)
        if (observedClassDistribution.maxClassIndex == trueClassIndex)
            mcCorrectWeight += row.weight
        if (doNaiveBayesPrediction(row).maxClassIndex.coerceAtLeast(0) == trueClassIndex)
            nbCorrectWeight += row.weight
        super.learnFromRow(row)
    }

    override fun getClassVotes(row : DataRow) : ObservedClassDistribution {
        if (mcCorrectWeight > nbCorrectWeight)
            return observedClassDistribution

        return doNaiveBayesPrediction(row)
    }

}
