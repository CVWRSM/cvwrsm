/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package wrimsv2_plugin.debugger.launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import wrimsv2_plugin.debugger.core.DebugCorePlugin;
import wrimsv2_plugin.debugger.exception.WPPException;
import wrimsv2_plugin.debugger.model.WPPDebugTarget;
import wrimsv2_plugin.tools.TimeOperation;

import java.lang.Runtime;

import javax.jws.WebParam.Mode;


public class WPPLaunchDelegate extends LaunchConfigurationDelegate {
	private String externalPath;
	private String gwDataFolder;
	private String mainFile;
	private String svarFile;
	private String initFile;
	private String dvarFile;
	private String svFPart;
	private String initFPart;
	private String aPart;
	private String timeStep;
	private int startYear;
	private int startMonth;
	private int startDay;
	private int endYear;
	private int endMonth;
	private int endDay;
	private String wreslPlus;

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException{
		List commandList = new ArrayList();
			
		// if in debug mode, add debug arguments - i.e. '-debug requestPort eventPort'
		int requestPort = -1;
		int eventPort = -1;
		requestPort = findFreePort();
		eventPort = findFreePort();
		if (requestPort == -1 || eventPort == -1) {
			abort("Unable to find free port", null);
		}
			
		createBatch(configuration, requestPort, eventPort, mode);
		
		try {
			if (mode.equals("debug")){
				DebugCorePlugin.debugSet.reset();
				Process process = Runtime.getRuntime().exec("WRIMSv2_Engine.bat");
				IProcess p = DebugPlugin.newProcess(launch, process, "DebugWPP");
				IDebugTarget target = new WPPDebugTarget(launch, p, requestPort, eventPort);
				launch.addDebugTarget(target);
			}else{
				Process process = Runtime.getRuntime().exec("WRIMSv2_Engine.bat");
				IProcess p = DebugPlugin.newProcess(launch, process, "RunWPP");
			}
		} catch (IOException e) {
			WPPException.handleException(e);
		}
	}
	
	public void createBatch(ILaunchConfiguration configuration, int requestPort, int eventPort, String mode){
		try {
			String studyName=null;
			studyName = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_STUDY, (String)null);
					
			String author=null;
			author = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_AUTHOR, (String)null);
				
			String date=null;
			date = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_DATE, (String)null);
			
			String description=null;
			description = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_DESCRIPTION, (String)null);
				
			mainFile = null;
			mainFile = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_PROGRAM, (String)null);
			
			dvarFile = null;
			dvarFile = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_DVARFILE, (String)null);
			DebugCorePlugin.savedDvFileName=dvarFile;
			
			svarFile = null;
			svarFile = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_SVARFILE, (String)null);
			DebugCorePlugin.savedSvFileName=svarFile;
			
			initFile = null;
			initFile = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_INITFILE, (String)null);
			
			gwDataFolder = null;
			gwDataFolder = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_GWDATAFOLDER, (String)null)+File.separator;
			
			aPart = null;
			aPart = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_APART, (String)null);
			DebugCorePlugin.aPart=aPart;
			
			svFPart = null;
			svFPart = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_SVFPART, (String)null);
			DebugCorePlugin.svFPart=svFPart;
			
			initFPart = null;
			initFPart = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_INITFPART, (String)null);
			DebugCorePlugin.initFPart=initFPart;
			
			timeStep = null;
			timeStep = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_TIMESTEP, (String)null);
			DebugCorePlugin.timeStep=timeStep;
			
			startYear = Integer.parseInt(configuration.getAttribute(DebugCorePlugin.ATTR_WPP_STARTYEAR, (String)null));
			startMonth = TimeOperation.monthValue(configuration.getAttribute(DebugCorePlugin.ATTR_WPP_STARTMONTH, (String)null));
			DebugCorePlugin.startYear=startYear;
			DebugCorePlugin.startMonth=startMonth;
			
			endYear = Integer.parseInt(configuration.getAttribute(DebugCorePlugin.ATTR_WPP_ENDYEAR, (String)null));
			endMonth = TimeOperation.monthValue(configuration.getAttribute(DebugCorePlugin.ATTR_WPP_ENDMONTH, (String)null));
			DebugCorePlugin.endYear=endYear;
			DebugCorePlugin.endMonth=endMonth;
			
			if (timeStep.equals("1MON")){
				startDay=TimeOperation.numberOfDays(startMonth, startYear);
				endDay=TimeOperation.numberOfDays(endMonth, endYear);
			}else{
				startDay= Integer.parseInt(configuration.getAttribute(DebugCorePlugin.ATTR_WPP_STARTDAY, (String)null));
				endDay=Integer.parseInt(configuration.getAttribute(DebugCorePlugin.ATTR_WPP_ENDDAY, (String)null));
			}
			DebugCorePlugin.startDay=startDay;
			DebugCorePlugin.endDay=endDay;
			
			wreslPlus=configuration.getAttribute(DebugCorePlugin.ATTR_WPP_WRESLPLUS, "no");
					
			int index = mainFile.lastIndexOf(File.separator);
			String mainDirectory = mainFile.substring(0, index + 1);
			externalPath = mainDirectory + "External";
			
			String engineFileFullPath = "WRIMSv2_Engine.bat";
			try {
				String configFilePath = generateConfigFile();
				FileWriter debugFile = new FileWriter(engineFileFullPath);
				PrintWriter out = new PrintWriter(debugFile);
				generateBatch(out, mode, requestPort, eventPort, configFilePath);
			}catch (IOException e) {
				WPPException.handleException(e);
			}
		} catch (CoreException e) {
			WPPException.handleException(e);
		}
	}
	
	public void generateBatch(PrintWriter out, String mode, int requestPort, int eventPort, String configFilePath){
		out.println("@echo off");
		out.println();
		out.println("set path=" + externalPath + ";"+"lib;%path%");
		out.println();
		if (mode.equals("debug")){
			out.println("jre\\bin\\java -Xmx1600m -Xss1024K -Duser.timezone=UTC -Djava.library.path=\"" + externalPath + ";lib\" -cp \""+externalPath+";"+"lib\\external"+";lib\\WRIMSv2.jar;lib\\commons-io-2.1.jar;lib\\XAOptimizer.jar;lib\\lpsolve55j.jar;lib\\gurobi.jar;lib\\heclib.jar;lib\\jnios.jar;lib\\jpy.jar;lib\\misc.jar;lib\\pd.jar;lib\\vista.jar;lib\\guava-11.0.2.jar;lib\\javatuples-1.2.jar;\" wrimsv2.components.DebugInterface "+requestPort+" "+eventPort+" "+"-config="+configFilePath);
		}else{
			out.println("jre\\bin\\java -Xmx1600m -Xss1024K -Duser.timezone=UTC -Djava.library.path=\"" + externalPath + ";lib\" -cp \""+externalPath+";"+"lib\\external;lib\\WRIMSv2.jar;lib\\commons-io-2.1.jar;lib\\XAOptimizer.jar;lib\\lpsolve55j.jar;lib\\gurobi.jar;lib\\heclib.jar;lib\\jnios.jar;lib\\jpy.jar;lib\\misc.jar;lib\\pd.jar;lib\\vista.jar;lib\\guava-11.0.2.jar;lib\\javatuples-1.2.jar;\" wrimsv2.components.ControllerBatch "+"-config="+configFilePath);
		}
		out.close();
	}
	
	public String generateConfigFile(){
		
		Map<String, String> configMap = new HashMap<String, String>();
		String configFilePath = null;
		
		try {				
			
			configMap.put("MainFile".toLowerCase(), mainFile);
			configMap.put("DvarFile".toLowerCase(),   dvarFile);
			configMap.put("SvarFile".toLowerCase(),   svarFile);
			configMap.put("SvarAPart".toLowerCase(),  aPart);
			configMap.put("SvarFPart".toLowerCase(),  svFPart);
			configMap.put("InitFile".toLowerCase(),   initFile);
			configMap.put("InitFPart".toLowerCase(),  initFPart);
			configMap.put("TimeStep".toLowerCase(),   timeStep);
			configMap.put("StartYear".toLowerCase(),  String.valueOf(startYear));
			configMap.put("StopYear".toLowerCase(),   String.valueOf(endYear));
			configMap.put("StartMonth".toLowerCase(), String.valueOf(startMonth));
			configMap.put("StopMonth".toLowerCase(),  String.valueOf(endMonth));
			
			if (gwDataFolder.length()>0) {
				configMap.put("groundwaterdir".toLowerCase(), gwDataFolder);
			} else {
				configMap.put("groundwaterdir".toLowerCase(), ".");
			}
			
			configMap.put("ShowWreslLog".toLowerCase(), "No");			
			if (DebugCorePlugin.solver.equals("XA") && DebugCorePlugin.log.equals("Log")){
				configMap.put("Solver".toLowerCase(), DebugCorePlugin.solver+"LOG");
			}else{
				configMap.put("Solver".toLowerCase(), DebugCorePlugin.solver);
			}
				
			if (DebugCorePlugin.log.equals("Log")){
				configMap.put("IlpLog".toLowerCase(), "Yes");
				if (DebugCorePlugin.solver.equals("XA")){
					configMap.put("IlpLogFormat".toLowerCase(), "CplexLp");
				}else if (DebugCorePlugin.solver.equals("LPSolve")){
					configMap.put("IlpLogFormat".toLowerCase(), "LpSolve");
				}
				configMap.put("IlpLogVarValue".toLowerCase(), "Yes");
			} else {
				configMap.put("IlpLog".toLowerCase(), "No");
				configMap.put("IlpLogFormat".toLowerCase(), "None");
				configMap.put("IlpLogVarValue".toLowerCase(), "No");
			}
			
			configMap.put("WreslPlus".toLowerCase(), wreslPlus);
	
			String mainFileAbsPath = configMap.get("MainFile".toLowerCase());
			
			String studyDir = new File(mainFileAbsPath).getParentFile().getParentFile().getAbsolutePath();
			String configName = "__study.config";
			File f = new File(studyDir, configName);
			File dir = new File(f.getParent());
			dir.mkdirs();
			f.createNewFile();
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
			 
			out.println("##################################################################################");
			out.println("# Command line Example:");
			out.println("# C:\\wrimsv2_SG\\bin\\runConfig_limitedXA.bat D:\\example\\EXISTING_BO.config");
			out.println("# ");	
			out.println("# Note:");			
			out.println("# 1. This config file and the RUN directory must be placed in the same directory.");
			out.println("# 2. Use relative path to increase the portability.");
			out.println("#    For example, use RUN\\main.wresl for MainFile and DSS\\INIT.dss for InitFile");
			out.println("##################################################################################");	
			out.println("");
			out.println("");
			
			out.println("MainFile           "+configMap.get("MainFile".toLowerCase()));
			out.println("Solver             "+configMap.get("solver".toLowerCase()));
			out.println("DvarFile           "+configMap.get("DvarFile".toLowerCase()));
			out.println("SvarFile           "+configMap.get("SvarFile".toLowerCase()));
			out.println("GroundwaterDir     "+configMap.get("groundwaterdir".toLowerCase()));
			out.println("SvarAPart          "+configMap.get("SvarAPart".toLowerCase()));
			out.println("SvarFPart          "+configMap.get("SvarFPart".toLowerCase()));
			out.println("InitFile           "+configMap.get("InitFile".toLowerCase()));
			out.println("InitFPart          "+configMap.get("InitFPart".toLowerCase()));
			out.println("TimeStep           "+configMap.get("TimeStep".toLowerCase()));
			out.println("StartYear          "+configMap.get("StartYear".toLowerCase()));
			out.println("StartMonth         "+configMap.get("StartMonth".toLowerCase()));
			out.println("StopYear           "+configMap.get("StopYear".toLowerCase()));
			out.println("StopMonth          "+configMap.get("StopMonth".toLowerCase()));
			out.println("IlpLog             "+configMap.get("IlpLog".toLowerCase()));
			out.println("IlpLogFormat       "+configMap.get("IlpLogFormat".toLowerCase()));
			out.println("IlpLogVarValue     "+configMap.get("IlpLogVarValue".toLowerCase()));
			out.println("WreslPlus          "+configMap.get("WreslPlus".toLowerCase()));
			
			if (DebugCorePlugin.solver.equalsIgnoreCase("LpSolve")) {
				
				out.println("LpSolveConfigFile         callite.lpsolve");
				out.println("LpSolveNumberOfRetries    2");				
				
			}
			
			out.close();
		
			configFilePath= new File(studyDir, configName).getAbsolutePath();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return configFilePath;
			
	}
	
	/**
	 * Throws an exception with a new status containing the given
	 * message and optional exception.
	 * 
	 * @param message error message
	 * @param e underlying exception
	 * @throws CoreException
	 */
	private void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, DebugCorePlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, message, e));
	}
	
	/**
	 * Returns a free port number on localhost, or -1 if unable to find a free port.
	 * 
	 * @return a free port number on localhost, or -1 if unable to find a free port
	 */
	public static int findFreePort() {
		ServerSocket socket= null;
		try {
			socket= new ServerSocket(0);
			return socket.getLocalPort();
		} catch (IOException e) { 
			WPPException.handleException(e);
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					WPPException.handleException(e);
				}
			}
		}
		return -1;		
	}		
}
