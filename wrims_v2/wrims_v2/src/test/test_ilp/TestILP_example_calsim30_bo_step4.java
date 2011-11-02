package test.test_ilp;

import java.io.IOException;
import org.antlr.runtime.RecognitionException;
import org.testng.Assert;
import org.testng.annotations.Test;

import wrimsv2.components.*;
import wrimsv2.wreslparser.elements.RegUtils;
import wrimsv2.wreslparser.elements.Tools;

public class TestILP_example_calsim30_bo_step4 {
	
	private double tolerance_perc = 0.0001/1000000; // 0.1 ppb
	private String studyPath;	
	private String dssPath;	
	private double expected;
	
	@Test(groups = { "ilp_example_calsim3" })
	public void calsim3_step4() throws RecognitionException, IOException{
		
		studyPath = "D:\\cvwrsm\\trunk\\wrims_v2\\wrims_v2\\examples\\calsim30_bo_svn51\\";
		dssPath = "D:\\cvwrsm\\trunk\\wrims_v2\\wrims_v2\\examples\\calsim30_bo_svn51\\common\\DSS\\";
		
		/// set control data		
		String[] controlDataString = {
		studyPath + "common\\CVGroundwater\\Data\\",   //groundwater dir
		studyPath + "CONV\\run\\mainCONV_30.wresl",
		dssPath +   "CalSim30_06_SV.dss",
		studyPath + "CONV\\DSS\\Init.dss",   // Init
		studyPath + "CONV\\DSS\\dv.dss",   // dv
		"CalSim30_06",       // sv dv F part
		"CalSim30_06",   // init file F part
		"CALSIM",        // part A
		"1MON",
		"1922",
		"1",
		 "31",
		 "1922",
		 "1",
		 "31", 
		 "ILP", 
		 "csv"};

		FilePaths.ilpFileDirectory = "ilp_TestIlp_example_calsim30_bo_step4";
		//FilePaths.ilpFile = "test.ilp";
		
        new Controller(controlDataString);

        
        expected = 1.6762301226828846E10; 
                                            
		
		double obj_value =  ControlData.xasolver.getObjective();
		
		Assert.assertEquals(obj_value, expected, expected*tolerance_perc);	

	}
	

}
