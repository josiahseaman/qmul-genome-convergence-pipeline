package uk.ac.qmul.sbcs.evolution.convergence.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import uk.ac.qmul.sbcs.evolution.convergence.ParsimonyReconstruction;
import uk.ac.qmul.sbcs.evolution.convergence.StateComparison;
import uk.ac.qmul.sbcs.evolution.convergence.TreeNode;
import uk.ac.qmul.sbcs.evolution.convergence.util.SitewiseSpecificLikelihoodSupportAaml;
import junit.framework.TestCase;

public class TreeNodeTest extends TestCase {

	public TreeNodeTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testTreeNodeStringInt() {
		TreeNode n1 = new TreeNode("((a,b),c)",1);
		TreeNode n2 = new TreeNode("((a:0.01,b:0.02):0.01,c:0.03)",1);
		TreeNode n3 = new TreeNode("((a:0.01,b:0.02):-0.01,c:0.03)",1);
		TreeNode n4 = new TreeNode("((a,b),(c,(d,e)))",1);
		assert(true);
	}

	public void testAssignStates(){
		HashMap<String, HashSet<String>[]> states = new HashMap<String,HashSet<String>[]>();
		HashSet<String>[] hseta = (HashSet<String>[]) Array.newInstance(HashSet.class, 2);
		HashSet<String>[] hsetb = (HashSet<String>[]) Array.newInstance(HashSet.class, 2);
		HashSet<String>[] hsetc = (HashSet<String>[]) Array.newInstance(HashSet.class, 2);
		hseta[0] = new HashSet<String>();
		hsetb[0] = new HashSet<String>();
		hsetc[0] = new HashSet<String>();
		hseta[0].add("black");
		hsetb[0].add("white");
		hsetc[0].add("black");
		hseta[1] = new HashSet<String>();
		hsetb[1] = new HashSet<String>();
		hsetc[1] = new HashSet<String>();
		hseta[1].add("black");
		hsetb[1].add("white");
		hsetc[1].add("fushcia");
		
		states.put("a", hseta);
		states.put("b", hsetb);
		states.put("c", hsetc);
		for (int i = 0; i < 10; i++) {
			System.out.println("rep "+i);
			TreeNode n1 = new TreeNode("((a,b),c)", 1);
			HashSet<String>[] baseStates = n1.getFitchStates(states);
			System.out.println("rep "+i+" set initial states");
			n1.printStates();
			n1.resolveFitchStatesTopnode();
			n1.resolveFitchStates(n1.states);
			n1.getEndPos();
			System.out.println("rep "+i+" final states");
			n1.printStates();
		}
	}
	
	public void testPrestinTrees(){
		TreeNode n1 = new TreeNode("(((LOXODONTA:0.080618,DASYPUS:0.028235):0.002756,(((((CANIS:0.012983,FELIS:0.013897):0.005719,(EQUUS:0.028437,((TURSIOPS:0.038936,BOS:0.016707):0.003048,VICUGNA:0.031996):0.004509):0.006443):0.000004,(MYOTIS:0.056507,((RHINOLOPHUS:0.066174,MEGADERMA:0.021473):0.006671,PTEROPUS:0.015521):0.000004):0.008379):0.002227,(SOREX:0.022136,ERINACEUS:0.013937):0.004338):0.004428,((MUS:0.034943,(ORYCTOLAGUS:0.021193,OCHOTONA:0.063783):0.025907):0.003677,(PAN:0.010448,HOMO:0.001622):0.021809):0.002889):0.000004):0.144025,MONODELPHIS:0.113014)",1);
		TreeNode n2 = new TreeNode("(((LOXODONTA: 0.080618, DASYPUS: 0.028235): 0.002756, (((((CANIS: 0.012983, FELIS: 0.013897): 0.005719, (EQUUS: 0.028437, ((TURSIOPS: 0.038936, BOS: 0.016707): 0.003048, VICUGNA: 0.031996): 0.004509): 0.006443): 0.000004, (MYOTIS: 0.056507, ((RHINOLOPHUS: 0.066174, MEGADERMA: 0.021473): 0.006671, PTEROPUS: 0.015521): 0.000004): 0.008379): 0.002227, (SOREX: 0.022136, ERINACEUS: 0.013937): 0.004338): 0.004428, ((MUS: 0.034943, (ORYCTOLAGUS: 0.021193, OCHOTONA: 0.063783): 0.025907): 0.003677, (PAN: 0.010448, HOMO: 0.001622): 0.021809): 0.002889): 0.000004): 0.144025, MONODELPHIS: 0.113014)",1);
		TreeNode n3 = new TreeNode("(((LOXODONTA: 0.080618, DASYPUS: 0.028235): 0.002756, (((((CANIS: 0.012983, FELIS: 0.013897): 0.005719, (EQUUS: 0.028437, ((TURSIOPS: 0.038936, BOS: 0.016707): 0.003048, VICUGNA: 0.031996): 0.004509): 0.006443): 0.000004, (MYOTIS: 0.056507, ((RHINOLOPHUS: 0.066174, MEGADERMA: 0.021473): 0.006671, PTEROPUS: 0.015521): 0.000004): 0.008379): 0.002227, (SOREX: 0.022136, ERINACEUS: 0.013937): 0.004338): 0.004428, ((MUS: 0.034943, (ORYCTOLAGUS: 0.021193, OCHOTONA: 0.063783): 0.025907): 0.003677, (PAN: 0.010448, HOMO: 0.001622): 0.021809): 0.002889): 0.000004): 0.144025, MONODELPHIS: 0.113014);",1);
		n1.getEndPos();
	}
	
	public void testReinflateSerAndAssignStates() throws IOException, ClassNotFoundException{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		HashMap<String, HashSet<String>[]> states = candidate.getDataset().getAminoAcidsAsFitchStates();
		HashSet<String>[] baseStates = species.getFitchStates(states).clone();
		int ambiguousAtRoot = 0;
		for(HashSet<String> statesSet:baseStates){
			if(statesSet.size()>1){
				ambiguousAtRoot++;
			}
		}
		species.resolveFitchStatesTopnode();
		species.resolveFitchStates(species.states);
		ParsimonyReconstruction pr = new ParsimonyReconstruction(states, species);
		pr.printAncestralComparison();
		String[] echolocators = {"MEGADERMA","RHINOLOPHUS","MYOTIS","PTERONOTUS"};
		String[] echolocatorsH2 = {"TURSIOPS","MEGADERMA","RHINOLOPHUS","MYOTIS","PTERONOTUS"};
		String[] controls = {"EIDOLON","PTEROPUS"};
		int pll_H1 = pr.findParallelSubtitutionsFromAncestral(echolocators, true);
		int pll_H1c= pr.findParallelSubtitutionsFromAncestralRejectingAmbiguities(echolocators,baseStates, true);
		int pll_H2 = pr.findParallelSubtitutionsFromAncestral(echolocatorsH2, true);
		int pll_H2c= pr.findParallelSubtitutionsFromAncestralRejectingAmbiguities(echolocatorsH2,baseStates,true);
		int pll_H2o= pr.findParallelSubtitutionsFromAncestralRejectingAmbiguitiesControllingOutgroups(echolocatorsH2,baseStates,true,controls);
		System.out.println("\nParallel H1\t\t"+pll_H1+"\nParallel H1c\t\t"+pll_H1c+"\nParallel H2\t\t"+pll_H2+"\nParallel H2c\t\t"+pll_H2c+"\nParallel H2o\t\t"+pll_H2o+"\n(Ambiguous at root:\t"+ambiguousAtRoot+")\n");
		species.getEndPos();
	}

	public void testPrint() throws IOException, ClassNotFoundException{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		String[] echolocatorsH2 = {"TURSIOPS","MEGADERMA","RHINOLOPHUS","MYOTIS","PTERONOTUS"};
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		species.printTree();
		System.out.println(species.printRecursively()+";");
		System.out.println(species.printRecursivelyLabelling(echolocatorsH2)+";");
		species.getEndPos();
	}

	public void testSubtreeContains() throws IOException, ClassNotFoundException{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		String[] echolocatorsH2 = {"TURSIOPS","MEGADERMA","RHINOLOPHUS","MYOTIS","PTERONOTUS"};
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		if(species.subtreeContains(echolocatorsH2)){
			fail();
		}
	}

	public void testSubtreeContainsAll() throws IOException, ClassNotFoundException{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		String[] allTaxa = {"LOXODONTA","DASYPUS","CANIS","FELIS","EQUUS","TURSIOPS","BOS","VICUGNA","MYOTIS","RHINOLOPHUS","MEGADERMA","PTEROPUS","SOREX","ERINACEUS","MUS","ORYCTOLAGUS","OCHOTONA","PAN","HOMO","MONODELPHIS"};
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		if(!species.subtreeContains(allTaxa)){
			fail();
		}
	}

	public void testPrintPaml() throws IOException, ClassNotFoundException{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		String[] echolocatorsH2 = {"TURSIOPS","MEGADERMA","RHINOLOPHUS","MYOTIS","PTERONOTUS"};
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		String[] trees = candidate.getFittedTrees();
		for(int i=0;i<trees.length;i++){
			TreeNode tree = new TreeNode(trees[i].replaceAll("\\s", ""),1);
			System.out.println("tree_"+i+" = "+tree.printRecursivelyLabelling(echolocatorsH2)+";");
		}
		candidate.getDataset().writePhylipFile("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/quick.phy");
	}
	
	public void testAreTipsPresentNonePresent() throws Exception{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		HashMap<String, HashSet<String>[]> states = candidate.getDataset().getAminoAcidsAsFitchStates();
		HashSet<String>[] baseStates = species.getFitchStates(states).clone();
		int ambiguousAtRoot = 0;
		for(HashSet<String> statesSet:baseStates){
			if(statesSet.size()>1){
				ambiguousAtRoot++;
			}
		}
		species.resolveFitchStatesTopnode();
		species.resolveFitchStates(species.states);
		ParsimonyReconstruction pr = new ParsimonyReconstruction(states, species);
		pr.printAncestralComparison();
		String[] echolocators = {"LOXO","MYO","TUR","HOM"};
		HashSet<String> echoMap = new HashSet<String>(Arrays.asList(echolocators));
		echoMap = species.areTipsPresent(echoMap);
		if(echoMap.size() != 0){
			fail();
		}
	}

	public void testAreTipsPresentAllPresent() throws Exception{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		HashMap<String, HashSet<String>[]> states = candidate.getDataset().getAminoAcidsAsFitchStates();
		HashSet<String>[] baseStates = species.getFitchStates(states).clone();
		int ambiguousAtRoot = 0;
		for(HashSet<String> statesSet:baseStates){
			if(statesSet.size()>1){
				ambiguousAtRoot++;
			}
		}
		species.resolveFitchStatesTopnode();
		species.resolveFitchStates(species.states);
		ParsimonyReconstruction pr = new ParsimonyReconstruction(states, species);
		pr.printAncestralComparison();
		String[] echolocators = {"MEGADERMA","RHINOLOPHUS","MYOTIS"};
		HashSet<String> echoMap = new HashSet<String>(Arrays.asList(echolocators));
		echoMap = species.areTipsPresent(echoMap);
		if(echoMap.size() != 3){
			fail();
		}
	}

	public void testAreTipsPresent() throws Exception{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		HashMap<String, HashSet<String>[]> states = candidate.getDataset().getAminoAcidsAsFitchStates();
		HashSet<String>[] baseStates = species.getFitchStates(states).clone();
		int ambiguousAtRoot = 0;
		for(HashSet<String> statesSet:baseStates){
			if(statesSet.size()>1){
				ambiguousAtRoot++;
			}
		}
		species.resolveFitchStatesTopnode();
		species.resolveFitchStates(species.states);
		ParsimonyReconstruction pr = new ParsimonyReconstruction(states, species);
		pr.printAncestralComparison();
		String[] echolocators = {"MEGADERMA","RHINOLOPHUS","MYOTIS","PTERONOTUS"};
		HashSet<String> echoMap = new HashSet<String>(Arrays.asList(echolocators));
		echoMap = species.areTipsPresent(echoMap);
		if(echoMap.size() != 3){
			fail();
		}
	}
	
	public void testGetTipAndMCRAStatesOf() throws Exception{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		HashMap<String, HashSet<String>[]> states = candidate.getDataset().getAminoAcidsAsFitchStates();
		HashSet<String>[] baseStates = species.getFitchStates(states).clone();
		int ambiguousAtRoot = 0;
		for(HashSet<String> statesSet:baseStates){
			if(statesSet.size()>1){
				ambiguousAtRoot++;
			}
		}
		species.resolveFitchStatesTopnode();
		species.resolveFitchStates(species.states);
		ParsimonyReconstruction pr = new ParsimonyReconstruction(states, species);
		pr.printAncestralComparison();
		String[] echolocators = {"MEGADERMA","RHINOLOPHUS","MYOTIS","PTERONOTUS"};
		HashSet<String> echoMap = new HashSet<String>(Arrays.asList(echolocators));
		echoMap = species.areTipsPresent(echoMap);
		HashMap<String,HashSet<String>[]> ancAndOthers = species.getTipAndMRCAStatesOf(echoMap);
		HashSet<String>[] MRCAstates = ancAndOthers.remove("MRCA");
		int numParallel = new StateComparison(MRCAstates,ancAndOthers).countParallelChanges();
		String[] echolocatorsH2 = {"TURSIOPS","MEGADERMA","RHINOLOPHUS","MYOTIS","PTERONOTUS"};
		String[] controls = {"EIDOLON","PTEROPUS"};
		int pll_H1 = pr.findParallelSubtitutionsFromAncestral(echolocators, true);
		int pll_H1c= pr.findParallelSubtitutionsFromAncestralRejectingAmbiguities(echolocators,baseStates, true);
		int pll_H2 = pr.findParallelSubtitutionsFromAncestral(echolocatorsH2, true);
		int pll_H2c= pr.findParallelSubtitutionsFromAncestralRejectingAmbiguities(echolocatorsH2,baseStates,true);
		int pll_H2o= pr.findParallelSubtitutionsFromAncestralRejectingAmbiguitiesControllingOutgroups(echolocatorsH2,baseStates,true,controls);
		System.out.println("\nParallel H1\t\t"+pll_H1+"\nParallel H1c\t\t"+pll_H1c+"\nParallel H2\t\t"+pll_H2+"\nParallel H2c\t\t"+pll_H2c+"\nParallel H2o\t\t"+pll_H2o+"\n(Ambiguous at root:\t"+ambiguousAtRoot+")\n");
		
		species.getEndPos();
	}
	
	public void testHowManyTips() throws Exception{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		if(species.howManyTips() != 20){
			fail();
		}
	}

	public void testHowManyFromMonophyleticSetTree13() {
		String t13 = "(VN21: 0.000004, (((JJD001: 0.000004, (((MN23: 0.000004, ZY28: 0.000004): 0.000004, (RHFE: 0.030798, (PTEROPUS: 0.059729, (((ERINACEUS: 0.000004, (((MUS: 0.064403, (ORYCTOLAGUS: 0.036155, OCHOTONA: 0.023128): 0.018503): 0.036734, (PAN: 0.000004, HOMO: 0.000004): 0.000004): 0.000004, (MONODELPHIS: 0.124955, LOXODONTA: 0.000004): 0.000004): 0.000004): 0.020019, ((CANIS: 0.019464, FELIS: 0.018551): 0.018938, (EQUUS: 0.042617, (TURSIOPS: 0.021097, BOS: 0.000004): 0.084114): 0.041578): 0.000004): 0.000004, MYOTIS: 0.062492): 0.000004): 0.057992): 0.010215): 0.000004, (RHPECH: 0.000004, (((YLD001: 0.000004, WYS0705: 0.000004): 0.000004, ZY11: 0.000004): 0.000004, JSL055: 0.000004): 0.000004): 0.018900): 0.000004): 0.000004, ((VN005: 0.000004, ((NBCP011: 0.000004, YL005: 0.000004): 0.000004, RHYU: 0.000004): 0.000004): 0.000004, RHPEPE: 0.000004): 0.000004): 0.000004, FLD002: 0.000004): 0.000004, B014: 0.000004)";
		TreeNode species = new TreeNode(t13.replaceAll("\\s", ""),1);
		HashSet<String> taxaChinensis = new HashSet<String>();
		taxaChinensis.add("RHPECH");
		taxaChinensis.add("JSL055");
		taxaChinensis.add("WYS0705");
		taxaChinensis.add("YLD001");
		taxaChinensis.add("ZY11");
		int howMany = species.howManyFromMonophyleticSet(taxaChinensis);
		if(howMany != taxaChinensis.size()){
			fail();
		}
	}

	public void testHowManyFromMonophyleticSetTree1() {
		String t13 = "((NBCP011: 0.100004, ((RHYU: 0.100004, ((WYS0705: 0.100004, (ZY11: 0.100004, (FLD002: 0.100004, ((JSL055: 0.100004, (RHPECH: 0.100004, ((RHPEPE: 0.100004, ((MN23: 0.100004, ZY28: 0.100004): 0.100004, (RHFE: 0.187342, (PTEROPUS: 0.336618, (((ERINACEUS: 0.797420, (((MUS: 0.262004, (ORYCTOLAGUS: 0.168029, OCHOTONA: 0.197419): 0.131167): 0.163877, (PAN: 0.100004, HOMO: 0.100004): 0.102283): 0.103581, (MONODELPHIS: 0.215115, DASYPUS: 0.318737): 0.105996): 0.100004): 0.100004, ((CANIS: 0.186406, FELIS: 0.314986): 0.163642, (EQUUS: 0.265279, ((TURSIOPS: 0.120532, BOS: 0.186697): 0.100004, VICUGNA: 0.413095): 0.100004): 0.100004): 0.100004): 0.148946, MYOTIS: 0.314254): 0.133378): 0.100004): 0.100004): 0.126881): 0.100004, YL005: 0.100004): 0.100004): 0.100004): 0.100004, YLD001: 0.100004): 0.100004): 0.100004): 0.100004): 0.100004, VN005: 0.100004): 0.100004): 0.100004, JJD001: 0.100004): 0.100004): 0.100004, VN21: 6.116110, B014: 0.100004)";
		TreeNode species = new TreeNode(t13.replaceAll("\\s", ""),1);
		HashSet<String> taxaChinensis = new HashSet<String>();
		taxaChinensis.add("RHPECH");
		taxaChinensis.add("JSL055");
		taxaChinensis.add("WYS0705");
		taxaChinensis.add("YLD001");
		taxaChinensis.add("ZY11");
		int howMany = species.howManyFromMonophyleticSet(taxaChinensis);
		if(howMany == taxaChinensis.size()){
			fail();
		}
	}

	public void testHowManyFromMonophyleticSetTree2() {
		String t13 = "((NBCP011: 0.100004, ((RHYU: 0.100004, ((WYS0705: 0.100004, (B014: 0.100004, (FLD002: 0.100004, ((JSL055: 0.100004, (VN21: 0.100004, ((RHPEPE: 0.100004, ((MN23: 0.100004, ZY28: 0.100004): 0.100004, (RHFE: 0.187342, (PTEROPUS: 0.336618, (((ERINACEUS: 0.797420, (((MUS: 0.262004, (ORYCTOLAGUS: 0.168029, OCHOTONA: 0.197419): 0.131167): 0.163877, (PAN: 0.100004, HOMO: 0.100004): 0.102283): 0.103581, (MONODELPHIS: 0.215115, DASYPUS: 0.318737): 0.105996): 0.100004): 0.100004, ((CANIS: 0.186406, FELIS: 0.314986): 0.163642, (EQUUS: 0.265279, ((TURSIOPS: 0.120532, BOS: 0.186697): 0.100004, VICUGNA: 0.413095): 0.100004): 0.100004): 0.100004): 0.148946, MYOTIS: 0.314254): 0.133378): 0.100004): 0.100004): 0.126881): 0.100004, YL005: 0.100004): 0.100004): 0.100004): 0.100004, YLD001: 0.100004): 0.100004): 0.100004): 0.100004): 0.100004, VN005: 0.100004): 0.100004): 0.100004, JJD001: 0.100004): 0.100004): 0.100004, RPECH: 6.116110, ZY11: 0.100004)";
		TreeNode species = new TreeNode(t13.replaceAll("\\s", ""),1);
		HashSet<String> taxaChinensis = new HashSet<String>();
		taxaChinensis.add("RHPECH");
		taxaChinensis.add("JSL055");
		taxaChinensis.add("WYS0705");
		taxaChinensis.add("YLD001");
		taxaChinensis.add("ZY11");
		int howMany = species.howManyFromMonophyleticSet(taxaChinensis);
		if(howMany == taxaChinensis.size()){
			fail();
		}
	}

	public void testContainsMonophyleticCladeTree13() {
		String t13 = "(VN21: 0.000004, (((JJD001: 0.000004, (((MN23: 0.000004, ZY28: 0.000004): 0.000004, (RHFE: 0.030798, (PTEROPUS: 0.059729, (((ERINACEUS: 0.000004, (((MUS: 0.064403, (ORYCTOLAGUS: 0.036155, OCHOTONA: 0.023128): 0.018503): 0.036734, (PAN: 0.000004, HOMO: 0.000004): 0.000004): 0.000004, (MONODELPHIS: 0.124955, LOXODONTA: 0.000004): 0.000004): 0.000004): 0.020019, ((CANIS: 0.019464, FELIS: 0.018551): 0.018938, (EQUUS: 0.042617, (TURSIOPS: 0.021097, BOS: 0.000004): 0.084114): 0.041578): 0.000004): 0.000004, MYOTIS: 0.062492): 0.000004): 0.057992): 0.010215): 0.000004, (RHPECH: 0.000004, (((YLD001: 0.000004, WYS0705: 0.000004): 0.000004, ZY11: 0.000004): 0.000004, JSL055: 0.000004): 0.000004): 0.018900): 0.000004): 0.000004, ((VN005: 0.000004, ((NBCP011: 0.000004, YL005: 0.000004): 0.000004, RHYU: 0.000004): 0.000004): 0.000004, RHPEPE: 0.000004): 0.000004): 0.000004, FLD002: 0.000004): 0.000004, B014: 0.000004)";
		TreeNode species = new TreeNode(t13.replaceAll("\\s", ""),1);
		HashSet<String> taxaChinensis = new HashSet<String>();
		taxaChinensis.add("RHPECH");
		taxaChinensis.add("JSL055");
		taxaChinensis.add("WYS0705");
		taxaChinensis.add("YLD001");
		taxaChinensis.add("ZY11");
		int howMany = species.howManyFromMonophyleticSet(taxaChinensis);
		if(!species.containsMonophyleticClade(taxaChinensis)){
			fail();
		}
	}
	
	public void testContainsMonophyleticClade() throws Exception{
		InputStream serfile = new FileInputStream("/Users/gsjones/Documents/all_work/QMUL/FSD/results_revision_mar2013/g_100_ENSG0000PRESTIN_ng.fas/g_100_ENSG0000PRESTIN_ng.fasinput100.faconv1367283909044wag.ser");
		ObjectInputStream inOne = new ObjectInputStream(serfile);
		SitewiseSpecificLikelihoodSupportAaml candidate = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		TreeNode species = new TreeNode(candidate.getFittedTrees()[0].replaceAll("\\s", ""),1);
		HashSet<String> someTaxa = new HashSet<String>();
		if(species.containsMonophyleticClade(someTaxa)){
			fail();
		}
		someTaxa.add("RHINOLOPHUS");
//		if(!species.containsMonophyleticClade(someTaxa)){
//			fail();
//		}
		someTaxa.add("MEGADERMA");
		if(!species.containsMonophyleticClade(someTaxa)){
			fail();
		}
		someTaxa.add("LOXODONTA");
		if(species.containsMonophyleticClade(someTaxa)){
			fail();
		}
		someTaxa = new HashSet<String>();
		someTaxa.add("LOXODONTA");
		someTaxa.add("DASYPUS");
		someTaxa.add("CANIS");
		someTaxa.add("FELIS");
		someTaxa.add("EQUUS");
		someTaxa.add("TURSIOPS");
		someTaxa.add("BOS");
		someTaxa.add("VICUGNA");
		someTaxa.add("MYOTIS");
		someTaxa.add("RHINOLOPHUS");
		someTaxa.add("MEGADERMA");
		someTaxa.add("PTEROPUS");
		someTaxa.add("SOREX");
		someTaxa.add("ERINACEUS");
		someTaxa.add("MUS");
		someTaxa.add("ORYCTOLAGUS");
		someTaxa.add("OCHOTONA");
		someTaxa.add("PAN");
		someTaxa.add("HOMO");
		someTaxa.add("MONODELPHIS");
		if(!species.containsMonophyleticClade(someTaxa)){
			fail();
		}
	}
}
