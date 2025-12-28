package war.metaphor.util;


import java.util.HashMap;
import java.util.Map;

public class UnionFind<T> {

    private final Map<T, T> parent = new HashMap<>();
    private final Map<T, Integer> rank = new HashMap<>();

    public T find(T node) {
        if (parent.get(node) != node) {
            parent.put(node, find(parent.get(node)));
        }
        return parent.get(node);
    }

    public void union(T node1, T node2) {
        T root1 = find(node1);
        T root2 = find(node2);

        if (root1 != root2) {
            int rank1 = rank.get(root1);
            int rank2 = rank.get(root2);

            if (rank1 > rank2) {
                parent.put(root2, root1);
            } else if (rank1 < rank2) {
                parent.put(root1, root2);
            } else {
                parent.put(root2, root1);
                rank.put(root1, rank1 + 1);
            }
        }
    }

    public void add(T node) {
        if (!parent.containsKey(node)) {
            parent.put(node, node);
            rank.put(node, 0);
        }
    }
}