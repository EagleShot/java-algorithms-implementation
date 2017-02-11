
/******************************************************************************
 *  Compilation:  javac SegmentTree.java
 *  Execution:    java SegmentTree
 *  
 *  A segment tree data structure.
 *
 ******************************************************************************/

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The {@code SegmentTree} class is an structure for efficient search of cummulative data.
 * It performs  Range Minimum Query and Range Sum Query in O(log(n)) time.
 * It can be easily customizable to support Range Max Query, Range Multiplication Query etc.
 * <p>
 * Also it has been develop with  {@code LazyPropagation} for range updates, which means
 * when you perform update operations over a range, the update process affects the least nodes as possible
 * so that the bigger the range you want to update the less time it consumes to update it. Eventually those changes will be propagated
 * to the children and the whole array will be up to date.
 * <p>
 * Example:
 * <p>
 * SegmentTreeHeap st = new SegmentTreeHeap(new Integer[]{1,3,4,2,1, -2, 4});
 * st.update(0,3, 1)
 * In the above case only the node that represents the range [0,3] will be updated (and not their children) so in this case
 * the update task will be less than n*log(n)
 *
 * Memory usage:  O(n)
 *
 * @author Ricardo Pacheco 
 */
public class SegmentTree {

    private Node[] heap;
    private long[] array;
    private int size;
    private ArrayList<FunctionObj> funcs;
    /**
     * Time-Complexity:  O(n*log(n))
     *
     * @param array the Initialization array
     */
    public SegmentTree(long[] array, ArrayList<FunctionObj> funcs) {
    	this.funcs = funcs;
        this.array = Arrays.copyOf(array, array.length);
        //The max size of this array is about 2 * 2 ^ log2(n) + 1
        size = (int) (2 * Math.pow(2.0, Math.floor((Math.log((double) array.length) / Math.log(2.0)) + 1)));
        heap = new Node[size];
        build(1, 0, array.length);
    }


    public int size() {
        return array.length;
    }

    //Initialize the Nodes of the Segment tree
    
    private void build(int v, int from, int size) {
        heap[v] = new Node(funcs.size());
        heap[v].from = from;
        heap[v].to = from + size - 1;

        if (size == 1) {
        	for(int i = 0; i < funcs.size(); ++i){
        		heap[v].accums[i] = array[from];
        	}
        } else {
            //Build childs
            build(2 * v, from, size / 2);
            build(2 * v + 1, from + size / 2, size - size / 2);

            for(int i = 0; i < funcs.size(); ++i){

                heap[v].accums[i] = funcs.get(i).func(heap[2 * v].accums[i], heap[2 * v + 1].accums[i]);
            }
        }
    }

    
    public long query(int from, int to, int funcInd){
    	return query(1, from, to, funcInd);
    }
    
    public long query(int v, int from, int to, int funcInd){
    	Node n = heap[v];

        //If you did a range update that contained this node, you can infer the Sum without going down the tree
        if (n.pendingVal != null && contains(n.from, n.to, from, to)) {
            return funcs.get(funcInd).combine((to - from + 1), n.pendingVal);
        }

        if (contains(from, to, n.from, n.to)) {
            return heap[v].accums[funcInd];
        }

        if (intersects(from, to, n.from, n.to)) {
            propagate(v);
            long leftVal = query(2 * v, from, to,funcInd);
            long rightVal = query(2 * v + 1, from, to,funcInd);

            return funcs.get(funcInd).func(leftVal, rightVal);
        }

        return funcs.get(funcInd).ident;
    }
    
    

    /**
     * Range Update Operation.
     * With this operation you can update either one position or a range of positions with a given number.
     * The update operations will update the less it can to update the whole range (Lazy Propagation).
     * The values will be propagated lazily from top to bottom of the segment tree.
     * This behavior is really useful for updates on portions of the array
     * <p>
     * Time-Complexity: O(log(n))
     *
     * @param from  from index
     * @param to    to index
     * @param value value
     */
    public void update(int from, int to, long value) {
        update(1, from, to, value);
    }

    private void update(int v, int from, int to, long value) {

        //The Node of the heap tree represents a range of the array with bounds: [n.from, n.to]
        Node n = heap[v];

        /**
         * If the updating-range contains the portion of the current Node  We lazily update it.
         * This means We do NOT update each position of the vector, but update only some temporal
         * values into the Node; such values into the Node will be propagated down to its children only when they need to.
         */
        if (contains(from, to, n.from, n.to)) {
            change(n, value);
        }

        if (n.size() == 1) return;

        if (intersects(from, to, n.from, n.to)) {
            /**
             * Before keeping going down to the tree We need to propagate the
             * the values that have been temporally/lazily saved into this Node to its children
             * So that when We visit them the values  are properly updated
             */
            propagate(v);

            update(2 * v, from, to, value);
            update(2 * v + 1, from, to, value);

            for(int i = 0; i < funcs.size(); ++i){
            	n.accums[i]= funcs.get(i).func(heap[2*v].accums[i], heap[2*v+1].accums[i]);
            }
        }
    }

    //Propagate temporal values to children
    private void propagate(int v) {
        Node n = heap[v];

        if (n.pendingVal != null) {
            change(heap[2 * v], n.pendingVal);
            change(heap[2 * v + 1], n.pendingVal);
            n.pendingVal = null; //unset the pending propagation value
        }
    }

    //Save the temporal values that will be propagated lazily
    private void change(Node n, long value) {
        n.pendingVal = value;
        for(int i = 0; i < funcs.size(); ++i){
        	n.accums[i] = funcs.get(i).combine(value, n.size());
        }
        array[n.from] = value;

    }

    //Test if the range1 contains range2
    private boolean contains(int from1, int to1, int from2, int to2) {
        return from2 >= from1 && to2 <= to1;
    }

    //check inclusive intersection, test if range1[from1, to1] intersects range2[from2, to2]
    private boolean intersects(int from1, int to1, int from2, int to2) {
        return from1 <= from2 && to1 >= from2   //  (.[..)..] or (.[...]..)
                || from1 >= from2 && from1 <= to2; // [.(..]..) or [..(..)..
    }

    //The Node class represents a partition range of the array.
    static class Node {
    	
    	public Node(int numAccums){
    		accums = new long[numAccums];
    	}
    	long[] accums;
        //Here We store the value that will be propagated lazily
        Long pendingVal = null;
        int from;
        int to;

        int size() {
            return to - from + 1;
        }

    }

    /**
     * Read the following commands:
     * init n v     Initializes the array of size n with all v's
     * set a b c... Initializes the array  with [a, b, c ...]
     * rsq a b      Range Sum Query for the range [a, b]
     * rmq a b      Range Min Query for the range [a, b]
     * up  a b v    Update the [a,b] portion of the array with value v.
     * exit
     * <p>
     * Example:
     * init
     * set 1 2 3 4 5 6
     * rsq 1 3
     * Sum from 1 to 3 = 6
     * rmq 1 3
     * Min from 1 to 3 = 1
     * input up 1 3
     * [3,2,3,4,5,6]
     *
     * @param args the command-line arguments
     */

    
    public static void main(String[] args){
    	long[] vals = new long[6];
    	for(int i = 0; i < 6;++i){
    		vals[i]=i;
    	}
    	ArrayList<FunctionObj> myList = new ArrayList<>();
    	myList.add(new Product());
    	SegmentTree myST = new SegmentTree(vals, myList);

    	System.out.println(myST.query(3, 3,0));
    	System.out.println(myST.query(0, 3,0));
    	System.out.println(myST.query(1, 5,0));
    	myST.update(0, 1, 5);
    	System.out.println(myST.query(0, 2,0));
    }

}


abstract class FunctionObj{
	public long ident;
	public abstract long func(long a, long b);

	public long combine(long val, long times){
		if(times==0)
			return ident;
		if(times== 1) 
			return val;
		long half = combine(val, times/2);
		if(times%2 == 0){
			return func(half,half);
		}
		return func(val, func(half,half));
	}
	
}

class Product extends FunctionObj{
	public Product(){
		ident = 1;
	}
	public long func(long a, long b){
		return a*b;
	}
	
}

class Sum extends FunctionObj{
	public Sum(){
		ident = 0;
	}
	public long func(long a, long b){
		return a+b;
	}
	
}
