package māia.ml.learner.standard.hoeffdingtree.split.criterion

import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution


/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
interface SplitCriterion {

    fun getMeritOfSplit(
        preSplitDistribution: ObservedClassDistribution,
        postSplitDistributions: Array<ObservedClassDistribution>
    ): Double

    fun getRangeOfMerit(
        preSplitDistribution: ObservedClassDistribution
    ): Double

}
