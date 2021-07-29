package māia.ml.learner.standard.hoeffdingtree.split.test

import māia.ml.dataset.DataRow
import māia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import māia.ml.learner.standard.hoeffdingtree.split.test.InstanceConditionalBinaryTest
import māia.util.Optional

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class NominalAttributeBinaryTest(
    attIndex: Int,
    attValue: Int
): InstanceConditionalBinaryTest() {
    override fun branchForRow(row : DataRow) : Optional<Int> {
        TODO("Not yet implemented")
    }

    override val attributesTestDependsOn : IntArray
        get() = TODO("Not yet implemented")

    override fun describeConditionForBranch(
        branch : Int,
        tree : HoeffdingTree
    ) : String {
        TODO("Not yet implemented")
    }
}
