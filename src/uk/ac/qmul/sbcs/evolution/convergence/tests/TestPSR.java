package uk.ac.qmul.sbcs.evolution.convergence.tests;

import java.io.*;
import java.util.*;
import uk.ac.qmul.sbcs.evolution.convergence.*;
import uk.ac.qmul.sbcs.evolution.convergence.util.*;
import javax.swing.*;

public class TestPSR {

	private JFileChooser chooser = new JFileChooser();
	private File inputFile;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new TestPSR().go();
	}
	
	public void go(){
		int fileChosenStatus = chooser.showOpenDialog(null);
		if(fileChosenStatus == JFileChooser.APPROVE_OPTION){
			try {
				inputFile = chooser.getSelectedFile();
				System.out.println("trying to read "+inputFile.getAbsolutePath()+" file\n");
				AlignedSequenceRepresentation PSR = new AlignedSequenceRepresentation();
				try{
					PSR.loadSequences(inputFile,true);
				}catch(TaxaLimitException ex){
					ex.printStackTrace();
				}
/*
				ArrayList<String> rawFileContents = PSR.getRawInput();
				System.out.println("\n\nfirst:\t"+rawFileContents.get(0));
				System.out.println("last:\t"+rawFileContents.get(rawFileContents.size()-1));
*/
				PSR.printShortSequences(20);
				System.out.println("read "+PSR.getNumberOfSites()+" sites and "+PSR.getNumberOfTaxa()+" taxa.");
			} catch (RuntimeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
