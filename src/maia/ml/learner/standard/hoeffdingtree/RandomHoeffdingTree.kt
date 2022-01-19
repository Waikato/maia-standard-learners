///*
// *    RandomHoeffdingTree.java
// *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
// *    @author Albert Bifet (abifet@cs.waikato.ac.nz)
// *
// *    This program is free software; you can redistribute it and/or modify
// *    it under the terms of the GNU General Public License as published by
// *    the Free Software Foundation; either version 3 of the License, or
// *    (at your option) any later version.
// *
// *    This program is distributed in the hope that it will be useful,
// *    but WITHOUT ANY WARRANTY; without even the implied warranty of
// *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *    GNU General Public License for more details.
// *
// *    You should have received a copy of the GNU General Public License
// *    along with this program. If not, see <http://www.gnu.org/licenses/>.
// *
// */
//package moa.classifiers.trees
//
//import maia.ml.learner.standard.hoeffdingtree.HoeffdingTree
//import maia.ml.learner.standard.hoeffdingtree.LeafPredictor
//import maia.ml.learner.standard.hoeffdingtree.node.LearningNode
//import maia.ml.learner.standard.hoeffdingtree.node.LearningNodeNB
//import maia.ml.learner.standard.hoeffdingtree.node.LearningNodeNBAdaptive
//import maia.ml.learner.standard.hoeffdingtree.node.RandomLearningNode
//import maia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution
//
//
///**
// * Random decision trees for data streams.
// *
// * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
// * @version $Revision: 7 $
// */
//open class RandomHoeffdingTree : HoeffdingTree() {
//
//
//    fun RandomHoeffdingTree() {
//        this.removePoorAttributes = false
//
//    }
//
//    fun isRandomizable(): Boolean {
//        return true
//    }
//
//    override fun newLearningNode(
//        initialClassObservations: ObservedClassDistribution?
//    ): LearningNode {
//        val observations = initialClassObservations ?: ObservedClassDistribution(classType.numCategories)
//
//        return when (leafPredictor) {
//            LeafPredictor.MAJORITY_CLASS -> RandomLearningNode(this, observations, true)
//            LeafPredictor.NAIVE_BAYES -> LearningNodeNB(this, observations, true)
//            LeafPredictor.NAIVE_BAYES_ADAPTIVE -> LearningNodeNBAdaptive(this, observations, true)
//        }
//    }
//
//}