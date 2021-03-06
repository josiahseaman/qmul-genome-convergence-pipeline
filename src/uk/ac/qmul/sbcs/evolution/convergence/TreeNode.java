package uk.ac.qmul.sbcs.evolution.convergence;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jebl.math.Random;

/**
 * 
 * A utility class for operations on phylogenetic trees.
 * @author <a href="mailto:joe@kitson-consulting.co.uk">Joe Parker, Kitson Consulting / Queen Mary University of London</a>
 * 
 */
// TODO Expand this javadoc
public class TreeNode {
	ArrayList<TreeNode> daughters;
	HashMap<String,Integer> tipNumberingMap = null;
	TreeNode parent;
	boolean isTerminal;
	String content;
	int startPos;
	int endPos;
	int nodeNumber = -1; // numbering of nodes; tips in order unless tip content is entirely numeric, in which case tips == numbers assumed. Internal nodes numbered L-R and root-tip
	double branchLength;
	public HashSet<String>[] states;
	int fitchStateChanges;
	// should only run tree stat calculations once
	private boolean treeStatsCalculated = false;
	// length of branches below, internal only
	private double internalBranchLengths = 0.0d;
	// length of branches below, external only
	private double externalBranchLengths = 0.0d;
	// sum of all internal and external branch lengths
	private double treeLength = 0.0d;
	// longest root-tip path
	private double treeHeight = 0.0d;
	// internal : (internal + external) branch lengths
	private double treeness = 0.0d;
	// external : internal branch lengths
	private double externalInternalRatio = 0.0d;
	// how many internal nodes have only external nodes (tips) as children
	private double cherryCount = 0.0d;
	// sum over internal nodes (abs(left daughter children - right daughter children)) / N
	// NB: assumes bifurcating! 
	// return -1 if polytomy
	private double collessTreeImbalanceNumerator = 0.0d;
	// number of terminal tips below this node (nb =1 ifTerminal)
	private int tipsBelowCount = 0;
	
	/**
	 * Constructor for node
	 * @param tree
	 * @param startAt
	 */
	public TreeNode(String tree, int startAt){
		this.content = tree.replaceAll("\\s+", "");
		this.startPos = startAt;
		this.endPos = startPos;
		this.isTerminal = false;
		this.daughters  = new ArrayList<TreeNode>();
		TreeNode somenode = null;
		String someName = null;
		String someDist = null;
		while(endPos < content.length()){
			char someChar = content.charAt(endPos);
			switch(someChar){
			case '(': //condition new node;
				endPos++;
				somenode = new TreeNode(tree,endPos);
				endPos = somenode.getEndPos();
				break;
			case ')': //condition end node;
				if(someName != null){
					daughters.add(new TreeNode(someName, someDist));
					someName = null;
					someDist = null;
				}else{
					somenode.setDistance(someDist);
					daughters.add(somenode);
					somenode = null;
					someName = null;
					someDist = null;
					
				}
				this.content = "(";
				for(TreeNode daughter:daughters){
					content = content + daughter.getContent() + ",";
				}
				content = content.substring(0, content.length()-1)+")";
				endPos++;
				return;
			case ':': //condition distance for previous node;
				someDist = "";
				endPos++;
				break;
			case ',': //condition second node;
				if(someName != null){
					daughters.add(new TreeNode(someName,someDist));
					someName = null;
					someDist = null;
				}else{
					somenode.setDistance(someDist);
					daughters.add(somenode);
					somenode = null;
					someDist = null;
				}
				endPos++;
				break;
			default:  //condition default;
				if(someName != null){
					if(someDist != null){
						// we have a distance initialised, and a name. The name should be OK, increment dist
						someDist = someDist + someChar;
					}else{
						// we only have a name initialised (probably haven't reached the ':' char yet
						someName = someName + someChar;
					}
				}else{
					if(someDist != null){
						// we have a dist, increment dist
						someDist = someDist + someChar;
					}else{
						// init name
						someName = Character.toString(someChar);
					}
				}
				endPos++;
				break;
			}
		}
	}
	
	/**
	 * Constructor for terminal taxa only
	 * @param name
	 */
	private TreeNode(String name, String brLength){
		this.content = name;
		if(brLength != null){
			this.branchLength = Double.parseDouble(brLength);
		}
		this.isTerminal = true;
		this.daughters = null;
	}
	
	/**
	 * Returns the position the last node constructor left at
	 * @return
	 */
	public int getEndPos(){
		return endPos;
	}
	
	/**
	 * Override the toString method for nodes.
	 */
	@Override
	public String toString(){
		return this.content;
	}
	
	/**
	 * Set the distance - normally for after an internal node has been initialised.
	 * @param brLength
	 */
	private void setDistance(String brLength){
		if(brLength != null){
			this.branchLength = Double.parseDouble(brLength);
		}
	}
	
	/**
	 * Numbering of nodes; tips in order unless tip content is entirely numeric, in which case tips == numbers assumed. Internal nodes numbered L-R and root-tip
	 * @param maxTipNumbering tip numbering from 1 on
	 * @param maxInternalNumbering number of last assigned tip
	 * @return updated last numbering; [tip,internal] (int[])
	 */
	public int[] setNodeNumbers(int maxTipNumbering, int maxInternalNumbering){
		if(isTerminal){
			/*
			 * There are several possible contingencies for tip naming schemes that this method has to address:
			 * 
			 * 	1. Tips are named simply as [A-Za-z] strings, and numbering is not important, e.g.: (((LOXODONTA:0.1,DASYPUS:0.1):0.1,((((CANIS:0.1,(EQUUS:0.1,((TURSIOPS:0.1,BOS:0.1):0.1,VICUGNA:0.1):0.1):0.1):0.1,((PTERONOTUS:0.1,MYOTIS:0.1):0.1,((RHINOLOPHUS:0.1,MEGADERMA:0.1):0.1,(PTEROPUS:0.1,EIDOLON:0.1):0.1):0.1):0.1):0.1,(SOREX:0.1,ERINACEUS:0.1):0.1):0.1,((MUS:0.1,(ORYCTOLAGUS:0.1,OCHOTONA:0.1):0.1):0.1,(PAN:0.1,HOMO:0.1):0.1):0.1):0.1):0.1,MONODELPHIS:0.1)
			 * 		(in this case numbering would proceed from (1,2..n) in the order tips are encountered.
			 * 	2. Tips are named simply as in (1), but a specific numbering order is required by another class. For instance, PAML may have numbered sequences/taxa alphabetically.
			 * 		(in this case the numbering for the same topology above may look like (((8, 3), ((((2, (5, ((20, 1), 21))), ((16, 12), ((18, 9), (17, 4)))), (19, 6)), ((11, (14, 13)), (15, 7)))), 10) in numbered form.
			 * 	3. Tips are already numeric only (as in the exmaple in (2)
			 * 	4. Tips are a horrible hybrid composite label, e.g. (((8_LOXODONTA, 3_DASYPUS) 24 , ((((2_CANIS, (5_EQUUS, ((20_TURSIOPS, 1_BOS) 31 , 21_VICUGNA) 30 ) 29 ) 28 , ((16_PTERONOTUS, 12_MYOTIS) 33 , ((18_RHINOLOPHUS, 9_MEGADERMA) 35 , (17_PTEROPUS, 4_EIDOLON) 36 ) 34 ) 32 ) 27 , (19_SOREX, 6_ERINACEUS) 37 ) 26 , ((11_MUS, (14_ORYCTOLAGUS, 13_OCHOTONA) 40 ) 39 , (15_PAN, 7_HOMO) 41 ) 38 ) 25 ) 23 , 10_MONODELPHIS) 22 ;
			 * 
			 * This class has ^attempted^ to cover all these but focussed mainly on (1) and (2). Note that support for tip labelling with specific name-ID mappings is supported through the setTipNameNumberMapping() method, in which a HashMap<String,Integer> is used to specify them.
			 * Note also that this is not very well tested for odd mixtures of labels (some numeric, some alphabetical, some both), or for whitespace / special chars. 
			 */
			//enable parsing of Rod Paige / TreeView format strings, containing IDs and names. e.g. 10_MONODELPHIS should receive '10'.
			// TODO grep on /^[0-9]+/ e.g. greedy on opening contiguous numbers?
			Pattern digitStart = Pattern.compile("^[0-9]+");
			Matcher digitMatch = digitStart.matcher(content);
			if(digitMatch.find()){
				// a number is present at the start of the content string
				try {
					// try and parse the tip content as a number is present
					int parsedNumber = Integer.parseInt(digitMatch.group());
					this.nodeNumber = parsedNumber;
					if(nodeNumber > maxTipNumbering){maxTipNumbering = nodeNumber;}
				} catch (NumberFormatException e) {
					// either no number is present, or we can't parse it
					//e.printStackTrace(); we don't really need the stack trace
					// assign numnber for this tip de novo
					maxTipNumbering++;
					this.nodeNumber = maxTipNumbering;
				}
			} else {
				// this content string appears to be alphanumeric, at least, numerals are not present at the start of the string
				// increment and label tip numbers as normal (so that maxtipNumbering == number of tips in the tree), but we'll check to see if a specific name-ID mapping exists too.
				maxTipNumbering++;
				this.nodeNumber = maxTipNumbering;
				// see if the tipNumberingMap is set
				if(this.tipNumberingMap != null){
					// note that nodeNumber and tempNumbering are ^not^ initialised. this is because the ID returned by tipNumberingMap.get(content) could have any value and is not predictable. 
					// this means that iniialising to 0 and testing for inequality (or any other test) might give odd results.
					try {
						// there is a map present, see if this tip content has a numbering ID specified
						int tempNumbering = tipNumberingMap.get(content);
						this.nodeNumber = tempNumbering;
					} catch (Exception e) {
						// no tip found with a numbering specified
					}
				}
			}
		}else{
			maxInternalNumbering++;
			this.nodeNumber = maxInternalNumbering;
			for(TreeNode daughter:daughters){
				int[] lastNodeNumberings = daughter.setNodeNumbers(maxTipNumbering, maxInternalNumbering);
				maxTipNumbering = lastNodeNumberings[0];
				maxInternalNumbering = lastNodeNumberings[1];
			}
		}
		int[] retVals = {maxTipNumbering,maxInternalNumbering};
		return retVals;
	}

	/**
	 * Not a very clearly encapsulated method. This call achieves <i>three</i> things:
	 * 	<ol>
	 * 		<li>Set the states of the terminal taxa by matching the {@link TreeNode#content} for any {@link TreeNode#isTerminal} nodes to the corresponding String keyin the inputStates HashMap;</li>
	 * 		<li>Set the states of the internal nodes of the tree, using the first pass of the Fitch (1967) algorithm (union or intersection) and the daughter node states</li>
	 * 		<li>Return the states of the current node, e.g. the top  (root) node of the tree if this method is called on that node. These may also be ambiguous.</li>
	 * </ol>
	 * This calls a post-order (leaves-to-root) traversal of the tree, terminal taxa will have their states determined by the input list.
	 * <p>Execution:
	 * <pre>
	 * if(isTerminal){
	 * 	states = inputStates.get(name)
	 *  return states
	 * }else{
	 * 	 leftDaughterStates = daughters[0].getFitchStates(inputStates)
	 * 	rightDaughterStates = daughters[1].getFitchStates(inputStates)
	 *  for(i, stateslength){
	 *  	resolve fitch states; e.g. if no intersection between left & right states, union; else intersection
	 *  }
	 *  return states
	 * }
	 * </pre>
	 * @param states; a HashMap of all of the states for terminal taxa
	 * @return post-order traversal will give sets of possible states for this node, pass up.
	 */
	@Deprecated
	/**
	 * WARNING WARNING WARNING WARNING WARNING
	 * Recently (r177; 25/05/2013) refactored daughters to ArrayList<TreeNode> from TreeNode[]
	 * THE CURRENT IMPLEMENTATION OF THIS METHOD DOES NOT ACCOUNT FOR daughters>2
	 * @TODO
	 */
	public HashSet<String>[] getFitchStates(HashMap<String, HashSet<String>[]> inputStates) {
		if(isTerminal){
			this.states = inputStates.get(content);
			return states;
		}else{
			HashSet<String>[]  leftStates = this.daughters.get(0).getFitchStates(inputStates);
			HashSet<String>[] rightStates = this.daughters.get(1).getFitchStates(inputStates);
			this.fitchStateChanges += this.daughters.get(0).getFitchStateChanges();
			this.fitchStateChanges += this.daughters.get(1).getFitchStateChanges();
			if(leftStates.length != rightStates.length){
				throw new IllegalArgumentException("Fitch reconstruction: lengths of daughters' state arrays don't match!");
			}else{
				this.states = (HashSet<String>[]) Array.newInstance(HashSet.class, leftStates.length);
				for(int i=0;i<leftStates.length;i++){
					// We'll use HashSets to hold the states, remembering that entries are unique
					HashSet<String> leftState =  leftStates[i];
					HashSet<String>rightState = rightStates[i];
					HashSet<String> unionSet = new HashSet<String>();		// Holds *all* states observed in either daughter nodes
					HashSet<String> intersectionSet = new HashSet<String>();// Holds *only* states seen in *both* daughters
					// Iterate through states in the left node (daughter)
					for(String someState:leftState){
						if(rightState.contains(someState)){
							// This particular state from the left node is also present in the right node, add to the intersection set
							intersectionSet.add(someState);
						}else{
							// This particular state from the left node is absent from the right node, but we will add it to the union set in case we need to pass that up
							unionSet.add(someState);
						}
					}
					if(intersectionSet.isEmpty()){
						/* There are no states from left present in right. So we need to pass up the union of left and right nodes (daughters).
						 * create the union set (already done for left, add all right)
						 */
						//	this.fitchStateChanges++;
						this.fitchStateChanges += unionSet.size()-1;
						unionSet.addAll(rightState);
						this.fitchStateChanges += rightState.size()-1;
						states[i] = unionSet;
					}else{
						/* 
						 * The 'intersection' set is not empty; meaning that there 
						 * is at least one state present in both left and right. 
						 * 
						 * According to the Fitch (1967a) algorithm we'll therefore 
						 * pass all the common states up the tree. 
						 * 
						 * There may be 1 or more than one in the intersection set; 
						 * it doesn't matter as we'll resolve them in the second 
						 * tree traversal.
						 */
						states[i] = intersectionSet;
					}
				}
				return states;
			}
		}
	}
	
	/**
	 * Performs <i>no</i> traversal to set the fitch states, simply randomly picks any of the available states if ambiguous (e.g. states[i].size()>1).
	 */
	public void resolveFitchStatesTopnode(){
		for(int i=0;i<states.length;i++){
			if(states[i].size() > 1){
				// we must be the top node, argh. pick a parent state
				int whichParentState = Random.nextInt(states[i].size());
				Object[] someState = states[i].toArray();
				states[i] =  new HashSet<String>();
				states[i].add((String)someState[whichParentState]);
			}
		}
	}

	/**
	 * Performs the pre-order (root-leaves) traversal to set the fitch states, 
	 * <br/>ASSUMING: 
	 * <ul>
	 * <li>a pre-order traversal has occured to set the states via {@link TreeNode#getFitchStates()};</i>
	 * <li><i>and</i> any ambiguous states present in the top node have been resolved (randomly) via {@link TreeNode#resolveFitchStatesTopnode()}</li>
	 * </ul>
	 */
	public void resolveFitchStates(HashSet<String>[] parentStates){
		if(!this.isTerminal){
			if(this.states == null){
				throw new NullPointerException("get fitch state changes error: states not assigned yet!");
			}else{
				for(int i=0;i<parentStates.length;i++){
					Object[] parentState = parentStates[i].toArray();
					if(states[i].contains((String)parentState[0])){
						// we must be the top node, argh. pick a parent state
						states[i] = parentStates[i];
					}else{
						int whichState = Random.nextInt(states[i].size());
						Object[] someState = states[i].toArray();
						states[i] =  new HashSet<String>();
						states[i].add((String)someState[whichState]);
					}
				}
				for(TreeNode daughter:daughters){
					daughter.resolveFitchStates(states);
				}
			}
		}
	}

	/*
	 *	TREE STATS SECTION...
	 *	TODO - implement these. Issue #46 
	 *
	// 	daughterInternalBranchLengths:	length of branches below, internal only

	// daughterExternalBranchLengths: 	length of branches below, external only
	
	// treeLength:	sum of all internal and external branch lengths
	
	// treeHeight:	longest root-tip path
	
	// treeness:	internal : (internal + external) branch lengths
	
	// externalInternalRatio:	external : internal branch lengths
	
	// cherryCount:	how many internal nodes have only external nodes (tips) as children
	
	// colless:	sum over internal nodes (abs(left daughter children - right daughter children)) / N
	// NB: assumes bifurcating!	return -1 if polytomy
	 */

	public void calculateTreeStats(){
		if(treeStatsCalculated){
			return;
		}else{
			if(!this.isTerminal){
				/*
				 *  calculate tree stats for an internal node:
				 *  	tree length
				 */
				boolean isCherry = true;
				for(TreeNode daughter:daughters){
					// calculate tree stats for lower nodes and get their lengths, add to this
					daughter.calculateTreeStats();
					// treeLength:	sum of all internal and external branch lengths
					this.treeLength+=daughter.getTreeLength();
					// treeHeight:	longest root-tip path
					this.treeHeight = Math.max(this.treeHeight, daughter.getTreeHeight());
					// cherryCount:	how many internal nodes have only external nodes (tips) as children
					if(!daughter.isTerminal){ isCherry = false; }
					this.cherryCount+=daughter.getTreeCherryCount();
					// 	internalBranchLengths:	length of branches below, internal only
					this.internalBranchLengths+=daughter.getInternalBranchLengths();
					// externalBranchLengths: 	length of branches below, external only
					this.externalBranchLengths+=daughter.getExternalBranchLengths();
					// count of tips below this node
					this.tipsBelowCount+=daughter.getCountTipsBelow();
				}
				// colless:	sum over internal nodes (abs(left daughter children - right daughter children)) / N
				// NB: assumes bifurcating!	return -1 if polytomy
				// return the numerator only
				if(daughters.size()==2){
					int daughterLeftTips  = daughters.get(0).getCountTipsBelow();
					int daughterRightTips = daughters.get(1).getCountTipsBelow();
					int C = 2*(Math.abs(daughterLeftTips - daughterRightTips));
					this.collessTreeImbalanceNumerator = 
							daughters.get(0).getTreeCollessTreeImbalanceNumerator() +
							daughters.get(1).getTreeCollessTreeImbalanceNumerator() +
							C;
				}else{
					this.collessTreeImbalanceNumerator = Double.NaN;
				}
				// postorder stats
				// 	internalBranchLengths:	length of branches below, internal only
				this.internalBranchLengths = this.internalBranchLengths+this.branchLength;
				
				// externalBranchLengths: 	length of branches below, external only
				// this.externalBranchLengths = this.externalBranchLengths;	nothing to do
				
				// treeLength:	sum of all internal and external branch lengths
				this.treeLength = this.treeLength+this.branchLength;
				
				// treeHeight:	longest root-tip path
				this.treeHeight = this.treeHeight+this.branchLength;
				
				// treeness:	internal : (internal + external) branch lengths
				this.treeness = this.internalBranchLengths / (this.internalBranchLengths + this.externalBranchLengths);

				// externalInternalRatio:	external : internal branch lengths
				this.externalInternalRatio = this.externalBranchLengths / this.internalBranchLengths;

				// cherryCount:	how many internal nodes have only external nodes (tips) as children
				if(isCherry){ this.cherryCount++; }
				// colless:	sum over internal nodes (abs(left daughter children - right daughter children)) / N
				// NB: assumes bifurcating!	return -1 if polytomy
				// return the numerator only
			}else{
				/*
				 *  calculate tree stats for a tip node:
				 *  	tree length
				 */
				// 	daughterInternalBranchLengths:	length of branches below, internal only
				this.internalBranchLengths = 0;
				
				// daughterExternalBranchLengths: 	length of branches below, external only
				this.externalBranchLengths = this.branchLength;

				// treeLength:	sum of all internal and external branch lengths
				this.treeLength = this.branchLength;
				
				// treeHeight:	longest root-tip path
				this.treeHeight = this.branchLength;
				
				// treeness:	internal : (internal + external) branch lengths
				this.treeness = Double.NaN;
				
				// externalInternalRatio:	external : internal branch lengths
				this.externalInternalRatio = Double.NaN;
				
				// cherryCount:	how many internal nodes have only external nodes (tips) as children
				this.cherryCount = 0;
				
				// colless:	sum over internal nodes (abs(left daughter children - right daughter children)) / N
				// NB: assumes bifurcating!	return -1 if polytomy
				this.collessTreeImbalanceNumerator = 0;
				
				// number of tips below, 1
				this.tipsBelowCount = 1;
			}
		}
		this.treeStatsCalculated = true;
	}

	/*
	 *	...TREE STATS SECTION.
	 */
	
	// number of tips below this node (nb if isTerminal, returns 1)
	public int getCountTipsBelow() {
		if(isTerminal){
			return 1;
		}else{
			if(!treeStatsCalculated){
				this.calculateTreeStats();
			}
			return this.tipsBelowCount;
		}
	}

	// sum of all internal and external branch lengths
	public double getTreeLength(){
		if(!treeStatsCalculated){
			this.calculateTreeStats();
		}
		return this.treeLength;
	}

	// how many internal nodes have only external nodes (tips) as children
	public double getTreeCherryCount() {
		if(!treeStatsCalculated){
			this.calculateTreeStats();
		}
		return this.cherryCount;
	}

	/**
	 * Colless (1982):
	 * 
	 * Sum over internal nodes 2*(abs(left daughter children - right daughter children)) 
	 * NB: assumes bifurcating! 
	 * 
	 * To get the full Colless stat, C / (n-1)(n-2), use the getTreeCollessNormalised() method.
	 * 
	 * @return int the NUMERATOR only of the Colless (1982) statistic. NaN if polytomy
	 */
	public double getTreeCollessTreeImbalanceNumerator() {
		if(!treeStatsCalculated){
			this.calculateTreeStats();
		}
		return this.collessTreeImbalanceNumerator;
	}

	/**
	 * Colless (1982):
	 * 
	 * Sum over internal nodes 2*(abs(left daughter children - right daughter children)) 
	 * NB: assumes bifurcating! 
	 * 
	 * This method DOES NOT traverse the tree, therefore can only give sensible results if called on the top (root) node.
	 * Returns Double.NaN if any polytomies.
	 * Tree traversal for the numerator of the statistic is calculated/held by getTreeCollessTreeImbalanceNumerator()
	 * 
	 * @return double the NORMALISED Colless (1982) statistic. 
	 */
	public double getTreeCollessNormalised() {
		if(!treeStatsCalculated){
			this.calculateTreeStats();
		}
		return this.collessTreeImbalanceNumerator / ( (this.getCountTipsBelow() - 1) * (this.getCountTipsBelow() - 2) );
	}

	// internal : (internal + external) branch lengths
	public double getTreeTreeness() {
		if(!treeStatsCalculated){
			this.calculateTreeStats();
		}
		return this.treeness;
	}

	// longest root-tip path
	public double getTreeHeight() {
		if(!treeStatsCalculated){
			this.calculateTreeStats();
		}
		return this.treeHeight;
	}

	// external : internal branch lengths
	public double getTreeExternalInternalRatio() {
		if(!treeStatsCalculated){
			this.calculateTreeStats();
		}
		return this.externalInternalRatio;
	}

	// length of branches below, external only
	public double getExternalBranchLengths() {
		if(!treeStatsCalculated){
			this.calculateTreeStats();
		}
		return this.externalBranchLengths;
	}

	// length of branches below, internal only
	public double getInternalBranchLengths() {
		if(!treeStatsCalculated){
			this.calculateTreeStats();
		}
		return this.internalBranchLengths;
	}

	private int getFitchStateChanges(){
		if(this.isTerminal){
			return 0;
		}else{
			if(this.states == null){
				throw new NullPointerException("get fitch state changes error: states not assigned yet!");
			}
			return this.fitchStateChanges;
		}
		
	}
	
	public void printStates(){
		if(isTerminal){
			for(HashSet<String> aState:states){
				Object[] stateArray = aState.toArray();
				System.out.print("\t"+stateArray[0].toString());
			}
			System.out.println("\t"+content);
		}else{
			for(HashSet<String> aState:states){
				Object[] stateArray = aState.toArray();
				for(Object o:stateArray){
					System.out.print("\t"+o.toString());
				}
				System.out.print("|");
			}
			System.out.println("\t"+content);
			for(TreeNode daughter:daughters){
				daughter.printStates();
			}
		}
	}

	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return the states
	 */
	public HashSet<String>[] getStates() {
		return states;
	}

	public void printTree() {
		// TODO Auto-generated method stub
		System.out.println(this.content);
	}
	
	public String printRecursively(){
		if(this.isTerminal){
			return content;
		}else{
			String retString = "(";
			for(TreeNode daughter:daughters){
				retString = retString + daughter.printRecursively() + ",";
			}
			retString = retString.substring(0, retString.length()-1) + ")";
			return retString;
		}
	}

	/**
	 * Prints the recursive (Newick) representation of this node (tree)
	 * @return String containing nodes below this one
	 */
	public String printRecursivelyAsNumberedNodes(){
		if(this.isTerminal){
			return this.nodeNumber+"_"+this.content;
		}else{
			String retString = "(";
			for(TreeNode daughter:daughters){
				retString = retString + daughter.printRecursivelyAsNumberedNodes() + ",";
			}
			retString = retString.substring(0, retString.length()-1) + ") "+this.nodeNumber;
			return retString;
		}
	}

	public String printRecursivelyLabelling(String[] someTaxa){
		if(this.isTerminal){
			if(this.subtreeContains(someTaxa)){
				return content+"#1";
			}else{
				return content;
			}
		}else{
			if(this.subtreeContains(someTaxa)){
				String retString = "(";
				for(TreeNode daughter:daughters){
					retString = retString + daughter.printRecursivelyLabelling(someTaxa) + ",";
				}
				retString = retString.substring(0, retString.length()-1) + ")#1";
				return retString;
			}else{
				String retString = "(";
				for(TreeNode daughter:daughters){
					retString = retString + daughter.printRecursivelyLabelling(someTaxa) + ",";
				}
				retString = retString.substring(0, retString.length()-1) + ")";
				return retString;
			}
		}
	}
	
	public boolean subtreeContains(String[] someTaxa){
		boolean thisContains = false;
		if(this.isTerminal){
			for(String taxon:someTaxa){
				if(taxon.equals(content)){
					thisContains = true;
				}
			}
		}else{
			boolean allContain = true;
			ArrayList<Boolean> daughtersContain = new ArrayList<Boolean>();
			for(TreeNode daughter:daughters){
				daughtersContain.add(daughter.subtreeContains(someTaxa));
			}
			if(daughtersContain.contains(false)){
				allContain = false;
			}
			thisContains = allContain;
		}
		return thisContains;	
	}

	/**
	 * Intended to return the tip states and MRCA of all said tips.
	 * <p>Currently relies on simply stopping and passing ret hashmap when it has size of tipsTotrace.length (+1 for the MRCA)
	 * <p>Therefore the desired tip list <b>MUST</b> have been pruned by {@link TreeNode}.areTipsPresent(HashSet<String> echoMap)} first..
	 * <p>NB also - 'MRCA' is used as a key for the MRCA states, so there <b>Must Not Be Any Tips Labelled 'MRCA'. At. All.</b> (ideally catch or failsafe this)
	 * @param tipsToTrace - the terminal taxon tips that we want the states + MRCA of.
	 * @return {@link HashMap&lt;String,HashSet&lt;String&gt;[]&gt;} - the states
	 */
	public HashMap<String, HashSet<String>[]> getTipAndMRCAStatesOf(HashSet<String> tipsToTrace) {
		HashMap<String,HashSet<String>[]> retMap = new HashMap<String,HashSet<String>[]>();
		if(isTerminal){
			// Just check if this is one of the desired tips
			if(tipsToTrace.contains(content)){
				retMap.put(this.content, this.states);
			}
		}else{
			// An internal node - iterate through daughters
			for(TreeNode daughter:daughters){
				HashMap<String,HashSet<String>[]> daughterContents = daughter.getTipAndMRCAStatesOf(tipsToTrace);
				if(daughterContents != null){
					retMap.putAll(daughterContents);
				}
			}
			// Check to see if we have all the daughters
			if(retMap.size() == tipsToTrace.size()){
				retMap.put("MRCA", this.states);
			}
		}
		return retMap;
	}

	/**
	 * Which of the tips in the supplied list are below this node?
	 * @param echoMap
	 * @return
	 */
	public HashSet<String> areTipsPresent(HashSet<String> echoMap) {
		HashSet<String> retMap = new HashSet<String>();
		if(echoMap.contains(content)){
			retMap.add(content);
		}
		if(!isTerminal){
			for(TreeNode daughter:daughters){
				HashSet<String> daughterContents = daughter.areTipsPresent(echoMap);
				if(daughterContents != null){
					retMap.addAll(daughterContents);
				}
			}
		}
		return retMap; 
	}
	
	/**
	 * Returns the size of the largest <i>monophyletic</i> clade for the given taxa list.
	 * <br/>Hard polytomies (nodes with n>2 daughters) are counted as monophyletic <b>only</b> if all daughters are monophyletic.
	 * @author Joe Parker
	 * @since r194 2013/08/02
	 * @param someTaxa - a {@link HashSet&lt;String&gt;} with target taxa to look for monophly of.
	 * @return #of terminal taxa below that node which are in a reciprocally monophyletic clade.
	 */
	public int howManyFromMonophyleticSet(HashSet<String> someTaxa){
		if(isTerminal){
			if(someTaxa.contains(this.content)){
				// return true if this taxon is in the desired list
				return 1;
			}else{
				return 0;
			}
		}else{
			int howManyMax = 0;
			int howManyTotal = 0;
			int howManyMin = Integer.MAX_VALUE;
			// We will aggregate the max clade sizes up the tree
			for(TreeNode daughter:daughters){
				int daughterHowManyMRCA = daughter.howManyFromMonophyleticSet(someTaxa);
				howManyTotal += daughterHowManyMRCA;
				if(daughterHowManyMRCA > howManyMax){
					// Daughter has a max clade worth aggregating
					howManyMax = daughterHowManyMRCA;
				}
				if(daughterHowManyMRCA < howManyMin){
					// Daughter decrement howManyMin (this should == 0 in the case that the daughter has no taxa from the list)
					howManyMin = daughterHowManyMRCA;
				}
			}
			if(howManyMin == 0){
				// There is at least one daughter with a taxon outside of the taxaList; stop aggregating
				if(howManyMax == 1){
					// The only match(es) in daughters are singletons - since we already know this node is <i>not</i> monophyletic (howManyMin==0) we should not increment.
					return 0;
				}else{
					// This node isn't monophyletic, but at least one of the daughters is; so pass the largest monophyletic value below.
					return howManyMax;
				}
			}else{
				// This node is itself directly monophyletic (no daughters with max clade==0) so pass up the aggregated score
				return howManyTotal;
			}
		}
	}
	
	/**
	 * This method attempts to determine whether there are any monophyletic clades present containing the {@link HashSet&lt;String&gt;} of target taxa.
	 * <p>@Deprecated - this method does <b>not</b> work well if hard polytomies are present (daughters.size()>2); instead use {@link uk.ac.qmul.sbcs.evolution.convergence.TreeNode#howManyFromMonophyleticSet()}
	 * @author Joe Parker
	 * @since r194 2013/08/02
	 * @param someTaxa - a {@link HashSet&lt;String&gt;} with target taxa to look for monophly of.
	 * @return boolean ifMonophyletic
	 */
	@Deprecated
	public boolean containsMonophyleticClade(HashSet<String> someTaxa){
		boolean hasMonophyly = false;
		if(isTerminal){
			// return true if this taxon is in the desired list
			if(someTaxa.contains(this.content)){
				hasMonophyly = true;
			}
		}else{
			int tipsBelow = this.howManyTips();
			int sizeOfMRCA = someTaxa.size();
			/*
			 * If this clade is � desired monophyletic clade, return true if all daughters have monophyly
			 * Else return true if any daughters have monophyly
			 */
			if(tipsBelow <= sizeOfMRCA){
				hasMonophyly = true;
				for(TreeNode daughter:daughters){
					if(!daughter.containsMonophyleticClade(someTaxa)){
						hasMonophyly = false;
					}
				}
			}else{
				if(daughters.size() > 2){
					// TODO correctly evaluate polytomies in the case that a polytomy contains internal, not external, nodes
					// FIXME just checking each (daughters' size == monophyletic set size) WON'T work, as daughter could contain clade without being monophyletic itself
					// for the moment, allow polytomy to dictate a false retval; sort this out later
					
					// FIXME OK, this is just for polytomies entirely of terminal nodes - VERY quick, NOT tested.. 2013 08 01

					// first see if we have a polytomy of only terminal taxa
					if(this.howManyTips() == someTaxa.size()){
						int allTerminalAndMonophyletic = 0;
						for(TreeNode daughter:daughters){
							if(daughter.containsMonophyleticClade(someTaxa) && daughter.isTerminal){
								allTerminalAndMonophyletic++;
							}
						}
						if(allTerminalAndMonophyletic == someTaxa.size()){
							hasMonophyly = true;
						}
					}

					// now, in case that's not applicable (e..g this has > n tips, or tips aren't all terminal, try seeing if the daughters have anything
					for(TreeNode daughter:daughters){
						if(daughter.containsMonophyleticClade(someTaxa) && (!daughter.isTerminal) && (daughter.howManyTips() == someTaxa.size())){
							hasMonophyly = true;
						}
					}
					// end FIXME
				}else{
					for(TreeNode daughter:daughters){
						if(daughter.containsMonophyleticClade(someTaxa)){
							hasMonophyly = true;
						}
					}
				}
			}
		}
		return hasMonophyly;
	}
	
	/**
	 * Simple utility method to count tips attached below this node (post-order traversal).
	 * @return int - number of tips below this node.
	 */
	public int howManyTips(){
		if(isTerminal){
			return 1;
		}else{
			int tips = 0;
			for(TreeNode daughter:daughters){
				tips += daughter.howManyTips();
			}
			return tips;
		}
	}
	
	/**
	 * A method that gets the terminal tips labelling, hopefully in left-right post-order traversal ordering.
	 * @return ArrayList<String> of terminal tips (taxa names) in order
	 */
	public ArrayList<String> getTipsInOrder(){
		ArrayList<String> tips = new ArrayList<String>();
		if(isTerminal){
			tips.add(content);
		}else{
			for(TreeNode daughter:daughters){
				for(String daughterTip:daughter.getTipsInOrder()){
					tips.add(daughterTip);
				}
			}
		}
		return tips;
	}

	/**
	 * Get a list of the taxa (terminal tips) below this node.
	 * @return
	 */
	public String[] getTipsBelow(){
		if(this.isTerminal){
			// Simple, return the taxon name
			String[] below = {this.content};
			return below;
		}else{
			// Internal node. Poll daughters.
			int howManyTips = this.howManyTips();
			String[] below = new String[howManyTips];
			int startIndex = 0;
			for(TreeNode daughter:this.daughters){
				// Get daughter tips, add vals to below array, starting at last index
				String[] daughterTips = daughter.getTipsBelow();
				for(int i=0;i<daughterTips.length;i++){
					below[startIndex+i] = daughterTips[i];
				}
				startIndex += daughterTips.length;
			}
			return below;
		}
	}
	
	/**
	 * A method to retrieve the number (node ID, as set in {@link TreeNode#setNodeNumbers()} method) of the lowest node containing all taxa present in taxaContained, a {@link HashSet} of {@link String}s representing taxa. 
	 * <p>Note that although all taxa <b>must</b> be present (so, this method does not have predictable behaviour in trees where branches have been pruned etc), the most recent clade containing them which will be reported <b>may not actually be <i>strictly</i> monophyletic</b>. That is, other taxa not listed might be present as well: this method <i>only</i> guarantees that the node returned is the lowest containing all members in the target list.</p> 
	 * @param taxaContained - a {@link HashSet}&lt;String&gt; of taxon names.
	 * @return int; ID of the lowest node containing all members of taxaContained; or -1 otherwise.
	 * @see HashSet
	 * @since r293 2014/10/10
	 */
	public int getNodeNumberingIDContainingTaxa(HashSet<String> taxaContained){
		int retval = -1;
		if(isTerminal && (taxaContained.size()==1) && (taxaContained.contains(content))){
			// there is only one taxon sought, and this is it. return this node (tip's) number (ID)
			retval = nodeNumber;
		}else if(!isTerminal){
			// loop through daughters. if any have nonegative retval, we'll pass that up- othereise check this node.
			for(TreeNode daughter:daughters){
				int daughterRetval = daughter.getNodeNumberingIDContainingTaxa(taxaContained);
				retval = Math.max(retval, daughterRetval);
			}
			// if any of the daughters have the taxa required as a monophyletic clade, we should have passed up their IDs. If not, we need to see if this node does (i.e. daughters together comprise target clade)
			if((retval == -1) && (this.areTipsPresent(taxaContained).size() == taxaContained.size())){
				retval = nodeNumber;
			}
		}
		return retval;
	}

	/**
	 * A method to retrieve the number (node ID, as set in {@link TreeNode#setNodeNumbers()} method) of the lowest node containing all taxa present in taxaContained, a {@link HashSet} of {@link String}s representing taxa. 
	 * <p>Note that although all taxa <b>must</b> be present (so, this method does not have predictable behaviour in trees where branches have been pruned etc), the most recent clade containing them which will be reported <b>may not actually be <i>strictly</i> monophyletic</b>. That is, other taxa not listed might be present as well: this method <i>only</i> guarantees that the node returned is the lowest containing all members in the target list.</p> 
	 * @param taxaContained - a {@link HashSet}&lt;String&gt; of taxon names.
	 * @return int[]; {ID of the lowest node containing all members of taxaContained,ID of the node immediately above that}; or -1 otherwise.
	 * @see HashSet
	 */
	public int[] getBranchNumberingIDContainingTaxa(HashSet<String> taxaContained){
		int[] retval = {-1,-1};
		if(isTerminal && (taxaContained.size()==1) && (taxaContained.contains(content))){
			// there is only one taxon sought, and this is it. return this node (tip's) number (ID)
			retval[0] = nodeNumber;
		}else if(!isTerminal){
			// loop through daughters. if any have nonegative retval, we'll pass that up- othereise check this node.
			for(TreeNode daughter:daughters){
				int[] daughterRetval = daughter.getBranchNumberingIDContainingTaxa(taxaContained);
				retval[0] = Math.max(retval[0], daughterRetval[0]);
				if(retval[0] != -1){
					retval[1] = Math.max(retval[1], daughterRetval[1]);
				}
			}
			// if any of the daughters have the taxa required as a monophyletic clade, we should have passed up their IDs. If not, we need to see if this node does (i.e. daughters together comprise target clade)
			if((retval[0] == -1) && (this.areTipsPresent(taxaContained).size() == taxaContained.size())){
				retval[0] = nodeNumber;
			}
			// if the daughter retval is nonnegative then retval[1] needs to be this node ID
			if((retval[0] > -1)&&(retval[0] != nodeNumber)&&(retval[1] == -1)){
				retval[1] = nodeNumber;
			}
		}
		return retval;
	}

	/**
	 * Sets the HashMap<String,Integer> that will contain the taxon names / numeric IDs (both unique). This information is required to ensure that classes using specific tip-ID mappings can specify them (e.g. {@link:uk.ac.qmul.sbcs.evolution.convergence.handlers.documents.parsers.codeml.CodemlAncestralSiteOutputParser} using PAML's tip-numberings, not native sequential numberings.
	 * @param tipNumberMap a HashMap of String,Integer containing the taxon names / numeric IDs (both unique)
	 */
	public void setTipNameNumberMapping(HashMap<String, Integer> tipNumberMap) {
		// set mapping for this node
		if(this.tipNumberingMap == null){
			this.tipNumberingMap = tipNumberMap;
		}
		if(!isTerminal){
			// traverse the tree recursively
			for(TreeNode daughter:daughters){
				daughter.setTipNameNumberMapping(tipNumberingMap);
			}
		}
	}

	/**
	 * A method that extracts the ID (numbering) for tip taxonName within the tree. NB names are matched using String.equals(String) call, so are caSe SeNsITive.
	 * <p><b>IMPORTANT</b> returns 0 with no error/exception if no matching tip can be found. Be warned!
	 * @param taxonName
	 * @return integer unique ID defining the tip which has this taxonName. NB returns 0 if no tip found with no error.
	 */
	public int getTipNumber(String taxonName) {
		int retval = 0;
		// is this a terminal?
		if(isTerminal){
			//it this tip the right one?
			if(content.equals(taxonName)){
				retval = nodeNumber;
			}
			return retval;
		}else{
			//recurse the tree
			for(TreeNode daughter:daughters){
				int searchDaughters = daughter.getTipNumber(taxonName);
				if(searchDaughters != 0){
					retval = searchDaughters;
				}
			}
			return retval;
		}
	}
	
	/**
	 * Return all the branches in the tree by postorder traversal
	 * @return ArrayList<TreeBranch> of all branches below this one
	 */
	public ArrayList<TreeBranch> getBranches(){
		/* First check if node numbering has been set for this tree as we'll need those...*/
		if(this.nodeNumber < 0){
			// TODO this.setNodeNumbers(maxTipNumbering, maxInternalNumbering);
			this.setNodeNumbers(0, this.howManyTips());
			// hoping this works - i.e the topnode, once set, will cause all the lower nodes to be numbered too.
		}
		
		/* Instantiate the return array, same size as n daughter nodes */
		ArrayList<TreeBranch> allLowerBranches = new ArrayList<TreeBranch>();
		
		/* Iterate over daughters */
		for(TreeNode daughter:daughters){
			/* first get a branch from this taxon and daughter */
			TreeBranch daughterBranch = new TreeBranch(this, daughter);
			allLowerBranches.add(daughterBranch);
			/* Next check to see if there are branches to be collected from the daughter. 
			 * We can use the daughterBranch isTerminal state to do this, may as well */
			if(!daughterBranch.endsInTerminalTaxon){
				// daughter isn't terminal, it has tips too. collect them
				ArrayList<TreeBranch> allDaughterBranches = daughter.getBranches();
				allLowerBranches.addAll(allDaughterBranches);
			}
		}
		
		/* Return */
		return allLowerBranches;
	}
	
	/**
	 * Returns a list of relative X,Y positions for line segments representing branches, to be rendered with a Graphics2D.drawLine(x1,y1,x2,y2) call or similar.
	 * <br/>Note this method assumes a strictly bifurcating tree, e.g. n=2 daughters for each node exactly.
	 * <br/>Note also that 'left' and 'right' refer to these two daughters, not left/right orientation on the screen.
	 * <p><b>WARNING this behaviour is not correct for polytomies</b>:<br/>See <a href="https://github.com/lonelyjoeparker/qmul-genome-convergence-pipeline/issues/44">issue #44</a>.
	 * @TODO Fix <a href="https://github.com/lonelyjoeparker/qmul-genome-convergence-pipeline/issues/44">issue #44</a>.
	 * @param startX - Xposition to start from
	 * @param startY - Y pos to start from
	 * @param branchIncrementX - how much to increment each branch by (width)
	 * @param branchIncrementY - how much to increment each branch by (height)
	 * @return - An ArrayList<Integer[]> of branches. Each Integer[4] of the form {x1, y1, x2, y2}. All x will be positive. Those branches 'left' of the root will have -ve x, those right will have +ve.
	 * @see {@link uk.ac.qmul.sbcs.evolution.convergence.gui.DisplayPhylogeny}, {@link uk.ac.qmul.sbcs.evolution.convergence.gui.DisplayPhylogenyPanel}
	 */
	public ArrayList<Integer[]> getBranchesAsCoOrdinates(int startX, int startY, int branchIncrementX, int branchIncrementY){
		// Instantiate the return array
		ArrayList<Integer[]> returnLineCoordinates = new ArrayList<Integer[]>();
		
		// Calculate co-ordinates for this branch, only extend in X-direction
		int endX = startX + branchIncrementX;
		Integer[] thisBranch = new Integer[4];
		thisBranch[0] = startX;
		thisBranch[1] = startY;
		thisBranch[2] = endX;
		thisBranch[3] = startY;
		returnLineCoordinates.add(thisBranch);
		
		// Calculate co-ordinates for the vertical line which will connect the daughters, extend Y direction only
		int endYleft = startY - branchIncrementY;
		int endYright= startY + branchIncrementY;
		Integer[] nodeConnector = new Integer[4];
		nodeConnector[0] = endX;
		nodeConnector[1] = endYleft;
		nodeConnector[2] = endX;
		nodeConnector[3] = endYright;			
		returnLineCoordinates.add(nodeConnector);
		
		// repeat calculation for daughters. assume n=2 daughters exactly. left daughter will have -Y, right daughter will have +Y
		if(!this.isTerminal){
			// can't just iterate - each daughter needs a different Y offset.
			// daughters.get(0)
			TreeNode leftDaughter = daughters.get(0);
			ArrayList<Integer[]> daughterLeftCoords = leftDaughter.getBranchesAsCoOrdinates(endX, endYleft, branchIncrementX, branchIncrementY);
			returnLineCoordinates.addAll(daughterLeftCoords);
			// daughters.get(1)
			TreeNode rightDaughter = daughters.get(1);
			ArrayList<Integer[]> daughterRightCoords = rightDaughter.getBranchesAsCoOrdinates(endX, endYright, branchIncrementX, branchIncrementY);
			returnLineCoordinates.addAll(daughterRightCoords);
		}
		
		// return finished list of co-ordinates
		return returnLineCoordinates;
	}
	
	/**
	 * Returns a list of relative X,Y positions for line segments representing branches, to be rendered with a Graphics2D.drawLine(x1,y1,x2,y2) call or similar.
	 * <br/>Note this method assumes a strictly bifurcating tree, e.g. n=2 daughters for each node exactly.
	 * <br/>Note also that 'left' and 'right' refer to these two daughters, not left/right orientation on the screen.
	 * <p><b>WARNING this behaviour is not correct for polytomies</b>:<br/>See <a href="https://github.com/lonelyjoeparker/qmul-genome-convergence-pipeline/issues/44">issue #44</a>.
	 * @TODO Fix <a href="https://github.com/lonelyjoeparker/qmul-genome-convergence-pipeline/issues/44">issue #44</a>.
	 * @param startX - Xposition to start from
	 * @param startY - Y pos to start from
	 * @param branchIncrementX - how much to increment each branch by (width)
	 * @param branchIncrementY - how much to increment each branch by (height)
	 * @return - An ArrayList<Integer[]> of branches. Each Integer[4] of the form {x1, y1, x2, y2}. All x will be positive. Those branches 'left' of the root will have -ve x, those right will have +ve.
	 * @see {@link uk.ac.qmul.sbcs.evolution.convergence.gui.DisplayPhylogeny}, {@link uk.ac.qmul.sbcs.evolution.convergence.gui.DisplayPhylogenyPanel}
	 */
	public ArrayList<Integer[]> getBranchesAsCoordinatesFromTips(int depth, int tipNumber){
		// Instantiate the return array
		ArrayList<Integer[]> returnLineCoordinates = new ArrayList<Integer[]>();

		/*
		 * The branch co-ords array will have this format:
		 * 	[0] - X1 (start of line X-pos)
		 * 	[1] - Y1 (start of line Y-pos)
		 * 	[2] - X2 (end of line X-pos)
		 * 	[3] - Y2 (end of line Y-pos)
		 *  [4] - depth (how many internal nodes from root)
		 *  [5] - tipNumber (how many terminal tips have already been drawn (and hence Y pos)
		 */
		if(isTerminal){
			// this is a terminal tip. place the co-ordinates at depth*20, (tipNumber-1)*20 and increment tipNumber
			tipNumber++;
			depth++;
			Integer[] thisBranch = new Integer[6];
			thisBranch[0] = depth * 20;
			thisBranch[1] = (tipNumber-1) * 20;
			thisBranch[2] = thisBranch[0] + 20;
			thisBranch[3] = thisBranch[1];
			thisBranch[4] = depth;
			thisBranch[5] = tipNumber;
			returnLineCoordinates.add(thisBranch);	
		}else{
			// this is an internal node so depth is incremented
			depth++;	

			/*
			 * Previous iteration through n=2 daughters exactly
			 * Defines:
			 * 	'left daughter'  == daughters.get(0) 
			 * 	'right daughter' == daughters.get(1) 
			 * This code is safe and works up to commit https://github.com/lonelyjoeparker/qmul-genome-convergence-pipeline/commit/36cc770a0182e3580859eae6e08c76a974607d1c
			 * 
			 * However... issue #44 (polytomies not drawn correctly)
			 * requires a different method which can iterate 
			 * through any n>1 daughters...
			 * 
			 * First, here's the code which _works_:
			 * 
				// calculation for daughters. assume n=2 daughters exactly. 
				// can't just iterate - each daughter needs a different Y offset.
	
				// daughters.get(0)
				TreeNode leftDaughter = daughters.get(0);
				ArrayList<Integer[]> daughterLeftCoords = leftDaughter.getBranchesAsCoordinatesFromTips(depth, tipNumber);
				returnLineCoordinates.addAll(daughterLeftCoords);
				
				// update the last tipNumber reached from last branch of daughter coords (now copied to returnLineCoordinates)
				Integer[] lastLeftDaughterBranch = returnLineCoordinates.get(returnLineCoordinates.size()-1);
				tipNumber = lastLeftDaughterBranch[5]; // NOTE that tipNumber for the *right* branch will be updated from this value (tipNumber from the *left* branch)
				
	
				// daughters.get(1)
				TreeNode rightDaughter = daughters.get(1);
				ArrayList<Integer[]> daughterRightCoords = rightDaughter.getBranchesAsCoordinatesFromTips(depth, tipNumber);
				returnLineCoordinates.addAll(daughterRightCoords);
				
				// update the last tipNumber reached from last branch of daughter coords (now copied to returnLineCoordinates)
				Integer[] lastRightDaughterBranch = returnLineCoordinates.get(returnLineCoordinates.size()-1);
				tipNumber = lastRightDaughterBranch[5];
			 * 
			 * And now, let's try and iterate through *every* element
			 * of the daughters array, even if n>2. We'll still define
			 * left = first and right = last daughter, but we'll have
			 * to generalise a bit...
			 */
			
			// Define left and right daughters, and their co-ordinates/depth/number arrays. 
			// Set them to null explicitly so we can test them
			TreeNode leftDaughter = null;
			ArrayList<Integer[]> daughterLeftCoords, daughterRightCoords;
			Integer[] lastLeftDaughterBranch = null, lastRightDaughterBranch = null;
			
			/* Attempt to iterate through all daughters updating coords list and y-pos (tipNumber) as we go */
			for(TreeNode daughter:daughters){
				ArrayList<Integer[]> daughterCoords = daughter.getBranchesAsCoordinatesFromTips(depth, tipNumber);

				/* If the first (left-most) daughter is null then this is it */
				if(leftDaughter==null){
					// Set first (left-most) daughter params
					leftDaughter = daughter;
					daughterLeftCoords = daughterCoords;
					lastLeftDaughterBranch = daughterLeftCoords.get(daughterLeftCoords.size()-1);
				}
				
				/* Whether this is the first node OR NOT, also assume it is the last node and update right-hand daughter */
				// rightDaughter = daughter; (we could set a TreeNode rightDaughter at this point, if we needed it for something)
				daughterRightCoords = daughterCoords;
				lastRightDaughterBranch = daughterRightCoords.get(daughterRightCoords.size()-1);
				
				/* Finally update the parent node coords list, and recursion depth */
				returnLineCoordinates.addAll(daughterRightCoords);
				tipNumber = lastRightDaughterBranch[5];
			}

			// use the positions from the two internal nodes to place this node's Y-position:
			int nodeYspan = lastRightDaughterBranch[1] - lastLeftDaughterBranch[1]; 
			int midpointIncrement = Math.round(((float)nodeYspan) / 2.0f);
			int nodeYpos = lastLeftDaughterBranch[1] + midpointIncrement;
			
			// draw a vertical line to represent this node, connecting all daughters
			Integer[] thisNode = new Integer[6];		
			thisNode[0] = (depth+1) * 20;				// X0
			thisNode[1] = lastLeftDaughterBranch[1];	// Y0 (same as first/left-hand-most daughter)
			thisNode[2] = thisNode[0];					// X1 (X1 == X0 as this is a vertical line)
			thisNode[3] = lastRightDaughterBranch[1];	// Y1 (same as last/right-hand-most daughter)
			thisNode[4] = depth;
			thisNode[5] = tipNumber;
			returnLineCoordinates.add(thisNode);	
			
			// draw a branch to this node
			Integer[] thisBranch = new Integer[6];
			thisBranch[0] = depth * 20;					// X0 at current tree (recursion) depth
			thisBranch[1] = nodeYpos;					// Y0 at current ypos
			thisBranch[2] = thisBranch[0] + 20;			// X1 at current depth+20 
			thisBranch[3] = nodeYpos;					// Y1 (Y1 == Y0 as this is a horizontal line)
			thisBranch[4] = depth;
			thisBranch[5] = tipNumber;
			returnLineCoordinates.add(thisBranch);	
		}
		
		// return finished list of co-ordinates
		return returnLineCoordinates;
		
	}

	/**
	 * Method to return the node corresponding to a particular node. NO GUARANTEE an ID is unique, or exists.
	 * <p>Use with caution!!!!
	 * @param nodeIDofMRCA
	 * @return
	 */
	public TreeNode getNodeByNumberingID(int nodeID) {
		// Traverse the daughters but first check if this is the right node (to save on traversal)
		if(nodeNumber == nodeID){
			return this;
		}else{
			if(isTerminal){
				return null;
			}else{
				TreeNode returnLowerNode = null;
				for(TreeNode daughter:daughters){
					TreeNode daughterSearchReturnsNode = daughter.getNodeByNumberingID(nodeID);
					if(daughterSearchReturnsNode != null){return daughterSearchReturnsNode;}
				}
				return returnLowerNode;
			}
		}
	}
}
