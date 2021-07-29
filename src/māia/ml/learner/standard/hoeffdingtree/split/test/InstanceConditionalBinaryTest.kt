package māia.ml.learner.standard.hoeffdingtree.split.test

import māia.ml.learner.standard.hoeffdingtree.split.test.SplitTest
import māia.util.Present
import māia.util.asOptional

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
abstract class InstanceConditionalBinaryTest : SplitTest() {

    final override val maxBranches : Present<Int>
        get() = 2.asOptional

}
