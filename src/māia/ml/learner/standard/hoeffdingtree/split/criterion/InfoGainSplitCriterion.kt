package māia.ml.learner.standard.hoeffdingtree.split.criterion

import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
import māia.ml.learner.standard.hoeffdingtree.util.entropy
import māia.ml.learner.standard.hoeffdingtree.util.numSubsetsGreaterThanFrac
import kotlin.math.log2

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class InfoGainSplitCriterion(
    val minBranchFrac: Double = 0.01
): SplitCriterion {

    override fun getMeritOfSplit(
        preSplitDistribution : ObservedClassDistribution,
        postSplitDistributions : Array<ObservedClassDistribution>
    ) : Double {
        return if (postSplitDistributions.numSubsetsGreaterThanFrac(minBranchFrac) < 2)
            Double.NEGATIVE_INFINITY
        else
            preSplitDistribution.entropy - postSplitDistributions.entropy
    }

    override fun getRangeOfMerit(
        preSplitDistribution : ObservedClassDistribution
    ) : Double {
        return log2(preSplitDistribution.nonZeroLength.coerceAtLeast(2).toDouble())
    }
}
