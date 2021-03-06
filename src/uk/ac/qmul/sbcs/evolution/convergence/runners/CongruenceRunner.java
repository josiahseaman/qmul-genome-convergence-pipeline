package uk.ac.qmul.sbcs.evolution.convergence.runners;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;

import uk.ac.qmul.sbcs.evolution.convergence.analyses.*;
import uk.ac.qmul.sbcs.evolution.convergence.util.CapitalisedFileReader;

public class CongruenceRunner {

	/**
	 * @param args
	 * 
	 * 	1. dataset
		2. maintrees
		3. constraint
		4. labelled trees
		5. workdir
		6. binaries
		7. filter permissivity %
		8. filter by factor (permissivity) or by number
	 * 
	 * 
	 * @since r125: 06/03/2013
	 * This convergence runner is designed to detect convergence using: 
	 * 		a specified set of AA models in Aaml, 
	 * 		and an unlimited number of topologies. 
	 * It will additionally prune labelled trees for downstream codeml selection analyses.
	 * 
	 * Workflow for revised pipeline:
	 *
	 *	-    alignment parsing, taxon list building
	 *	-    determine # of substitution models, build list of them
	 *	-    prune input.trees
	 *	-    prune labelled.trees
	 *	-    prune constraint.tre
	 *	-    RAxML -g on constraint.tre
	 *	-    cat RAxML.tre with input.trees
	 *	-    for each (model:models) {get sitewise lnL for all trees}
	 *	
     * 	@TODO generally reorganise args
     *	@TODO RAxML -g method
     *	@TODO produce some random trees
     *	@TODO class to read multi-tree lnL files
     *	@TODO taxa list as a file 
     *	@TODO models list as a file
     *	@TODO <b>BIG DEAL</b> - sitewise lnL class to be written
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		File dataSet = new File(args[0]);
		File mainTreesFile = new File(args[1]);
		File constraintTreeFile = new File(args[2]);
		File labelledTreesFile = new File(args[3]);
		File workDir = new File(args[4]);
		String runID = new String(dataSet.getName()+"conv"+System.currentTimeMillis());
		File binaries = new File(args[5]);
		int thisFilter = Integer.parseInt(args[6]);
		boolean doFactor = false;
		switch(Integer.parseInt(args[7])){
			case (1): doFactor = true; break;
			case (0): doFactor = false; break;
		}
		TreeSet<String> taxaList;
		try {
			taxaList = new CongruenceRunner().parseTaxaListConfigFile(args[8]);
			String[] modelsList = {"wag"}; // other models ("jones","dayhoff") disabled for now
			MultiHnCongruenceAnalysis analysis = new MultiHnCongruenceAnalysis(dataSet, mainTreesFile, constraintTreeFile, labelledTreesFile, workDir, binaries, runID, taxaList, modelsList, thisFilter, doFactor);
			analysis.run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Not able to proceed without a taxon list. Check path and retry.");
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param configFileArg - path to file containing a list of possible taxa
	 * @return
	 * @throws Exception 
	 */
	private TreeSet<String> parseTaxaListConfigFile(String configFileArg) throws Exception{
		TreeSet<String> taxaList = new TreeSet<String>();
		File configFile = new File(configFileArg);
		if(configFile.canRead()){
			ArrayList<String> taxa = new CapitalisedFileReader().loadSequences(configFile,false);
			for(String taxon:taxa){
				taxaList.add(taxon);
			}
		}else{
			throw new Exception("Unable to parse the taxon list.\n");
		}
		return taxaList;
	}
}

