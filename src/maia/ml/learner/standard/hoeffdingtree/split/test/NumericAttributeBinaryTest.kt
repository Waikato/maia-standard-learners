package maia.ml.learner.standard.hoeffdingtree.split.test

import maia.ml.dataset.DataRow
import maia.ml.dataset.error.MissingValue
import maia.ml.dataset.type.standard.Numeric
import maia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import maia.util.Absent
import maia.util.Optional
import maia.util.asOptional
import maia.util.assertType

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class NumericAttributeBinaryTest(
    protected val attIndex: Int,
    protected val attValue: Double,
    protected val equalsPassesTest: Boolean
): InstanceConditionalBinaryTest() {

    override fun branchForRow(
        row : DataRow
    ) : Optional<Int> {
        return try {
            val value = row.getValue(assertType<Numeric<*, *>>(row.headers[attIndex].type).canonicalRepresentation)
            if (value < attValue || (value == attValue && equalsPassesTest))
                0.asOptional
            else
                1.asOptional
        } catch (e: MissingValue) {
            Absent
        }
    }

    override val attributesTestDependsOn : IntArray
        get() = intArrayOf(attIndex)

    override fun describeConditionForBranch(
        branch : Int,
        tree : HoeffdingTree
    ) : String {
        val compareChar = if (branch == 0) "<" else ">"
        val equalsBranch = if (equalsPassesTest) 0 else 1
        return buildString {
            append("[")
            append(tree.trainHeaders[attIndex].name)
            append(":att ${attIndex + 1}] ")
            append(compareChar)
            append(if (branch == equalsBranch) "= " else " ")
            append(attValue)
        }
    }
}
