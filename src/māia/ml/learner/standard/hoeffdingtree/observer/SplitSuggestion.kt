package māia.ml.learner.standard.hoeffdingtree.observer

import māia.ml.learner.standard.hoeffdingtree.split.test.SplitTest
import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class SplitSuggestion(
    val splitTest : SplitTest?,
    resultingClassDistributions : Array<ObservedClassDistribution>,
    val merit : Double
) : Comparable<SplitSuggestion> {

    val resultingClassDistributions: Array<ObservedClassDistribution>

    init {
        this.resultingClassDistributions = resultingClassDistributions.clone()
    }

    override fun compareTo(other : SplitSuggestion) : Int {
        return merit.compareTo(other.merit)
    }

    val numSplits: Int
        get() = resultingClassDistributions.size

    fun resultingClassDistributionForSplit(
        splitIndex: Int
    ): ObservedClassDistribution {
        return resultingClassDistributions[splitIndex]
    }
}
