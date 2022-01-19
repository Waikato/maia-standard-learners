package maia.ml.learner.standard.hoeffdingtree.split.test

import maia.util.Present
import maia.util.asOptional

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
abstract class InstanceConditionalBinaryTest : SplitTest() {

    final override val maxBranches : Present<Int>
        get() = 2.asOptional

}
