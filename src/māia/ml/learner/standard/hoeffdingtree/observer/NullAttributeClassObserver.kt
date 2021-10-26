package māia.ml.learner.standard.hoeffdingtree.observer

import māia.ml.dataset.DataRow
import māia.ml.dataset.type.standard.Nominal
import māia.ml.dataset.type.standard.UntypedData
import māia.ml.learner.standard.hoeffdingtree.split.criterion.SplitCriterion
import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class NullAttributeClassObserver(
    classDataType: Nominal<*, *, *, *>
) : AttributeClassObserver<UntypedData<*, *>>(UntypedData.PlaceHolder(true), classDataType) {
    override fun observeAttributeForClass(
        row : DataRow,
        classIndex : Int,
        weight : Double
    ) {}

    override fun probabilityOfAttributeValueGivenClass(
        row : DataRow,
        classIndex : Int
    ) : Double {
        return 0.0
    }

    override fun getBestEvaluatedSplitSuggestion(
        criterion : SplitCriterion,
        preSplitDistribution : ObservedClassDistribution,
        attributeIndex : Int,
        binaryOnly : Boolean
    ) : SplitSuggestion? {
        return null
    }

    override fun observeAttributeTarget(
        attVal : Double,
        target : Double
    ) {
        TODO("Not yet implemented")
    }

}
