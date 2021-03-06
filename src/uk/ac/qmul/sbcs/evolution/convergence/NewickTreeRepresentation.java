package uk.ac.qmul.sbcs.evolution.convergence;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import uk.ac.qmul.sbcs.evolution.convergence.util.CapitalisedFileReader;
import uk.ac.qmul.sbcs.evolution.convergence.util.BasicFileWriter;

/**
 * 
 * @author - Joe Parker
 * @since - 02/11/2011
 * @updated - 09/07/2017
 * @version - 0.1.0
 * This class provides <b>limited</b> support for a Newick tree file and associated I/O.
 * It assumes one single tree on one line (the first) of the file.
 * The main purpose is to provide functionality to prune phylogenetic trees through the pruneTaxon() method.
 * <p>Note that the pruneTaxon() method simply uses regular expressions to parse the tree string; it does <b>NOT</b> create (or rely on) a true Newick tree repsesentation as a series of nested nodes, with associated traversal operations etc. 
 * In particular there is no guarantee that non-standard treefile formats will be correctly parsed, including BEAST and MrBayes trees.
 * 
 * <b>Update 09/07/2017:</b>
 * <b>Up-version to 0.1.0</b>
 * Now explicitly trying to detect NEXUS file, if so dying gracefully.
 */
public class NewickTreeRepresentation {
	private String treeString = null;
	private String[] separateTopologies;
	private File treeFile = null;
	private int numberOfTaxa = 0;
	private int numberOfTrees = 0;
	private TreeSet<String> taxaNames = null;
	
	
	@Deprecated
	/**
	 * WARNING this constructor should not be used at present, as it relies on the internal private obtainTaxaNames() method to get the taxaNames list that is needed for a safe pruneTaxon() call.
	 * At the moment this method is not implemented, and so this constructor is marked as deprecated.
	 * @param tree
	 */
	public NewickTreeRepresentation(){}
	
	/**
	 * @since r126 - multiple trees in a single treefile
	 * @param inputFile - can have any number of trees, but error checking limited to 'not null' on line.. 
	 * @param names
	 */
	public NewickTreeRepresentation(File inputFile, TreeSet<String> names){
		this.treeFile = inputFile;
		if(treeFile.exists()&&treeFile.canRead()&&(!treeFile.isDirectory())&&treeFile.isFile()){
			ArrayList<String> trees = new CapitalisedFileReader().loadSequences(treeFile,true);
			CharSequence nexusCS = new String("NEXUS").subSequence(0, "NEXUS".length());
			CharSequence treesCS = new String("TREES").subSequence(0, "TREES".length());
			CharSequence endCS = new String("END").subSequence(0, "END".length());
			try {
				if(trees.get(0).toUpperCase().contains(nexusCS)){
					// assume it is NEXUS
					System.err.println("tree parsing NEXUS.");
					boolean inTreesBlock = false;
					treeString = null;
					for(String tree:trees){
						if((!tree.isEmpty())&&inTreesBlock){
							if(tree.contains(endCS)){
								inTreesBlock=false;
							}else{
								String[] splitOnEquals = tree.split("=",2);
								if(splitOnEquals.length==2){
									// it looks like we've found a line of form:
									// tree FOO = (blah);
									// .. and should have the bit after.
									String cleanedTree = splitOnEquals[1].trim();
									tree=cleanedTree;
									if(treeString == null){
										treeString = tree;
									}else{
										treeString = treeString + "\n"+ tree;
									}
									numberOfTrees++;
									System.out.println(tree);
								}
							}
						}else{
							if(tree.contains(treesCS)){inTreesBlock=true;}
						}
					}
				}else{
					// assume not NEXUS
					// assume it is Phylip/Newick
					// but catch a treefile exception
					treeString = null;
					for(String tree:trees){
						if(!tree.isEmpty()){
							if(treeString == null){
								treeString = tree;
							}else{
								treeString = treeString + "\n"+ tree;
							}
							numberOfTrees++;
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.println("tree parsing failed.");
				e.printStackTrace();
			}
			this.taxaNames = names;
			this.numberOfTaxa = taxaNames.size();
			this.separateTopologies = new String[this.numberOfTrees];
			if(this.numberOfTrees>0){
				this.separateTopologies = this.treeString.replaceAll("\\s+", "").split(";");
			}else{
				this.separateTopologies[0] = this.treeString;
			}
			this.separateTopologies = StringCharAppender.appendEach(separateTopologies, ';');
		}else{
			System.err.println("SERIOUS: cannot read/open treefile "+treeFile.getPath()+" for some reason.");
			System.out.println("SERIOUS: cannot read/open treefile "+treeFile.getPath()+" for some reason.");
		}
	}
	
	@Deprecated
	/**
	 * WARNING this constructor should not be used at present, as it relies on the internal private obtainTaxaNames() method to get the taxaNames list that is needed for a safe pruneTaxon() call.
	 * At the moment this method is not implemented, and so this constructor is marked as deprecated.
	 * @param tree
	 */
	public NewickTreeRepresentation(File inputFile){
		this.treeFile = inputFile;
		if(treeFile.exists()&&treeFile.canRead()&&(!treeFile.isDirectory())&&treeFile.isFile()){
			ArrayList<String> trees = new CapitalisedFileReader().loadSequences(treeFile,true);
			CharSequence nexusCS = new String("NEXUS").subSequence(0, "NEXUS".length());
			CharSequence treesCS = new String("TREES").subSequence(0, "TREES".length());
			CharSequence endCS = new String("END").subSequence(0, "END".length());
			try {
				if(trees.get(0).toUpperCase().contains(nexusCS)){
					// assume it is NEXUS
					System.err.println("tree parsing NEXUS.");
					boolean inTreesBlock = false;
					treeString = null;
					for(String tree:trees){
						if((!tree.isEmpty())&&inTreesBlock){
							if(tree.contains(endCS)){
								inTreesBlock=false;
							}else{
								String[] splitOnEquals = tree.split("=",2);
								if(splitOnEquals.length==2){
									// it looks like we've found a line of form:
									// tree FOO = (blah);
									// .. and should have the bit after.
									String cleanedTree = splitOnEquals[1].trim();
									tree=cleanedTree;
									if(treeString == null){
										treeString = tree;
									}else{
										treeString = treeString + "\n"+ tree;
									}
									numberOfTrees++;
									System.out.println(tree);
								}
							}
						}else{
							if(tree.contains(treesCS)){inTreesBlock=true;}
						}
					}
				}else{
					// assume not NEXUS
					// assume it is Phylip/Newick
					// but catch a treefile exception
					treeString = null;
					for(String tree:trees){
						if(!tree.isEmpty()){
							if(treeString == null){
								treeString = tree;
							}else{
								treeString = treeString + "\n"+ tree;
							}
							numberOfTrees++;
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.println("tree parsing failed.");
				e.printStackTrace();
			}
			this.taxaNames = this.obtainTaxaNames(treeString);
			this.numberOfTaxa = taxaNames.size();
			this.separateTopologies = new String[this.numberOfTrees];
			if(this.numberOfTrees>0){
				this.separateTopologies = this.treeString.replaceAll("\\s+", "").split(";");
			}else{
				this.separateTopologies[0] = this.treeString;
			}
			this.separateTopologies = StringCharAppender.appendEach(separateTopologies, ';');
		}else{
			System.err.println("SERIOUS: cannot read/open treefile "+treeFile.getPath()+" for some reason.");
			System.out.println("SERIOUS: cannot read/open treefile "+treeFile.getPath()+" for some reason.");
		}
	}
	
	public NewickTreeRepresentation(String tree, TreeSet<String> names){
		this.treeString = tree;
		this.taxaNames = names;
		String[] separateTrees = treeString.replaceAll("\\s+", "").split(";");
		for(String someTree:separateTrees){
			if(someTree.length()>2){
				this.numberOfTrees++;
			}
		}
		//this.numberOfTrees = treeString.split(";").length;
		this.numberOfTaxa = taxaNames.size();
		this.separateTopologies = new String[this.numberOfTrees];
		if(this.numberOfTrees>0){
			for(int i=0;i<this.numberOfTrees;i++){
				if(separateTrees[i].length()>2){
					this.separateTopologies[i] = separateTrees[i];
				}
			}
		}else{
			this.separateTopologies[0] = this.treeString;
		}
		this.separateTopologies = StringCharAppender.appendEach(separateTopologies, ';');
	}

	@Deprecated
	/**
	 * WARNING this constructor should not be used at present, as it relies on the internal private obtainTaxaNames() method to get the taxaNames list that is needed for a safe pruneTaxon() call.
	 * At the moment this method is not implemented, and so this constructor is marked as deprecated.
	 * @param tree
	 */
	public NewickTreeRepresentation(String tree){
		this.treeString = tree;
		this.taxaNames = this.obtainTaxaNames(treeString);
		this.numberOfTaxa = taxaNames.size();
		this.numberOfTrees = treeString.replaceAll("\\s+", "").split(";").length;
		this.separateTopologies = new String[this.numberOfTrees];
		if(this.numberOfTrees>0){
			this.separateTopologies = this.treeString.replaceAll("\\s+", "").split(";");
		}else{
			this.separateTopologies[0] = this.treeString;
		}
		this.separateTopologies = StringCharAppender.appendEach(separateTopologies, ';');
	}
	
	public String getTreeString() {
		return treeString;
	}

	public File getTreeFile() {
		return treeFile;
	}

	public int getNumberOfTaxa() {
		return numberOfTaxa;
	}

	public TreeSet<String> getTaxaNames() {
		return taxaNames;
	}
	
	
	public void write(){
		if(this.treeFile == null){
			this.treeFile = new File(System.getProperty("user.dir")+"/NewickTreeRepresentation"+System.currentTimeMillis()+".tre");
		}
		new BasicFileWriter(this.treeFile, this.treeString);
	}
	
	public void write(File outputTreeFile){
		new BasicFileWriter(outputTreeFile, this.treeString);
	}
	

	/**
	 * Write the tree to given file, but label terminal tips iteratively in separate trees / lines
	 * @param outputTreeFile
	 */
	public void writeLabellingTipsRecursively(File outputTreeFile){
		String labelledOutput = this.printIterativelyLabellingTips();
		new BasicFileWriter(outputTreeFile, labelledOutput);
	}

	public void setTreeFile(File newTreeFile){
		this.treeFile = newTreeFile;
	}
	
	private TreeSet<String> obtainTaxaNames(String inputTreeString){
		TreeNode node = new TreeNode(inputTreeString, 1);
		String[] tipNames = node.getTipsBelow();
		TreeSet<String> names = new TreeSet<String>();
		for(String aTip:tipNames){
			names.add(aTip);
		}
		return names;
	}
	
	public void printSimply(){
		System.out.println(this.treeString);
	}
	
	public void pruneTaxon(String taxonToPrune) throws TaxonNotFoundError{
		assert(this.taxaNames.contains(taxonToPrune));
		System.out.println("Attempting to prune taxon "+taxonToPrune);
		if(this.taxaNames.contains(taxonToPrune)){
			String regex = taxonToPrune+":[0-9|\\.]{1,}";
			treeString = treeString.replaceFirst(regex, "");
			System.out.println("tst "+treeString);
			boolean replacementNeeded = true;
			while(replacementNeeded){
				int editsThisPass = 0;
				regex = "\\(\\)";
				String tmp = treeString.replaceAll(regex, "");
				if(tmp != treeString){
					this.treeString = tmp;
					System.out.println("tmp"+tmp);
					System.out.println("tst"+treeString);
					editsThisPass++;
				}
				
				regex = "\\,\\,";
				tmp = treeString.replaceAll(regex, ",");
				if(tmp != treeString){
					this.treeString = tmp;
					System.out.println("tmp"+tmp);
					System.out.println("tst"+treeString);
					editsThisPass++;
				}
				
				regex = "\\(\\,";
				tmp = treeString.replaceAll(regex, "(");
				if(tmp != treeString){
					this.treeString = tmp;
					System.out.println("tmp"+tmp);
					System.out.println("tst"+treeString);
					editsThisPass++;
				}

				regex = "\\,\\)";
				tmp = treeString.replaceAll(regex, ")");
				if(tmp != treeString){
					this.treeString = tmp;
					System.out.println("tmp"+tmp);
					System.out.println("tst"+treeString);
					editsThisPass++;
				}
				
				regex = "\\(:[0-9|\\.]{1,}\\,";
				tmp = treeString.replaceAll(regex, "(");
				if(tmp != treeString){
					this.treeString = tmp;
					System.out.println("tmp"+tmp);
					System.out.println("tst"+treeString);
					editsThisPass++;
				}

				regex = "\\,:[0-9|\\.]{1,}\\)";
				tmp = treeString.replaceAll(regex, ")");
				if(tmp != treeString){
					this.treeString = tmp;
					System.out.println("tmp"+tmp);
					System.out.println("tst"+treeString);
					editsThisPass++;
				}

				
				/* This may be the most complicated edit:
				 * Look for whole taxa (incl. branch lengths) surrounded by brackets.
				 * Remove brackets
				 * Replace .... 
				 */
				System.out.println("Skipping orphan taxa check, refer to javadoc for NewickTreeRepresentation.");
/**
 * 
 * 	VERY IMPORTANT
 *  @since 0.0.1 - Date 7/12/2011
 *  This routine has been removed... seems to be unnecessary for PAML and causes errors.
 *  
 *  BEGIN DELETION
 */
				System.out.println("CHECK: orphan taxa");
				regex = "\\([A-Za-z|\\_|\\-]{1,}:[0-9|\\.]{1,}\\)";
		//		regex = "Bos";
//				String regex2 = "\\([A-Za-z|\\_|\\-]{1,}:[0-9|\\.]{1,}\\)";
				Pattern orphan = Pattern.compile(regex);
				Matcher orphanMatch = orphan.matcher(this.treeString);
				if(orphanMatch.find()){
					String foundOrphan = orphanMatch.group();
					String replacement = foundOrphan.substring(1,foundOrphan.length()-2);
					System.out.println("WARNING! detected orphan taxon "+foundOrphan+", paml may have trouble: tmp"+tmp+"\nReplacing "+replacement);
					tmp = treeString.replaceAll(regex, foundOrphan.substring(1,foundOrphan.length()-2)); //set with an arbitrary branch length...
					this.treeString = tmp;
					System.out.println("tst"+treeString);
					editsThisPass++;
//					tmp = treeString; //just for now, reset tmp just in case...
				}
 				System.out.println("NO: orphan taxa");
/**
 *	END DELETION
 *
 */
				if(editsThisPass < 1){
					replacementNeeded = false;
					System.out.println("Done pruning tree");
				}

			}
		}else{
			throw new TaxonNotFoundError();
		}
	}

	public void writeMultipleReplicates(File outputTreeFile,int numberOfReplicates){
		String out = "";
		for(int i=0;i<numberOfReplicates;i++){
			out += this.treeString + "\n";
		}
		new BasicFileWriter(outputTreeFile, out);
	}

	public NewickTreeRepresentation pruneTaxa(TreeSet<String> taxaToPrune) {
		NewickTreeRepresentation unprunedTree;
		try {
			unprunedTree = (NewickTreeRepresentation) this.clone();
		} catch (CloneNotSupportedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			unprunedTree = this;
		}
		Iterator itrTaxon = taxaToPrune.iterator();
		while(itrTaxon.hasNext()){
			String taxonToPrune = (String)itrTaxon.next().toString().toUpperCase();
			try {
				unprunedTree.pruneTaxon(taxonToPrune);
				System.out.println("Pruned taxon "+taxonToPrune+" from tree.");
			} catch (TaxonNotFoundError e) {
				// TODO Auto-generated catch block
				System.out.println("Couldn't prune taxon "+taxonToPrune+" from tree.");
				e.printStackTrace();
			}
		}
		return unprunedTree;
	}

	public NewickTreeRepresentation concatenate(NewickTreeRepresentation r) throws TaxaListsMismatchException {
		if(this.taxaListsMatch(r)){
			String concatenatedTreeString;
			if(this.treeString.endsWith("\n")){
				concatenatedTreeString = this.treeString  + r.getTreeString();
			}else{
				concatenatedTreeString = this.treeString + "\n" + r.getTreeString();
			}
			return new NewickTreeRepresentation(concatenatedTreeString, this.taxaNames);
		}else{
			JOptionPane.showMessageDialog(null, "Taxon lists do not match - analysis likely to fail!", "Warning!", JOptionPane.WARNING_MESSAGE, UIManager.getIcon("OptionPane.warningIcon"));
			throw new TaxaListsMismatchException();
		}
	}

	/**
	 * Tests whether another NewickTreeRepresentation's taxon list matches this one. Both lists are sorted and compared for string equality
	 * @param otherTaxonList
	 * @return
	 */
	public boolean taxaListsMatch(NewickTreeRepresentation otherTree){
		// get the other list
		TreeSet<String> otherTaxonList = otherTree.getTaxaNames();
		// if they are different lengths (numbers of taxa) we know they don't match already..
		if(this.taxaNames.size() != otherTaxonList.size()){
			return false;
		}else{
			// TreeSet should already be sorted so no need to sort them
			boolean match = true;
			// Cast to String[] arrays, they will be the same size. we'll iterate through them
			String[] theseNames = taxaNames.toArray(new String[taxaNames.size()]);
			String[] otherNames = otherTaxonList.toArray(new String[taxaNames.size()]);
			// We'll use a while loop, incrementing the array index. as soon as we find a mismatch we can stop
			int index = 0;
			while((match) && (index<theseNames.length)){
				if(!theseNames[index].equals(otherNames[index])){
					// they mismatch - set match flag to false, which should terminate the loop
					match = false;
				}
				index++;
			}
			return match;
		}
		
	}
	
	public int getNumberOfTrees() {
		return this.numberOfTrees;
	}

	public String[] getIndividualTrees(){
		return this.separateTopologies;
	}
	
	public String getSpecificTree(int index) throws ArrayIndexOutOfBoundsException{
		return this.separateTopologies[index];
	}

	/**
	 * Returns a String containing one tree for each terminal taxon present, labelling each one in turn.
	 * @return - String
	 */
	public String printIterativelyLabellingTips() {
		// Create the TreeNode
		TreeNode n = new TreeNode(this.treeString,1);
		// Set up buffer
		StringBuffer b = new StringBuffer();
		// Iterate through taxa
		Iterator itr = this.taxaNames.iterator();
		while(itr.hasNext()){
			String[] labelTaxa = {((String) itr.next()).toUpperCase()};
			b.append(n.printRecursivelyLabelling(labelTaxa));
			b.append(";\n");
		}
		// Return buffer
		return b.toString();
	}
	
	/**
	 * Private static class to append a char to each element of a string array.
	 * @author <a href="mailto:joe@kitson-consulting.co.uk">Joe Parker, Kitson Consulting / Queen Mary University of London</a>
	 *
	 */
	private static class StringCharAppender{
		/**
		 * Append a char to each element of a string array.
		 * <br/>If the array has zero elements, the input is returned unchanged.
		 * <br/>If the array has one or more elements, each element has the charToAppend added, unless it is null.
		 * @param input - String[] array
		 * @param charToAppend - char to append
		 * @return
		 */
		public static String[] appendEach(String[] input, char charToAppend){
			int inputLength = input.length;
			// check the input array has nonzero length, proceed if so
			if(input.length>1){
				String[] appendedInput = new String[inputLength];
				for(int element=0;element<inputLength; element++){
					if(input[element] != null){
						// Only append char is element is non-null. Otherwise add a null element, unchanged.
						appendedInput[element] = input[element]+charToAppend;
					}
				}
				return appendedInput;
			}else{
				// No point in doing anything, just return input
				return input;
			}
		}
	}
}
