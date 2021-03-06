import java.util.*;
import java.io.*;

public class Graph {
    public Set<Node> nodes = new HashSet<>();
    public Set<Arc> arcs = new HashSet<>();
    public int[] shortestPath;

    // CHANGE "filename" TO SELECT THE TEST GRAPH
    public static String filename = "testGraph1"; // "testGraph2";
    public static String SRC = "src/" + filename + ".txt";

    /******** INITIALIZING ORIGINAL GRAPH ********/
    /*
     * Returns: void
     */
    public void initGraph() throws FileNotFoundException {
        File file = new File(SRC);
        Scanner sc = new Scanner(file);
        int iter = 0;
        Set<Integer> nodesNames = new HashSet<Integer>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] split = line.split(" ");

            if (iter == 0) { // node, supply, node, supply,...
                for (int i = 0; i < split.length; i += 2) {
                    int name = Integer.parseInt(split[i]);
                    int supply = Integer.parseInt(split[i + 1]);
                    nodesNames.add(name);
                    Node supplyNode = new Node(name, supply);
                    nodes.add(supplyNode);
                }
                iter++;
            } else if (iter == 1) { // node, supply, node, supply,...
                for (int i = 0; i < split.length; i += 2) {
                    int name = Integer.parseInt(split[i]);
                    int supply = -1 * Integer.parseInt(split[i + 1]);
                    nodesNames.add(name);
                    Node sourceNode = new Node(name, supply);
                    nodes.add(sourceNode);
                }
                iter++;
            } else { // arcname n1 n2 capacity cost
                String arcName = split[0];
                int first = Integer.parseInt(split[1]);
                int second = Integer.parseInt(split[2]);
                int u = Integer.parseInt(split[3]);
                int c = Integer.parseInt(split[4]);
                Node n1 = null;
                Node n2 = null;
                if (!nodesNames.contains(first)) {
                    n1 = new Node(first, 0);
                    nodesNames.add(first);
                    nodes.add(n1);
                } else {
                    for (Node cur : nodes) {
                        if (cur.name == first) {
                            n1 = cur;
                        }
                    }
                }
                if (!nodesNames.contains(second)) {
                    n2 = new Node(second, 0);
                    nodesNames.add(second);
                    nodes.add(n2);
                } else {
                    for (Node cur : nodes) {
                        if (cur.name == second) {
                            n2 = cur;
                        }
                    }
                }
                Arc arc = new Arc(arcName, n1, n2, u, c);
                arcs.add(arc);
            }
        }
    }

    /******** INITIALIZING RESIDUAL GRAPH ********/
    /*
     * Returns: void
     */
    public void initResidualGraph(Graph g) {
        // this.nodes = g.nodes;
        for (Node node : g.nodes) {
            this.nodes.add(node);
        }
        for (Arc arc : g.arcs) {
            this.arcs.add(new Arc(arc.name, arc.n1, arc.n2, arc.u - arc.x, arc.c));
            this.arcs.add(new Arc("r" + arc.name, arc.n2, arc.n1, arc.x, -1 * arc.c));
        }
    }

    /******** helper func - findPathFromSourceToSink ********/
    /*
     * Function: searches the residual graph to see if there is a
     * path from source to sink with positive risidual capacity.
     * 
     * Returns: True or false if there is a path that exists
     * 
     * Updates: Parent params of nodes for backtracing and MinResCap
     * for updating capacity along arcs in residual graph
     */
    public boolean findPathFromSourceToSink(Node s, Node t) { // BFS
        int minResCap = Integer.MAX_VALUE; // stores
        for (Node node : this.nodes) {
            node.viewed = false;
            node.parent = null;
        }

        LinkedList<Node> queue = new LinkedList<Node>();
        s.viewed = true;
        s.parent = null;
        queue.add(s);

        while (queue.size() != 0) {
            Node cur = queue.poll();
            // System.out.println("searched: " + cur.name);
            for (Arc arc : arcs) {
                if (arc.n1.equals(cur) && !arc.n2.viewed && arc.u > 0) {
                    if (arc.u < minResCap) {
                        minResCap = arc.u;
                        // System.out.println("updated minrescap to " + minResCap + " from arc " +
                        // arc.name);
                    }
                    arc.n2.parent = arc.n1;

                    if (arc.n2.equals(t)) {
                        // System.out.println("final minrescap: " + minResCap);
                        arc.n2.minResCap = minResCap;
                        return true;
                    }

                    queue.add(arc.n2);
                    arc.n2.viewed = true;
                }
            }
        }
        return false;
    }
    /******** helper function: FORD-FULKERSON ********/
    /*
     * Returns: void
     */
    public void modifiedFordFulkerson(Node s, Node t) {
        while (this.findPathFromSourceToSink(s, t)) {
            for (Node cur = t; !cur.equals(s); cur = cur.parent) {
                for (Arc rarc : this.arcs) {
                    if (rarc.n1.equals(cur) && rarc.n2.equals(cur.parent)) { // add min residual capacity to reverse arc
                        rarc.u += t.minResCap;
                    } else if (rarc.n2.equals(cur) && rarc.n1.equals(cur.parent)) { // subtract residual capacity from
                                                                                    // forward arc
                        rarc.u -= t.minResCap;
                    }
                }
            }
        }
    }

    /******** Generate feasible flow ********/
    /*
     * Returns: Boolean if a feasible flow has been detected
     */
    public boolean generateFeasibleFlow() {
        Graph rFordFulkerson = new Graph();
        rFordFulkerson.initResidualGraph(this);

        int nameSource = -1;
        int nameSink = -2;
        Node s = new Node(nameSource, 0);
        rFordFulkerson.nodes.add(s);
        Node t = new Node(nameSink, 0);
        rFordFulkerson.nodes.add(t);

        for (Node node : rFordFulkerson.nodes) {
            if (node.supply > 0) {
                Arc sourceArc = new Arc("s" + node.name, s, node, node.supply, 0); // capacity along source arc is equal
                                                                                   // to supply of the source node
                rFordFulkerson.arcs.add(sourceArc);
                Arc rSourceArc = new Arc("rs" + node.name, node, s, 0, 0);
                rFordFulkerson.arcs.add(rSourceArc);
            } else if (node.supply < 0) {
                Arc sinkArc = new Arc("t" + node.name, node, t, -1 * node.supply, 0); // capcity along sink arc is equal
                                                                                      // to the opposite of the supply
                                                                                      // of the sink node
                rFordFulkerson.arcs.add(sinkArc);
                Arc rSinkArc = new Arc("rs" + node.name, t, node, 0, 0);
                rFordFulkerson.arcs.add(rSinkArc);
            }
        }

        rFordFulkerson.modifiedFordFulkerson(s, t);

        for (Arc rarc : rFordFulkerson.arcs) { // There is a feasible flow if and only if capacity is maximized
                                               // (residual
            // capacity = 0) from all edges flowing out of s and into t
            if (rarc.n1.equals(s)) {
                if (rarc.u != 0) {
                    return false;
                }
            } else if (rarc.n2.equals(t)) {
                if (rarc.u != 0) {
                    return false;
                }
            }
        }

        for (Arc arc : this.arcs) { // update flow on arcs of original graph
            for (Arc rarc : rFordFulkerson.arcs) {
                if (arc.n1.equals(rarc.n1) && arc.n2.equals(rarc.n2)) {
                    arc.x = arc.u - rarc.u;
                }
            }
        }
        return true;
    }

    /******** helper function: Modified Bellman-Ford Algorithm ********/
    /*
     * Returns: Boolean if a a negative cycle has been detected
     */
    public boolean modifiedBellmanFordNegCycleDetect() {
        // 1) establish parent and distance variables with initial conditions
        for (Node node : nodes) {
            node.parent = null;
            node.inNegCycle = false;
            node.dist = Integer.MAX_VALUE;
            // set distance for source nodes to 0, distance to itself is 0
            if (node.supply > 0) {
                node.dist = 0;
            }
        }

        // 2) Relax each index in our node set by updating shortest distance to each
        // node |nodes| - 1 times
        for (int i = 0; i < nodes.size(); i++) {
            for (Arc arc : arcs) {
                if ((arc.n2.dist > arc.n1.dist + arc.c) && (arc.n1.dist != Integer.MAX_VALUE)) {
                    arc.n2.dist = arc.n1.dist + arc.c;
                    arc.n2.parent = arc.n1;
                }
            }
        }
        List<Node> cycle = new ArrayList<>();
        Node foundCycle = null;
        // 3) Repeat to detect any negative cycles with arcs with positive residual
        // capacity, return 0 if detected (step 2 should
        // be optimal, so if there is another shorter path, there must be a negative
        // cycle)
        for (Arc arc : arcs) {
            if ((arc.n2.dist > arc.n1.dist + arc.c) && (arc.n1.dist != Integer.MAX_VALUE) && (arc.u > 0)) {
                foundCycle = arc.n2;
                break;
            }
        }
        // 4) if no negative cycle detected, return false
        if (foundCycle == null) {
            return false;
        }

        //not necessary, used for testing
        Node startNode = foundCycle;
        Node prevNode = null;
        while (true) {
            prevNode = startNode.parent;
            if (cycle.contains(prevNode)) {
                break;
            }
            cycle.add(prevNode);
            prevNode.inNegCycle = true;
            startNode = prevNode;
        }

        int i = cycle.indexOf(prevNode);
        for (int j = 0; j < i; j++) {
            cycle.remove(j);
        }

        return true;
    }

    /******** NEGATIVE CYCLE CANCELING ALGORITHM ********/
    /*
     * Returns: Boolean, true if a a negative cycle has been detected and the updates have been performed
     *                   false if no negative cycle has been detected, optimal solution has been found
     */
    public boolean negativeCycleCanceling() {
        // 1) generate residual graph
        Graph rNCC = new Graph();
        rNCC.initResidualGraph(this);

        // 2) detect negative cycles (function is above)
        if (rNCC.modifiedBellmanFordNegCycleDetect()) {
            Set<Arc> negCycle = new HashSet<>();
            int uc = Integer.MAX_VALUE;

            // 3) search arcs for arcs in negative cycle based on parent vals and store minimum residual capacity
            for (Arc rarc : rNCC.arcs) {
                if (rarc.n1.inNegCycle && rarc.n2.inNegCycle && rarc.n2.parent.equals(rarc.n1)) {
                    negCycle.add(rarc);
                    if (rarc.u < uc) {
                        uc = rarc.u;
                    }

                }
            }
            //if the residual capacity is negative, do nothing and iterate again (test for all positive residual capacity)
            if (uc < 0) {
                return true;
            }

            for (Arc negCycleArc : negCycle) { // update residual capacity with minimum residual capacity along arcs in neg cycle
                for (Arc rarc : rNCC.arcs) {
                    if (negCycleArc.n1.equals(rarc.n1) && negCycleArc.n2.equals(rarc.n2)) {
                        rarc.u -= uc;
                    } else if (negCycleArc.n1.equals(rarc.n2) && negCycleArc.n2.equals(rarc.n1)) {
                        rarc.u += uc;
                    }
                }
            }
            for (Arc arc : this.arcs) { // update flow on arcs of original graph
                for (Arc rarc : rNCC.arcs) {
                    if (arc.n1.equals(rarc.n1) && arc.n2.equals(rarc.n2)) {
                        arc.x = arc.u - rarc.u;
                    }
                }
            }
            return true;
        }
        //return false to exit if no negative cycle has been detected
        return false;

    }

    public static void main(String[] args) throws Exception {
        Graph g = new Graph();
        g.initGraph();

        if (!g.generateFeasibleFlow()) {
            System.out.println("There is no feasible flow in the selected graph");
            System.exit(0);
        }
        System.out.println("Feasible flow detected, running negative cycle cancelling on the following feasible flow:");
        for (Arc arc : g.arcs) {
            System.out.println(arc.name + ": " + arc.x);
        }
        int iter = 0;
        while (g.negativeCycleCanceling()) {
            iter++;
            System.out.println(iter);
            System.out.println("updated flow: ");
            for (Arc arc : g.arcs) {
                System.out.println(arc.name + ": " + arc.x);
            }
        }
        System.out.println("Final minimum cost flow after negative cycle canceling is: ");
        int sum = 0;
        for (Arc arc : g.arcs) {
            System.out.println(arc.name + "| flow: " + arc.x + ", cost: " + (arc.c * arc.x));
            sum += (arc.c * arc.x);
        }
        System.out.println("Final Minimum cost: " + sum);
    }
}
