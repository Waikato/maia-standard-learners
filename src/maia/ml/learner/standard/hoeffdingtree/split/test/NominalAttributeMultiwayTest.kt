package maia.ml.learner.standard.hoeffdingtree.split.test

import maia.ml.dataset.DataRow
import maia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import maia.util.Optional

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class NominalAttributeMultiwayTest(
    protected val attributeIndex: Int
) : SplitTest() {

    override fun branchForRow(row : DataRow) : Optional<Int> {
        TODO("Not implemented yet.")
    }

    override val maxBranches : Optional<Int>
        get() = TODO("Not yet implemented")

    override val attributesTestDependsOn : IntArray
        get() = TODO("Not yet implemented")

    override fun describeConditionForBranch(
        branch : Int,
        tree : HoeffdingTree
    ) : String {
        TODO("Not yet implemented")
    }

}
