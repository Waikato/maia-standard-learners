package maia.ml.learner.standard.hoeffdingtree.node

import maia.ml.dataset.DataRow
//import maia.ml.dataset.type.DataTypeWithMissingValues
import maia.ml.dataset.util.*
import maia.ml.learner.standard.hoeffdingtree.HoeffdingTree
import maia.ml.learner.standard.hoeffdingtree.observer.AttributeClassObserver
import maia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
import kotlin.math.floor
import kotlin.math.sqrt
import maia.ml.dataset.type.standard.Nominal
import maia.ml.dataset.type.standard.Numeric


open class RandomLearningNode(
    owner: HoeffdingTree,
    observations: ObservedClassDistribution,
    isActive: Boolean
): LearningNode(owner, observations, isActive) {

    protected lateinit var listAttributes: IntArray
    protected var numAttributes = 0

    override fun learnFromRow(row : DataRow) {
        //val trueClassIndex =
        //    owner.classType.indexOfInternalUnchecked(row.getColumn(owner.classColumnIndex))

        //observedClassDistribution[trueClassIndex] += row.weight
        observedClassDistribution[owner.classColumnIndex] += row.weight

        if (this.listAttributes == null) {
            this.numAttributes = floor(sqrt(row.numColumns.toDouble())).toInt()
            this.listAttributes = IntArray(this.numAttributes)

            for (j in 0 until this.numAttributes) {
                var isUnique = false
                while (!isUnique) {
                    this.listAttributes[j] =
                        owner.classifierRandom?.nextInt(this.numAttributes - 1)!!
                    isUnique = true;

                    for (i in 0..j) {
                        if (this.listAttributes[j] == this.listAttributes[i]) {
                            isUnique = false
                            break
                        }
                    }

                }

            }

        }

        for(j in 0 until this.numAttributes)
        {
            val i = listAttributes[j]
            val instAttIndex: Int = observerIndex(i)
            var obs: AttributeClassObserver<*> = attributeObservers[i]
            if (obs == null) {
                val type = owner.predictInputHeaders[instAttIndex].type

                /*obs = if (isPossiblyMissing<Nominal<*, *, *, *>>(type)) {
                    owner.nominalEstimatorFactory(
                        type.ensureMissingValues() as DataTypeWithMissingValues<*, String, Nominal<*, *, *, *>, *, *>,
                        owner.classType
                    )
                } else if (isPossiblyMissing<Numeric<*, *>>(type)) {
                    owner.numericEstimatorFactory(
                        type.ensureMissingValues() as DataTypeWithMissingValues<*, Double, Numeric<*, *>, *, *>,
                        owner.classType
                    )
                } else {
                    throw Exception("Attribute type must be numeric or nominal")
                }*/
            }
            obs.observe(
                //row.getValueExternal(instAttIndex),
                //row.javaClass as Int,       //Potentially wrong?
                //row.weight
                row, row.getValue(owner.classType.indexRepresentation), row.weight
            )
        }
    }
}