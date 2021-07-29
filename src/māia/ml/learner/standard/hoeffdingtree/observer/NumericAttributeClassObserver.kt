package māia.ml.learner.standard.hoeffdingtree.observer

import māia.ml.dataset.type.DataTypeWithMissingValues
import māia.ml.dataset.type.Nominal
import māia.ml.dataset.type.Numeric

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
abstract class NumericAttributeClassObserver(
    dataType: DataTypeWithMissingValues<*, Double, Numeric<*>, *, *>,
    classDataType: Nominal<*>
): AttributeClassObserver<DataTypeWithMissingValues<*, Double, Numeric<*>, *, *>>(
    dataType,
    classDataType
) {

}
