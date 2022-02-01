//package maia.ml.learner.standard.hoeffdingtree
//
//import moa.classifiers.trees.RandomHoeffdingTree
//import maia.ml.learner.Learner
//import maia.ml.dataset.DataRow
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//import maia.ml.learner.type.LearnerType
//
//open class AdaptiveRandomForest : Ozabag(
//    targetIndex = 0,
//    ensembleSize = 10,
//
//)
//
//{
//    public val FeaturesPerTree = 6
//    public val lambdaOption: Float = 6.0F
//    public  val numberOfJobs: Int = 1
//    protected val FEATURES_M = 0
//    protected val FEATURES_SQRT = 1
//    protected val FEATURES_SQRT_INV = 2
//    protected val FEATURES_PERCENT = 3
//
//
//    protected val SINGLE_THREAD = 0
//
//    lateinit var ensemble2: RandomHoeffdingTree
//    protected var instancesSeen: Long = 0
//    protected var subspaceSize = 0
//    private var executor: ExecutorService? = null
//
//
//    open fun resetLearningImpl() {
//        // Reset attributes
//        ensemble = ArrayList<RandomHoeffdingTree>(emptyList())//emptyArray()
//        subspaceSize = 0
//        instancesSeen = 0
//
//        // Multi-threading
//        val numberOfJobs: Int
//        numberOfJobs =
//            if (this.numberOfJobs === -1) Runtime.getRuntime()
//                .availableProcessors() else this.numberOfJobs
//        // SINGLE_THREAD and requesting for only 1 thread are equivalent.
//        // this.executor will be null and not used...
//        if (numberOfJobs != this.SINGLE_THREAD && numberOfJobs != 1) this.executor =
//            Executors.newFixedThreadPool(numberOfJobs)
//    }
//
//    protected override fun trainOnRow(
//        row : DataRow
//    ) {
//        ++this.instancesSeen
//        if(this.ensemble == null)
//            initEnsemble(row);
//
//    }
//
//    protected open fun initEnsemble(row : DataRow) {
//        // Init the ensemble.
//        val ensembleSize: Int = this.ensembleSize
//        ensemble2 = RandomHoeffdingTree()
//
//
//        subspaceSize = this.FeaturesPerTree
//
//        // The size of m depends on:
//        // 1) mFeaturesPerTreeSizeOption
//        // 2) mFeaturesModeOption
//        val n: Int = row.numColumns - 1
//        //when (this.mFeaturesModeOption.getChosenIndex()) {
//        //    this.FEATURES_SQRT -> subspaceSize = Math.round(Math.sqrt(n.toDouble()))
//                .toInt() + 1
//        //    this.FEATURES_SQRT_INV -> subspaceSize =
//        //        n - Math.round(Math.sqrt(n.toDouble()) + 1)
//                    .toInt()
//        //    this.FEATURES_PERCENT -> {
//                // If subspaceSize is negative, then first find out the actual percent, i.e., 100% - m.
//        //        val percent =
//        //            if (subspaceSize < 0) (100 + subspaceSize) / 100.0 else subspaceSize / 100.0
//        //        subspaceSize = Math.round(n * percent).toInt()
//        //    }
//        //}
//        // Notice that if the selected mFeaturesModeOption was
//        //  AdaptiveRandomForest.FEATURES_M then nothing is performed in the
//        //  previous switch-case, still it is necessary to check (and adjusted)
//        //  for when a negative value was used.
//
////        // m is negative, use size(features) + -m
////        if (subspaceSize < 0) subspaceSize = n + subspaceSize
////        // Other sanity checks to avoid runtime errors.
////        //  m <= 0 (m can be negative if this.subspace was negative and
////        //  abs(m) > n), then use m = 1
////        if (subspaceSize <= 0) subspaceSize = 1
////        // m > n, then it should use n
////        if (subspaceSize > n) subspaceSize = n
////        val treeLearner: RandomHoeffdingTree =
////            getPreparedClassOption(this.treeLearnerOption) as ARFHoeffdingTree
////        treeLearner.resetLearning()
////        for (i in 0 until ensembleSize) {
////            treeLearner.subspaceSizeOption.setValue(subspaceSize)
////            ensemble[i] = ARFBaseLearner(
////                i,
////                treeLearner.copy() as ARFHoeffdingTree,
////                classificationEvaluator.copy() as BasicClassificationPerformanceEvaluator,
////                instancesSeen,
////                !this.disableBackgroundLearnerOption.isSet(),
////                !this.disableDriftDetectionOption.isSet(),
////                driftDetectionMethodOption,
////                warningDetectionMethodOption,
////                false
////            )
////        }
//    }
//}
