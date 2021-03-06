package uk.ac.qmul.sbcs.evolution.convergence.handlers.documents.parsers.codeml;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.qmul.sbcs.evolution.convergence.TreeNode;
import uk.ac.qmul.sbcs.evolution.convergence.util.BasicFileReader;


/**
 * Class to parse the convergentSites.out output from the modified codeml-ancestral package by	Jason De Koning at evolutionarygenomics.com / U Colorado, and used in Castoe <i>et al.</i> (2009)'s paper on convergence in agamid lizards.
 * <p>Constructor should be called with a single {@link java.io.File} argument, this being the location of the convergentSites.out file. The constructor will then build a hash of node comparison IDs and site convergent/divergent probability data. This can then be polled by the getData() method.</p>
 * <p>Note that general outputs (tree string, taxon list) can be pulled from the codeml-anc.out file using {@link uk.ac.qmul.sbcs.evolution.convergence.handlers.documents.parsers.codeml.CodemlAncestralGeneralOutputParser}</p>
 * @author <a href="mailto:joe@kitson-consulting.co.uk">Joe Parker, Kitson Consulting / Queen Mary University of London</a>
 * @see <a href="http://www.evolutionarygenomics.com/codeml_ancestral/ancestral.html">evolutionarygenomics.com</a>
 * @see <a href="http://www.evolutionarygenomics.com/codeml_ancestral/codeMLancestral/README.txt">codeml-ancestral README</a>
 * @see {@link uk.ac.qmul.sbcs.evolution.convergence.handlers.documents.parsers.codeml}
 * @see {@link uk.ac.qmul.sbcs.evolution.convergence.handlers.documents.parsers.codeml.CodemlAncestralGeneralOutputParser}
*/
public class CodemlAncestralSiteOutputParser {
	// A class to parse the sites info from codeml-ancestral
	// By 'sites info' we mean the '' file from the codeml-ancestral analysis

	public final File inputFile;		// the convergentSites.out file from codeml-ancestral
	public HashMap<BranchPairComparison,ArrayList<Float[]>> data; // a hashmap of the parsed data, key as pairs of nodes (branch comparisons) in the form 'A_B' where A and B are branch IDs in the 'from..to' notation.
	public TreeNode phylogeny; 			// a phylogeny which this data has been drawn from

	/**
	 * Default no-arg constructor. Deprecated. Use CodemlAncestralSiteOutputParser(File input) instead.
	 */
	@Deprecated
	public CodemlAncestralSiteOutputParser(){
		this.inputFile = null;
		this.data = null;
		this.phylogeny = null;
	}

	public CodemlAncestralSiteOutputParser(File input){
		this.inputFile = input;
		this.phylogeny = null;
		// load input file data as ArrayList<String>
		ArrayList<String> inputData = new BasicFileReader().loadSequences(inputFile, false, true);
		// add the data on divergent and convergent site probabilities by sitewise branch-pair comparison
		// initialise HashMap
		data = new HashMap<BranchPairComparison,ArrayList<Float[]>>();
		// iterate through loaded data
		for(String line:inputData){
			String[] tokens = line.split("\t");
			if(tokens.length == 8){
				// The internal class BranchPairComparison (containing a Set of Branches) is used to hold the branch ID information.
				// FIXME TODO IMPORTANT note that at present brnach IDs 'branch_foo_branch_bar' and 'branch_bar_branch_foo' are *NOT* equivalent keys and will be treated separately!!!! --> SHOULD BE FIXED now as bidirectional Set.contains() used to hold IDs
				// TODO check codeml_ancestral output files to see whether this ever occurs, or only triagonal comparisons are made (e.g A_B, A_C, A_D; B_C, B_D; C_D but not e.g. also D_A, D_B, D_C; C_A, C_B; B_A and diagonals A_A B_B C_C D_D etc)
				String branch_A = tokens[2]; 	// the first branch, will contain pattern of the form "branch pair 22..10" or similar
				String branch_B = tokens[3];	// the second branch, will contain pattern of the form "39..11:" or similar
				// Now get the integers from these strings. Using http://stackoverflow.com/questions/4030928/extract-digits-from-a-string-in-java for simplicity
				// NB this is *not* efficient, really..
				// First split the branch strings into 'from' and 'to' portions using the '..' notation in PAML output:
				String[] branch_A_tokens = tokens[2].split("\\.\\.");
				String[] branch_B_tokens = tokens[3].split("\\.\\.");
				// Now strip all non-digit chars fro those strings:
				String branch_A_cleaned_from 	= branch_A_tokens[0].replaceAll("\\D+","");
				String branch_A_cleaned_to 		= branch_A_tokens[1].replaceAll("\\D+","");
				String branch_B_cleaned_from 	= branch_B_tokens[0].replaceAll("\\D+","");
				String branch_B_cleaned_to 		= branch_B_tokens[1].replaceAll("\\D+","");
				// Now the cleaned strings can be parsed as integers
				int branch_A_from 	= Integer.parseInt(branch_A_cleaned_from);
				int branch_A_to 	= Integer.parseInt(branch_A_cleaned_to);
				int branch_B_from 	= Integer.parseInt(branch_B_cleaned_from);
				int branch_B_to 	= Integer.parseInt(branch_B_cleaned_to);
				// Finally the Branches / BranchPairComparisons can be initialised for these
				// TODO consider overriding compare() in BranchPairComparison...
				BranchPairComparison theBranches = new BranchPairComparison(branch_A_from, branch_A_to, branch_B_from, branch_B_to);
				// FIXED amend branch info parsing to remove unwanted whitespace etc, e.h. from  'branch pair 22..10' or '39..11:' to '22..10' or '39..11'
				// FIXED ultimately data structure should be e.g. HashMap<Set<Int>[],ArrayList<Float[]>> where key is an Set<Int>[], each elem of [] is a Set<Integer> e.g. {{22,10},{39,11}} 
				// String composite_unique_branch_key = branch_A + "_" + branch_B;
				// parse the probabilities, should be in tokens[4..7]
				Float prob_divergent 			= Float.parseFloat(tokens[4]);		// probability of divergent changes between branches A and B
				Float prob_convergent_all 		= Float.parseFloat(tokens[5]);		// probability of all types of convergent changes between branches A and B
				Float prob_convergent_parallel 	= Float.parseFloat(tokens[6]);		// probability of parallel convergent changes between branches A and B
				Float prob_convergent_strict 	= Float.parseFloat(tokens[7]);		// probability of strict convergent changes between branches A and B
				// initialise Float[] to be added to data
				Float[] newProbabilities = new Float[4];
				newProbabilities[0] = prob_divergent;
				newProbabilities[1] = prob_convergent_all;
				newProbabilities[2] = prob_convergent_parallel;
				newProbabilities[3] = prob_convergent_strict;
				if(data.containsKey(theBranches)){
					// this branch pair has already been seen in the data, simply add to its branch info line (the probabilities for divergence, all convergence, parallel, strict convergence respectively)
					ArrayList<Float[]> existingProbabilities = data.remove(theBranches);
					existingProbabilities.add(newProbabilities);
					data.put(theBranches, existingProbabilities);
				}else{
					// this branch pair has not already been seen in the data, create a new ArrayList<String> with this line's info (the probabilities for divergence, all convergence, parallel, strict convergence respectively)
					ArrayList<Float[]> existingProbabilities = new ArrayList<Float[]>();
					existingProbabilities.add(newProbabilities);
					data.put(theBranches, existingProbabilities);
				}
			}
		}
	}
	
	/**
	 * @return the phylogeny
	 */
	public TreeNode getPhylogeny() {
		return phylogeny;
	}

	/**
	 * Set the phylogeny (also calls setNodeNumbers as we'll need these to extract the node IDs for branch-pair comparison etc)
	 * @param phylogeny the phylogeny to set
	 */
	public void setPhylogeny(TreeNode phylogeny) {
		this.phylogeny = phylogeny;
		// set internal node numbers, we'll need these...
		this.phylogeny.setNodeNumbers(0, this.phylogeny.howManyTips());
	}

	/**
	 * Set the pylogeny AND specified a HashMap<String,Integer> of terminal tip name - ID mappings. (also calls setNodeNumbers as we'll need these to extract the node IDs for branch-pair comparison etc)
	 * @param phylogeny the phylogeny to set
	 * 
	 */
	public void setPhylogenyWithSpecifiedTipLabelNumberMapping(TreeNode phylogeny, HashMap<String,Integer> specifiedTipNameNumberMapping) {
		this.phylogeny = phylogeny;
		// set the specified mapping for tip names - numbers
		phylogeny.setTipNameNumberMapping(specifiedTipNameNumberMapping);
		// set internal node numbers, we'll need these...
		this.phylogeny.setNodeNumbers(0, this.phylogeny.howManyTips());
	}

	public float[][] getAllBranchPairProbabilitiesSitewiseSummed(){
		// return all branch-pair divergent, convergent, parallel, strict-convergent probabilities, as a list (square matrix). sitewise values (different sites' on a branch-pair comparison) will be summed.
		float[][] returnMatrix = new float[data.size()][4];
		int rowIndex = 0;
		Iterator<BranchPairComparison> itr = data.keySet().iterator();
		while(itr.hasNext()){
			// loop through branch pairs
			BranchPairComparison branch_pair = itr.next();
			float[] rowData = new float[4];
			// get branch pair sitewise info
			ArrayList<Float[]> branch_pair_probabilities = data.get(branch_pair);
			for(Float[] sitewise_branch_pair_probabilities:branch_pair_probabilities){
				// sum over all substitutions at this branch pair
				for(int i=0;i<4;i++){
					rowData[i] = rowData[i] + sitewise_branch_pair_probabilities[i];
				}
			}
			// add to retMat
			returnMatrix[rowIndex] = rowData;
			rowIndex++;
		}
		return returnMatrix;
	}
	
	/**
	 * Return all the sitewise (not-summed) divergent, convergent, parallel, strict-convergent probabilities for any branch-pair comparisons including a particular node number.
	 * <p>This is presented as an [n][4] matrix.
	 * <p>This method uses the {@link BranchPairComparison#contains()} method to search for nodes in the data.
	 * @param nodeNumber - an integer UID describing a node (tip or internal) in the data
	 * @return float[n][4] containing the divergent, convergent, parallel, strict-convergent probabilities for any branch-pair comparisons including the specified node number.
	 */
	public float[][] getSitewiseBranchPairProbabilitiesIncludingSpecificNodeNumber(int nodeNumber){
		// return all the sitewise (not-summed) divergent, convergent, parallel, strict-convergent probabilities for any branch-pair comparisons including a particular node number
		// FIXME implement this
		ArrayList<Float[]> returnArray = new ArrayList<Float[]>();
		Iterator<BranchPairComparison> dataItr = data.keySet().iterator();
		while(dataItr.hasNext()){
			BranchPairComparison candidate = dataItr.next();
			if(candidate.contains(nodeNumber)){
				returnArray.addAll(data.get(candidate));
			}
		}
		float[][] returnMatrix = new float[returnArray.size()][4];
		int row_index = 0;
		for(Float[] row:returnArray){
			for(int i=0;i<4;i++){
				returnMatrix[row_index][i] = row[i];
			}
			row_index++;
		}
		return returnMatrix;
	}
	
	/**
	 * Return all the sitewise (summed) divergent, convergent, parallel, strict-convergent probabilities for any branch-pair comparisons including a particular node number.
	 * <p>This is achieved by taking column sums from an [n][4] matrix returned by the {@link CodemlAncestralSiteOutputParser#getSitewiseBranchPairProbabilitiesIncludingSpecificNodeNumber(int nodeNumber)} method.
	 * <p><i>That</i> method uses the {@link BranchPairComparison#contains()} method to search for nodes.
	 * @param nodeNumber - an integer UID describing a node (tip or internal) in the data
	 * @return float[4] containing the summed divergent, convergent, parallel, strict-convergent probabilities for any branch-pair comparisons including the specified node number.
	 */
	public float[] getSummedBranchPairProbabilitiesIncludingSpecificNodeNumber(int nodeNumber){
		// return all the sitewise (summed) divergent, convergent, parallel, strict-convergent probabilities for any branch-pair comparisons including a particular node number
		float[][] returnMatrix = this.getSitewiseBranchPairProbabilitiesIncludingSpecificNodeNumber(nodeNumber);
		float[] retArr = new float[4];
		for(int i=0;i<returnMatrix.length;i++){
			for(int j=0;j<4;j++){
				retArr[j] += returnMatrix[i][j];
			}
		}
		return retArr;
	}
	
	public float[] getProbabilitiesForNodeComparisons(BranchPairComparison comparison) throws NullBranchPairComparisonPointerException{
		float[] returnProbabilities = new float[4];
		if(!data.containsKey(comparison)){
			throw new NullBranchPairComparisonPointerException("Branch pair "+comparison.toString()+" not present in data!");
		}else{
			// return all the sitewise (summed) divergent, convergent, parallel, strict-convergent probabilities for the branch-pair comparison between branches A1->A2 and B1->B2
			// currently no good way to do this as data containing Strings of composite_unique_branch_key are not a logical data structure. buggy as fuck, not even going to implement this
			// TODO not implemented
			// FIXME implement this
			// get branch pair sitewise info
			/*
			TODO the get() method does not work on BranchPairComparison at the moment, so nothing returned
			FIXME work out the best way to either subclass HashMap for overidden get(), or implement relevant (?/ equals() ?) for BPC class..
			FIXME see also http://stackoverflow.com/questions/4794953/for-a-hashmap-that-maps-from-a-custom-class-how-to-make-it-so-that-two-equivale
			 * 
			 */
			ArrayList<Float[]> branch_pair_probabilities = data.get(comparison);
			for(Float[] sitewise_branch_pair_probabilities:branch_pair_probabilities){
				// sum over all substitutions at this branch pair
				for(int i=0;i<4;i++){
					returnProbabilities[i] = returnProbabilities[i] + sitewise_branch_pair_probabilities[i];
				}
			}
		}
		return returnProbabilities;
	}

	public float[] getProbabilitiesForNodeComparisons(int nodeFrom_A, int nodeTo_A, int nodeFrom_B, int nodeTo_B){
		BranchPairComparison c = new BranchPairComparison(nodeFrom_A,nodeTo_A,nodeFrom_B,nodeTo_B);
		// return all the sitewise (summed) divergent, convergent, parallel, strict-convergent probabilities for the branch-pair comparison between branches A1->A2 and B1->B2
		// currently no good way to do this as data containing Strings of composite_unique_branch_key are not a logical data structure. buggy as fuck, not even going to implement this
		// TODO not implemented
		// FIXME implement this
		return this.getProbabilitiesForNodeComparisons(c);
	}

	public float[] getProbabilitiesForAncestralBranchComparisonsDefinedByTaxonSetMRCAs(String[] TaxonSetTo_A, String[] TaxonSetTo_B) throws ReferencePhylogenyNotSetException{
		if(this.phylogeny == null){throw new ReferencePhylogenyNotSetException();}
		// return all the sitewise (summed) divergent, convergent, parallel, strict-convergent probabilities for the branch-pair comparison between branches A1->A2 and B1->B2, defined by taxon sets
		// e.g. A1 = MRCA of TaxonSetFrom_A, A2 = MRCA of TaxonSetFrom_A etc. 
		// assumes TreeNode phylogeny present/set
		// needs to be robust to terminal branches, too
		// currently no good way to do this as data containing Strings of composite_unique_branch_key are not a logical data structure. buggy as fuck, not even going to implement this
		// TODO not implemented
		// FIXME implement this
		// populate taxon set for MRCA(A)
		HashSet<String> taxaContained_A = new HashSet<String>(Arrays.asList(TaxonSetTo_A));
		// populate taxon set for MRCA(B)
		HashSet<String> taxaContained_B = new HashSet<String>(Arrays.asList(TaxonSetTo_B));
		// get the branch IDs for branches leading to MRCA(A) and MRCA(B)
		int[] branch_A_IDs = phylogeny.getBranchNumberingIDContainingTaxa(taxaContained_A);
		int[] branch_B_IDs = phylogeny.getBranchNumberingIDContainingTaxa(taxaContained_B);
		BranchPairComparison bpc = new BranchPairComparison(branch_A_IDs[0],branch_A_IDs[1],branch_B_IDs[0],branch_B_IDs[1]);
		return this.getProbabilitiesForNodeComparisons(bpc);
	}

	public float[] getProbabilitiesForNodeComparisonsDefinedByMRCASubtreeOf(String[] TaxonSet) throws ReferencePhylogenyNotSetException{
		if(this.phylogeny == null){throw new ReferencePhylogenyNotSetException();}
		// return all the sitewise (summed) divergent, convergent, parallel, strict-convergent probabilities for all branch-pair comparisons present in MRCA subtree defined by taxon sets
		// e.g. A1 = MRCA of TaxonSetFrom_A, A2 = MRCA of TaxonSetFrom_A etc. 
		// assumes TreeNode phylogeny present/set
		// needs to be robust to terminal branches, too
		// currently no good way to do this as data containing Strings of composite_unique_branch_key are not a logical data structure. buggy as fuck, not even going to implement this
		// TODO not implemented
		// FIXME implement this
		return null;
	}

	public float[] getProbabilitiesForNodeComparisonsOnBranchesBetweenNodes(int node_from, int node_to) throws ReferencePhylogenyNotSetException{
		if(this.phylogeny == null){throw new ReferencePhylogenyNotSetException();}
		// return all the sitewise (summed) divergent, convergent, parallel, strict-convergent probabilities for the branch-pair comparison between branches connecting the two nodes node_from and node_to.
		// Can be tips, internals, or a combination
		// assumes TreeNode phylogeny present/set
		// TODO not implemented
		// FIXME implement this
		return null;
	}
	
	public float[] getProbabilitiesForNodeComparisonsOnBranchesBetweenTips(String taxon_from, String taxon_to) throws ReferencePhylogenyNotSetException{
		if(this.phylogeny == null){throw new ReferencePhylogenyNotSetException();}
		// return all the sitewise (summed) divergent, convergent, parallel, strict-convergent probabilities for the branch-pair comparison between branches connecting the two terminal taxa taxon_from and taxon_to. 
		// assumes TreeNode phylogeny present/set
		// TODO not implemented
		// FIXME implement this
		return null;
	}
	
	public float[] getProbabilitiesBySiteIndex(int siteIndex){
		// return all the divergent, convergent, parallel, strict-convergent probabilities, summed over all branch-pair comparison, for a given site index
		// currently no good way to do this as data containing site indices or pattern indices discarded at the moment. buggy as fuck, not even going to implement this
		// TODO not implemented
		// FIXME implement this
		return null;
	}
	
	public float[][] getProbabilitiesAllSites(){
		// return all the divergent, convergent, parallel, strict-convergent probabilities, summed over all branch-pair comparison, for every site in the alignment
		// currently no good way to do this as data containing site indices or pattern indices discarded at the moment. buggy as fuck, not even going to implement this
		// TODO not implemented
		// FIXME implement this
		return null;
	}
	
	public boolean containsNode(int i) {
		boolean hasNode = false;
		Iterator<BranchPairComparison> itr = data.keySet().iterator();
		while(itr.hasNext()){
			BranchPairComparison p = itr.next();
			hasNode = (hasNode || p.contains(i));
		}
		return hasNode;
	}

	/**
	 * Tests whether this branch is present in the data. 
	 * <p><b>IMPORTANT</b>: this method marked private to try and ensure that Branch is properly constructed (e.g. sortIntPair() has been called to sort branch nodes into numeric order.)
	 * @param b
	 * @return
	 */
	private boolean containsBranch(Branch b){
		boolean hasBranch = false;
		Iterator<BranchPairComparison> itr = data.keySet().iterator();
		while(itr.hasNext()){
			BranchPairComparison p = itr.next();
			hasBranch = (hasBranch || p.containsBranch(b));
		}
		return hasBranch;
	}
	
	/**
	 * Tests whether this branch is present in the data. 
	 * <p><b>IMPORTANT</b>: this method marked public but containsBranch(Branch b) is private to try and ensure that Branch is properly constructed (e.g. sortIntPair() has been called to sort branch nodes into numeric order.)
	 * @param b
	 * @return
	 */
	public boolean containsBranch(int node_from, int node_to){
		int[] sortedInts = this.sortIntPair(node_from, node_to);
		Branch b = new Branch(sortedInts[0], sortedInts[1]);
		return this.containsBranch(b);
	}

	/**
	 * Sorts two ints into numerical order
	 * @param int_a
	 * @param int_b
	 * @return int[] where int_a and int_b are sorted
	 */
	public static int[] sortIntPair(int int_a, int int_b){
		int[] retArray = new int[2];
		if(int_a < int_b){
			// a is smaller than b, supplied order is numerical order
			retArray[0] = int_a;
			retArray[1] = int_b;
		}else{
			// a is bigger than b, reverse supplied order for numerical order
			retArray[0] = int_b;
			retArray[1] = int_a;
			
		}
		return retArray;
	}
	
	/**
	 * Custom exception extends NullPointerException - for use when an operation is attempted using a branch pair comparison that is not present in the data.
	 * @author <a href="mailto:joe@kitson-consulting.co.uk">Joe Parker, Kitson Consulting / Queen Mary University of London</a>
	 *
	 */
	public class NullBranchPairComparisonPointerException extends NullPointerException{

		public NullBranchPairComparisonPointerException(String exceptionString) {
			// TODO Auto-generated constructor stub
			System.out.println(exceptionString);
			System.err.println(exceptionString);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -2902265783356134872L;
		
	}
	
	public class ReferencePhylogenyNotSetException extends Exception{

		/**
		 * Custom exception when there is no reference phylogeny present yet
		 */
		private static final long serialVersionUID = -4901978545143436342L;
	}
	
	/**
	 * Internal class to determine whether an ArrayList<Branch> contains a continuous path/lineage from tip A <=> tip B.
	 * <p><b>NOTE</b> that internal lineage representation implemented in {@link HashSet}; thus, duplicate branches <i>can exist</i>
	 * <p><b>NOTE</b> that there is no mechanism at the moment to define nodes as tips only. This means internal node UIDs <i>could</i> be used. This would be a bad idea, as chains could extend towards terminal tips without ever joining!
	 * @author <a href="mailto:joe@kitson-consulting.co.uk">Joe Parker, Kitson Consulting / Queen Mary University of London</a>
	 *
	 */
	public class BranchLineage{
		final int tip_UID_A;	// the tip the lineage runs from	
		final int tip_UID_B;	// the tip the lineage ultimately runs to
		private int extent_chain_A;	// the chain of branches from A
		private int extent_chain_B;	// the chain of branches from B
		private boolean adjoins = false;	// whether A and B chains join 
		private HashSet<Branch> branchPool;	// branches added to the lineage that don't add 
		private HashSet<Branch> chain_A;
		private HashSet<Branch> chain_B;
		private HashSet<Branch> completeLineage;
		
		/**
		 * Constructor for BranchLineage - args are tip UIDs for destination points, and the {@link Branch}es that contain them.
		 * <p><b>NOTE</b> that internal lineage representation implemented in {@link HashSet}; thus, duplicate branches <i>can exist</i>
		 * 
		 * @param A - UID for node number at one tip of the lineage
		 * @param B - UID for node number at the other tip of the lineage
		 */
		public BranchLineage(int A, int B){
			tip_UID_A = A;
			tip_UID_B = B;
			extent_chain_A = tip_UID_A;
			extent_chain_B = tip_UID_B;
			branchPool = new HashSet<Branch>();
			chain_A = new HashSet<Branch>();
			chain_B = new HashSet<Branch>(); 
		}
		
		/**
		 * Returns the {@link HashSet} of all {@link Branch}es that connect this lineage, if it is complete. Tests whether adjoins is already true, or attempts to complete from branch pool if it isn't before giving up and throwing a NullPointerException if the lineage can't be completed from the branch pool.
		 * @return HashSet<Branch> of the branches comprising this lineage.
		 * @throws NullPointerException if the lineage can't be completed from the two chains (A and B) and the current branch pool.
		 */
		public HashSet<Branch> getCompleteLineage() throws NullPointerException{
			if(adjoins){
				return completeLineage;
			}else{
				if(this.doesLineageAdjoin()){
					return completeLineage;
				}else{
					throw new NullPointerException("Lineage not set / does not adjoin");
				}
			}
		}
		
		/**
		 * Adds a branch to the internal branches ArrayList
		 * @param newBranch - branch to be added to lineage.
		 */
		public boolean addBranch(Branch newBranch){
			// before adding this branch to the branch pool, see if by chance we can extend either chain
			boolean A_contains = false;
			boolean B_contains = false;
			if(newBranch.contains(extent_chain_A)){
				chain_A.add(newBranch);
				extent_chain_A = newBranch.otherNode(extent_chain_A);
				A_contains = true;
			}
			if(newBranch.contains(extent_chain_B)){
				if(A_contains){
					// the branch contains A and B chain tips
					// therefore the lineage is complete (Branch b will already have been added to chain_A, and extent_A == extent_B); no need to add this branch
					adjoins = true;
					completeLineage = new HashSet<Branch>();
					completeLineage.addAll(chain_A);
					completeLineage.addAll(chain_B);
				}else{
					chain_B.add(newBranch);
					extent_chain_B = newBranch.otherNode(extent_chain_B);
					B_contains = true;
				}
			}
			// call doesLineageAdjoin() in case the new branch has allowed previous branches to be inserted;
			this.doesLineageAdjoin();
			// finish up by returning a boolean depending on whether the branch has been successfully added or not
			if(A_contains || B_contains){
				// Branch b happens to contain A or B chain tip, those chains have been extended, no need to add to branch pool
				return true;
			}else{
				// Neither 
				return branchPool.add(newBranch);
			}
		}
		
		/**
		 * Evaluates whether the ArrayList<Branch> branches describes a continuous lineage from A to B; returns true if so; all other values false.
		 * <p>Pseudocode:
		 * <pre>
		 * extent_A = tip_A
		 * extent_B = tip_B
		 * chain_A
		 * chain_B
		 * exhausted = false #this will set to true if we ever iterate through all available branches without a hit
		 * while(!exhausted){
		 * 	extended_this_round = false
		 * 	for(branch:branches){
		 * 		if(branch.contains(extent_A){
		 * 			extent_A = branch.tip.not_extent_A
		 * 			chain_A.add(branch)
		 * 			extended_this_round = true
		 * 		}
		 * 		if(branch.contains(extent_B){... as for A...}
		 * 		if(branch.contains(extent_A && extent_B){
		 * 			#this is the final branch, lineage is complete
		 * 			adjoins = true; extended_this_round = true
		 * 		}
		 * 	}
		 * 	exhausted = !extended_this_round
		 * }
		 * </pre>
		 * @return true if branches describe a path from A to B; otherwise false
		 */
		public boolean doesLineageAdjoin(){
			if(adjoins){
				// lineage has already been evaluated and adjoins flag evaluated true; return it
				return adjoins;
			}else{
				// we need to evaluate whether lineages adjoin
				boolean exhausted = false;
				while(!exhausted){
					// flag to keep track of whether we've managed to add a branch to from the pool to the chains
					boolean extended_this_round = false;
					// loop through the branches, trying to extend the chain if we can...
					Iterator<Branch> branchPoolItr = branchPool.iterator();
					while(branchPoolItr.hasNext()){
						Branch someBranch = branchPoolItr.next();
						boolean A_contains = false;
						boolean B_contains = false;
						// see if the branch can extend chain A
						if(someBranch.contains(extent_chain_A)){
							// add to chain A
							chain_A.add(someBranch);
							// remove from pool
							branchPoolItr.remove();
							extent_chain_A = someBranch.otherNode(extent_chain_A);
							A_contains = true;
						}
						// see if the branch can extend chain B
						if(someBranch.contains(extent_chain_B)){
							// first check termination condition
							if(A_contains){
								// the branch contains A and B chain tips
								// therefore the lineage is complete (Branch b will already have been added to chain_A, and extent_A == extent_B); no need to add this branch
								adjoins = true;
								completeLineage = new HashSet<Branch>();
								completeLineage.addAll(chain_A);
								completeLineage.addAll(chain_B);
							}else{
								// add to chain B
								chain_B.add(someBranch);
								// remove from pool
								branchPoolItr.remove();
								extent_chain_B = someBranch.otherNode(extent_chain_B);
								B_contains = true;
							}
						}
						// have we managed to exend either chain in this round?
						extended_this_round = (A_contains || B_contains || extended_this_round);
					}
					// if we've not managed to add a branch at all, then we have exhausted all possibilities - can't form a lineage between these chains with this branch pool!
					exhausted = !extended_this_round;
				}
				return adjoins;
			}
		}
	}
	
	/**
	 * Internal class to represent pairs of branches (themselves pairs of nodes) which are compared in a branch-pair comparison.
	 * <p>Note that the BranchPairComparison and the contains() method <b>is not</b> an implementation of {@link java.util.Set#contains()}, or subclass of e.g. {@link java.util.HashSet#contains()}.
	 * @author <a href="mailto:joe@kitson-consulting.co.uk">Joe Parker, Kitson Consulting / Queen Mary University of London</a>
	 * @see {@link Branch}
	 */
	public class BranchPairComparison{
		final Branch branch_A;
		final Branch branch_B;
		
		/**
		 * No-arg constructor. Deprecated.
		 * @deprecated
		 * TODO consider overriding compare() in BranchPairComparison...
		 */
		@Deprecated
		private BranchPairComparison(){
			branch_A = null;
			branch_B = null;
		}

		public BranchPairComparison(int node_from_branch_A, int node_to_branch_A, int node_from_branch_B, int node_to_branch_B){
			/*
			 * TODO Order the nodes within branches, and the branches within BCP,
			 * so that they are in numeric order, and hashCode() and equals(Object)
			 * can be implemented with predictable behaviour.
			 */
			// first sort branches' pairs
			int[] sorted_branch_A_pair = sortIntPair(node_from_branch_A, node_to_branch_A);
			int[] sorted_branch_B_pair = sortIntPair(node_from_branch_B, node_to_branch_B);
			// now add branches depending on whether A[0] > B[0] or not
			if(sorted_branch_A_pair[0] < sorted_branch_B_pair[0]){
				// start A lower than start B so add pair as {A,B}
				branch_A = new Branch(sorted_branch_A_pair[0], sorted_branch_A_pair[1]);
				branch_B = new Branch(sorted_branch_B_pair[0], sorted_branch_B_pair[1]);
			}else{
				// see if B[0] > A[0]
				if(sorted_branch_A_pair[0] > sorted_branch_B_pair[0]){
					// start A lower than start B so add pair as {B,A}
					branch_A = new Branch(sorted_branch_B_pair[0], sorted_branch_B_pair[1]);
					branch_B = new Branch(sorted_branch_A_pair[0], sorted_branch_A_pair[1]);
				}else{
					// A[0] == B[0], e.g. same start; so pick depending on (A[1] > B[1])
					if(sorted_branch_A_pair[1] < sorted_branch_B_pair[1]){
						// end A lower than end B so add pair as {A,B}
						branch_A = new Branch(sorted_branch_A_pair[0], sorted_branch_A_pair[1]);
						branch_B = new Branch(sorted_branch_B_pair[0], sorted_branch_B_pair[1]);
					}else{
						// end A higher than end B so add pair as {B,A}
						branch_A = new Branch(sorted_branch_B_pair[0], sorted_branch_B_pair[1]);
						branch_B = new Branch(sorted_branch_A_pair[0], sorted_branch_A_pair[1]);
					}
				}
			}
		}
		
		@Override
		/**
		 * @Override
		 * Overides equals() method to allow proper comparison operations for e.g. Set interface
		 * <p><b>IMPORTANT</b>: Assumes branch nodes have been sorted at constructor, so that node_from � node_to
		 */
		public boolean equals(Object aBranchPairComparison){
			BranchPairComparison compare = (BranchPairComparison) aBranchPairComparison;
			// return true if all nodes are equal - the branches' nodes should have been sorted in constructor
			return (
					(this.branch_A.node_begin == compare.branch_A.node_begin) &&
					(this.branch_A.node_end == compare.branch_A.node_end) &&
					(this.branch_B.node_begin == compare.branch_B.node_begin) &&
					(this.branch_B.node_end == compare.branch_B.node_end) 
			);
		}
		
		@Override
		/**
		 * @Override
		 * Overides hashCode() method to allow proper comparison operations for e.g. Set interface
		 * <p><b>IMPORTANT</b>: Assumes branch nodes have been sorted at constructor, so that node_from � node_to
		 */
		public int hashCode(){
			return (this.branch_A.hashCode()+"_"+this.branch_B.hashCode()).hashCode();
		}

		/**
		 * Returns true if this either of the branches under comparison joins node I
		 * @param I - node ID 
		 * @return {@link Boolean} - true if node is present
		 * @see {@link java.util.Set#contains()}
		 */
		public boolean contains(Integer I){
			return (branch_A.contains(I) || branch_B.contains(I));
		}

		/**
		 * Returns true if this either of the branches under comparison is branch specified by node values A and B
		 * @param int branch_from - first node 
		 * @param int branch_to   - second node 
		 * @return {@link Boolean} - true if branch is present
		 * @see {@link Branch}
		 */
		public boolean containsBranch(int branch_from, int branch_to){
			Branch b = new Branch(branch_from, branch_to);
			return (branch_A.equals(b) || branch_B.equals(b));
		}

		/**
		 * Returns true if this either of the branches under comparison is branch B
		 * @param b - Branch 
		 * @return {@link Boolean} - true if branch is present
		 * @see {@link Branch}
		 */
		public boolean containsBranch(Branch b){
			return (branch_A.equals(b) || branch_B.equals(b));
		}

		/**
		 * Overidden toString() method. 
		 * @return String of form "from_A..to_A|from_B..to_B"
		 */
		@Override
		public String toString(){
			return branch_A.toString() + "|" + branch_B.toString();
		}
	}
	
	/**
	 * Internal class to represent branch (pairs of nodes) which are compared in a branch-pair comparison.
	 * <p>Note that the contains() method <b>is not</b> an implementation of {@link java.util.Set#contains()}, or subclass of e.g. {@link java.util.HashSet#contains()}.
	 * @author <a href="mailto:joe@kitson-consulting.co.uk">Joe Parker, Kitson Consulting / Queen Mary University of London</a>
	 * @see {@link BranchPairComparison}
	 */
	public class Branch{
		final int node_begin;
		final int node_end;
		final TreeSet<Integer> branch;
		
		/**
		 * No-arg constructor. Deprecated.
		 * @deprecated
		 */
		@Deprecated
		private Branch(){
			node_begin = Integer.MIN_VALUE;
			node_end = Integer.MIN_VALUE;
			branch = null;
		}
		
		public Branch(int node_from, int node_to){
			/*
			 * TODO Order the nodes within branches, and the branches within BCP,
			 * so that they are in numeric order, and hashCode() and equals(Object)
			 * can be implemented with predictable behaviour.
			 */
			int[] sortedNodes = sortIntPair(node_from, node_to);
			node_begin = sortedNodes[0];
			node_end = sortedNodes[1];
			branch = new TreeSet<Integer>();
			branch.add(node_begin);
			branch.add(node_end);
		}
		
		/**
		 * Returns true if this branch joins node I
		 * @param I - node ID 
		 * @return {@link Boolean} - true if node is present
		 * @see {@link java.util.Set#contains()}
		 */
		public boolean contains(Integer I){
			return branch.contains(I);
		}
		
		/**
		 * Given a node UID, checks whether this branch contains this node. If so returns the other node UID. If not, throws NullPointerException
		 * @param some_node - the UID of some node, hopefully present on this branch
		 * @return int - UID of the other node comprising this branch
		 * @throws NullPointerException if some_node isn't in fact present on this branch.
		 */
		public int otherNode(int some_node) throws NullPointerException{
			if(this.contains(some_node)){
				if(some_node == node_begin){return node_end;}else{return node_begin;}
			}else{
				throw new NullPointerException("Node "+some_node+" not found at branch "+this.toString());
			}
		}
		
		/**
		 * Bidirectional equality test. Returns true if the branch joins both node start AND node end.
		 * @param start node at start of branch
		 * @param finish node at end of branch
		 * @return true if branch describes the same pair of nodes
		 */
		public boolean equals(int start, int finish){
			return((node_begin==start && node_end==finish)||(node_begin==finish && node_end==start));
		}
		
		/**
		 * Bidirectional equality test. Returns true if the two branches are equal, e.g. join the same pair of nodes.
		 * @param B branch to test
		 * @return true if branches describe the same pair of nodes
		 */
		@Override
		public boolean equals(Object o){
			if(o instanceof Branch){
				Branch b = (Branch) o;
				return(this.equals(b.node_begin, b.node_end));
			}
			return false;
		}		

		/**
		 * Bidirectional equality test. Returns true if the two branches are equal, e.g. join the same pair of nodes.
		 * @param B branch to test
		 * @return true if branches describe the same pair of nodes
		 */
		public boolean equals(Branch b){
			return(this.equals(b.node_begin, b.node_end));
		}		
		
		/**
		 * Overidden toString() method. 
		 * @return String of form "from..to"
		 */
		@Override
		public String toString(){
			return node_begin + ".." + node_end;
		}
		
		/**
		 * Overidden hashCode() method. 
		 * @return hashCode of String of form "from..to"
		 */
		@Override
		public int hashCode(){
			return this.toString().hashCode();
		}
	}
}
