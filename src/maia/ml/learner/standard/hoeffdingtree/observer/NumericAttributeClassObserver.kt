package maia.ml.learner.standard.hoeffdingtree.observer

import maia.ml.dataset.type.standard.Nominal
import maia.ml.dataset.type.standard.Numeric

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
abstract class NumericAttributeClassObserver(
    dataType: Numeric<*, *>,
    classDataType: Nominal<*, *, *, *, *>
): AttributeClassObserver<Numeric<*, *>>(
    dataType,
    classDataType
) {

}
