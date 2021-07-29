package māia.ml.learner.standard.hoeffdingtree.split.test

import māia.ml.dataset.DataRow
import māia.ml.dataset.util.ifNotMissing
import māia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import māia.util.Absent
import māia.util.Optional
import māia.util.asOptional

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
        row.ifNotMissing(attIndex) { _, value ->
            return if ((value as Double) < attValue || (value == attValue && equalsPassesTest))
                0.asOptional
            else
                1.asOptional
        }

        return Absent
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
            append(tree.trainHeaders.getColumnHeader(attIndex).name)
            append(":att ${attIndex + 1}] ")
            append(compareChar)
            append(if (branch == equalsBranch) "= " else " ")
            append(attValue)
        }
    }
}
