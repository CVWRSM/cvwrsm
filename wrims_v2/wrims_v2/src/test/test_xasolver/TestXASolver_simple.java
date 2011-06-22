package test.test_xasolver;

import java.io.IOException;
import org.antlr.runtime.RecognitionException;
import org.testng.Assert;
import org.testng.annotations.Test;

import wrimsv2.components.*;
import wrimsv2.wreslparser.elements.RegUtils;
import wrimsv2.wreslparser.elements.Tools;

public class TestXASolver_simple {
	
	private double tolerance_perc = 0.1/1000000; // 0.1 ppm
	private String logFilePath;	
	private String studyPath;	
	private String dssPath;	
	private String objMatchString = "L P   O P T I M A L   S O L U T I O N ---> OBJ:\\s+\\d+.\\d+";
	private double expected;
	
	@Test(groups = { "xa_solver_example_studies" })
	public void example1_step1() throws RecognitionException, IOException{
		
		studyPath = "D:\\cvwrsm\\trunk\\wrims_v2\\wrims_v2\\examples\\example1\\";
		dssPath = "D:\\cvwrsm\\trunk\\wrims_v2\\wrims_v2\\examples\\example1\\DSS\\";
		
		/// set control data		
		String[] controlDataString = {
		studyPath + "run\\mainEx1.wresl",
		dssPath +   "ExampleSV.dss",
		dssPath +   "ExampleINIT.dss",
		"",

		"EXAMPLE",
		"INIT",
		"CALSIM",
		"1MON",
		"1921",
		"10",
		 "31",
		 "1921",
		 "10",
		 "31", 
		 "XA", 
		 "csv_Example1"};
		
        new Controller(controlDataString);

		// TODO: compare objective function value 
        // wrims v1 XA16 OBJ: 41169932.383266 
        // wrims v2 XA16 OBJ: 41169932.835543
        
        expected = 41169932.383266;

        
        logFilePath = studyPath+"run//xa.log";
		String logText = Tools.readFileAsString(logFilePath);	

		int n = RegUtils.timesOfMatches(logText, objMatchString);
		Assert.assertEquals(n, 1);
		
		String line = RegUtils.getLastMatch(logText, objMatchString);
		String value = RegUtils.getLastMatch(line, "\\d+.\\d+");
		
		double obj_value =  Double.parseDouble(value);
		
		Assert.assertEquals(obj_value, expected, expected*tolerance_perc);	

	}
	

	@Test(groups = { "xa_solver_example_studies" })
	public void example3_step1() throws RecognitionException, IOException{
		
		studyPath = "D:\\cvwrsm\\trunk\\wrims_v2\\wrims_v2\\examples\\example3\\";
		dssPath = "D:\\cvwrsm\\trunk\\wrims_v2\\wrims_v2\\examples\\example3\\DSS\\";
		
		/// set control data		
		String[] controlDataString = {
		studyPath + "run\\mainEx3.wresl",
		dssPath +   "ExampleSV.dss",
		dssPath +   "ExampleINIT.dss",
		"",
	
		"EXAMPLE",
		"INIT",
		"CALSIM",
		"1MON",
		"1921",
		"10",
		 "31",
		 "1921",
		 "10",
		 "31", 
		 "XA", 
		 "csv_Example3"};
		
	    new Controller(controlDataString);
	
		// TODO: compare objective function value 
	    // wrims v1 XA16 OBJ: 44636443.556727 
	    // wrims v2 XA16 OBJ: 
	    
	    expected = 44636443.556727;
	
	    
	    logFilePath = studyPath+"run//xa.log";
		String logText = Tools.readFileAsString(logFilePath);	
	
		int n = RegUtils.timesOfMatches(logText, objMatchString);
		Assert.assertEquals(n, 1);
		
		String line = RegUtils.getLastMatch(logText, objMatchString);
		String value = RegUtils.getLastMatch(line, "\\d+.\\d+");
		
		double obj_value =  Double.parseDouble(value);
		
		Assert.assertEquals(obj_value, expected, expected*tolerance_perc);	
	
	}
	

}
