package maia.ml.learner.standard.hoeffdingtree.split.test

import maia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import maia.ml.dataset.DataRow
import maia.util.Optional
import maia.util.Present


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
