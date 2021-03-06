package uk.ac.qmul.sbcs.evolution.convergence.tests;

import uk.ac.qmul.sbcs.evolution.convergence.util.SitewiseSpecificLikelihoodSupportAaml;
import junit.framework.TestCase;
import java.io.*;

public class SitewiseSpecificLikelihoodSupportSerializationTest extends TestCase {

	SitewiseSpecificLikelihoodSupportAaml SSLS1;
	SitewiseSpecificLikelihoodSupportAaml SSLS2;
	SitewiseSpecificLikelihoodSupportAaml SSLS3;

	
	public SitewiseSpecificLikelihoodSupportSerializationTest() {
	}

	public SitewiseSpecificLikelihoodSupportSerializationTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSerialization(){
		SSLS1 = new SitewiseSpecificLikelihoodSupportAaml();
		float[] SSLS1a = new float[2];
		SSLS1a[0] = 0.04f;
		SSLS1a[1] = 0.02f;
		SSLS1.setAlpha(SSLS1a);
		SSLS1.setInputFileName("ssls one");
		SSLS2 = new SitewiseSpecificLikelihoodSupportAaml();
		SSLS1a[0] = 10.04f;
		SSLS1a[1] = 40.02f;
		SSLS2.setAlpha(SSLS1a);
		SSLS2.setInputFileName("ssls two");
		SSLS2.setNumberOfModels(1);
		try {
			FileOutputStream fileOutOne = new FileOutputStream("/Users/gsjones/Documents/all_work/programming/java/QMUL_GCP/SSLS.1.ser");
			ObjectOutputStream outOne;
			outOne = new ObjectOutputStream(fileOutOne);
			outOne.writeObject(this.SSLS1);
			outOne.close();
			fileOutOne.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			FileOutputStream fileOutTwo = new FileOutputStream("/Users/gsjones/Documents/all_work/programming/java/QMUL_GCP/SSLS.2.ser");
			ObjectOutputStream outTwo = new ObjectOutputStream(fileOutTwo);
			outTwo.writeObject(this.SSLS2);
			outTwo.close();
			fileOutTwo.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assert(true);
		SSLS1 = null;
		SSLS2 = null;
		assert(true);
	}
	
	public void testInflation(){
		try {
			FileInputStream fileInOne = new FileInputStream("/Users/gsjones/Documents/all_work/programming/java/QMUL_GCP/SSLS.1.ser");
			ObjectInputStream inOne = new ObjectInputStream(fileInOne);
			SSLS1 = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			FileInputStream fileInTwo = new FileInputStream("/Users/gsjones/Documents/all_work/programming/java/QMUL_GCP/SSLS.2.ser");
			ObjectInputStream inTwo = new ObjectInputStream(fileInTwo);
			SSLS2 = (SitewiseSpecificLikelihoodSupportAaml) inTwo.readObject();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(SSLS1.getInputFileName());
		System.out.println(SSLS2.getInputFileName());
		assert(true);
	}

	public void testInflationMultipleActualSSLS(){
		try {
			FileInputStream fileInOne = new FileInputStream("/Users/gsjones/Documents/all_work/programming/java/QMUL_GCP/SSLS.wag.ser");
			ObjectInputStream inOne = new ObjectInputStream(fileInOne);
			SSLS1 = (SitewiseSpecificLikelihoodSupportAaml) inOne.readObject();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			FileInputStream fileInTwo = new FileInputStream("/Users/gsjones/Documents/all_work/programming/java/QMUL_GCP/SSLS.jones.ser");
			ObjectInputStream inTwo = new ObjectInputStream(fileInTwo);
			SSLS2 = (SitewiseSpecificLikelihoodSupportAaml) inTwo.readObject();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			FileInputStream fileInTwo = new FileInputStream("/Users/gsjones/Documents/all_work/programming/java/QMUL_GCP/SSLS.dayhoff.ser");
			ObjectInputStream inTwo = new ObjectInputStream(fileInTwo);
			SSLS3 = (SitewiseSpecificLikelihoodSupportAaml) inTwo.readObject();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(SSLS1.getInputFileName());
		System.out.println(SSLS2.getInputFileName());
		long ssls1_elapsed = SSLS1.elapsed();
		long ssls2_elapsed = SSLS2.elapsed();
		long ssls3_elapsed = SSLS3.elapsed();
		long ssls_1_elapsedMillis = SSLS1.getFinished().getTime() - SSLS1.getStarted().getTime();
		long ssls_2_elapsedMillis = SSLS2.getFinished().getTime() - SSLS2.getStarted().getTime();
		long ssls_3_elapsedMillis = SSLS3.getFinished().getTime() - SSLS3.getStarted().getTime();
		System.out.println(ssls1_elapsed +"\n"+ ssls2_elapsed +"\n"+ ssls3_elapsed);
		System.out.println(ssls_1_elapsedMillis +"\n"+ ssls_2_elapsedMillis +"\n"+ ssls_3_elapsedMillis);
		assert(true);
	}
}
