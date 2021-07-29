package māia.ml.learner.standard.hoeffdingtree.split.test

import māia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import māia.ml.dataset.DataRow
import māia.util.Optional
import māia.util.Present


/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
abstract class SplitTest {

    abstract fun branchForRow(row: DataRow): Optional<Int>

    fun resultKnownForRow(row: DataRow): Boolean = branchForRow(row) is Present

    abstract val maxBranches: Optional<Int>

    abstract val attributesTestDependsOn: IntArray

    abstract fun describeConditionForBranch(branch: Int, tree: HoeffdingTree): String

}
