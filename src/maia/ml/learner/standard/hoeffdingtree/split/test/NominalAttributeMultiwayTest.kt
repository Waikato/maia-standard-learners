package maia.ml.learner.standard.hoeffdingtree.split.test

import maia.ml.dataset.DataRow
import maia.ml.dataset.type.standard.Nominal
import maia.ml.dataset.util.isMissing
import maia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import maia.util.Absent
import maia.util.Optional
import maia.util.Present
import maia.util.assertType

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class NominalAttributeMultiwayTest(
    protected val attributeIndex: Int
) : SplitTest() {

    override fun branchForRow(row : DataRow) : Optional<Int> {
        val type = assertType<Nominal<*, *, *, *>>(row.headers[attributeIndex].type)
        return if (row.isMissing(type))
            Absent
        else
            Present(row.getValue(type.indexRepresentation))
    }

    override val maxBranches : Optional<Int> = Absent

    override val attributesTestDependsOn : IntArray = IntArray(1) { attributeIndex }

    override fun describeConditionForBranch(
        branch : Int,
        tree : HoeffdingTree
    ) : String {
        val header = tree.trainHeaders[attributeIndex]
        val type = assertType<Nominal<*, *, *, *>>(header.type)
        return "${header.name} = ${type[branch]}"
    }

}
