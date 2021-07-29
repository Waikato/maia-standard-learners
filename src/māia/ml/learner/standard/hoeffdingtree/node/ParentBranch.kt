package māia.ml.learner.standard.hoeffdingtree.node

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
data class ParentBranch(
    val parent: SplitNode,
    val branch: Int
) {
    val child: Node?
        get() = parent[branch]

    fun setChild(child: Node) {
        parent[branch] = child
    }
}
