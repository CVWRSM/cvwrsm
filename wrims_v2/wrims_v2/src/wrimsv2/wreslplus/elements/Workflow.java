package wrimsv2.wreslplus.elements;

import java.io.File;
import java.util.ArrayList;

import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslparser.elements.StudyParser;
import wrimsv2.wreslplus.elements.procedures.ErrorCheck;
import wrimsv2.wreslplus.elements.procedures.ProcWeight;
import wrimsv2.wreslplus.elements.procedures.ToLowerCase;

public class Workflow {
	
	
	private Workflow(){}


	public static StudyTemp checkStudy(String mainFilePath) {
		
		StudyParser.reset();
		
		String canonicalMainFilePath = Tools.getCanonicalLowCasePath(mainFilePath);
		
		ParserUtils.setRunDir(new File(canonicalMainFilePath).getParent()); 
		
		StudyTemp st = ParserUtils.parseWreslMain(canonicalMainFilePath);
		// TODO: need to check sequence error here
		// check study
		
		if (StudyParser.total_errors>0) {
			
			LogUtils.parsingSummaryMsg("Wresl+ parsing aborted", StudyParser.total_errors);
			
			return null;
		}
		
		ErrorCheck.checkVarRedefined(st);	
				
		
		ToLowerCase.convert(st);	
		// TODO: make backup of original var list
		Procedures.findEffectiveModelinMain(st); // main file only

		Procedures.processIncFilePath(st);
		Procedures.processVarIncFileList(st);
		Procedures.processDependants(st);

		/// put sequence included models into fileModelDataTable
		for (String se : st.seqList){
			
			String modelName = st.seqMap.get(se).model;
			ModelTemp mt = st.modelMap.get(modelName);  // this is model in main file
			
			if (st.fileModelNameMap.keySet().contains(mt.pathRelativeToRunDir)) {
				
				st.fileModelNameMap.get(mt.pathRelativeToRunDir).add(modelName);
				
			} else {
				ArrayList<String> modelNameList = new ArrayList<String>();
				modelNameList.add(modelName);
				st.fileModelNameMap.put(mt.pathRelativeToRunDir, modelNameList);
			}
			
			st.fileModelDataTable.put(mt.pathRelativeToRunDir, modelName, mt);			
			
		}

		/// put model included models into fileModelDataTable
		for (String incM: st.incModelList_effective){
			
			ModelTemp incMt = st.modelMap.get(incM);  // this is model in main file
				
			if (st.fileModelNameMap.keySet().contains(incMt.pathRelativeToRunDir)) {
				
				st.fileModelNameMap.get(incMt.pathRelativeToRunDir).add(incM);
				
			} else {
				ArrayList<String> modelNameList = new ArrayList<String>();
				modelNameList.add(incM);
				st.fileModelNameMap.put(incMt.pathRelativeToRunDir, modelNameList);
			}
			
			st.fileModelDataTable.put(incMt.pathRelativeToRunDir, incM, incMt);			
			
		}
		
		// parse modelList_effective all included files		
		// store results to a map using relativePath as key
		for (String se : st.seqList){
			
			SequenceTemp seqObj = st.seqMap.get(se); 
			String m = seqObj.model;
			
			System.out.println("[M]"+m);

			ModelTemp mt = st.modelMap.get(m);

			ParserUtils.parseAllIncFile(mt.incFileRelativePathList, st);					
			
		}
		
		// parse incModelList_effective all included files		
		// store results to a map using relativePath as key
		for (String incM : st.incModelList_effective){
			
			System.out.println("[incM]"+incM);

			ModelTemp mt = st.modelMap.get(incM);

			ParserUtils.parseAllIncFile(mt.incFileRelativePathList, st);					
			
		}
	

		if (StudyParser.total_errors>0) {
			
			LogUtils.parsingSummaryMsg("Wresl+ parsing stopped", StudyParser.total_errors);
			
			return null;
		}
		
		// find all offspring
		// store results to kidMap and AOMap, fileGroupOrder
		Procedures.findKidMap(st);
		
		
		Procedures.findAllOffSpring(st);
		
		
		Procedures.findFileGroupOrder(st);
		

		Procedures.postProcessIncFileList(st);
		
		
		Procedures.postProcessVarListinIncFile(st);
				
		
		// can be processed at final stage
		Procedures.copyModelVarMapToSequenceVarMap(st);
		
		Procedures.processGoalHS2(st); 
				

		ErrorCheck.checkVarRedefined(st);	
		
		// TODO: test only. remove this after testing
		// TODO: seperate dv dep from others
		// remove dv from alias's dependants
		Procedures.classifyDependants(st);
		

		// check variable used before definition
		ErrorCheck.checkVarUsedBeforeDefined(st);
		
		Procedures.convertAliasToGoal(st); 
		

		
		Procedures.analyzeVarNeededFromCycle(st);
		Procedures.createSpaceInVarCycleValueMap(st);
		
		ProcWeight.collectWeightVar(st);
		
		ProcWeight.processWeightGroup(st);
		

		Procedures.collectTimeStep(st);
		
				
		LogUtils.parsingSummaryMsg("Wresl+ parsing completed",StudyParser.total_errors);
		
		if (StudyParser.total_errors>0) return null;
		
		return st;
		
	}	



}
	
