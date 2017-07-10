package wrimsv2.sql;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;
import wrimsv2.evaluator.DataTimeSeries;
import wrimsv2.evaluator.DssDataSetFixLength;
import wrimsv2.evaluator.DssOperation;
import wrimsv2.evaluator.TimeOperation;

public class SQLServerRWriter{
	
	private String JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";       
    private String tableName = ControlData.sqlGroup;                 //input 
	private String scenarioTableName="Scenario";
	
	private String URL;
	private String database;
	
	private Connection conn = null;
	private Statement stmt = null;
	
	private String scenarioName=FilePaths.sqlScenarioName;
	private int scenarioIndex;
	
	private String slackPrefix="slack__";
	private String surplusPrefix="surplus__";
	private String csvRemotePath;
	private String csvLocalPath;
	private File csvFile;
	
	public SQLServerRWriter(){   
		connectToDataBase();
	}
			
	public void process(){
		if (tableName.equalsIgnoreCase(scenarioTableName)){
			tableName=tableName+"_Studies";
		}
		setScenarioIndex();
		createTable();
		deleteOldData();
		createCSV();
		writeData();
		close();
	}
	
	public void connectToDataBase(){	
		
		try {
			if (ControlData.databaseURL.contains(";")){
				int index=ControlData.databaseURL.lastIndexOf(";");
				URL=ControlData.databaseURL.substring(0, index);
				database=ControlData.databaseURL.substring(index+14);
			}else{
				URL=ControlData.databaseURL;
				database=ControlData.databaseURL;
			}
			
			Class.forName(JDBC_DRIVER);
			System.out.println("Connecting to a selected database...");
			conn = DriverManager.getConnection(URL, ControlData.USER, ControlData.PASS);
			stmt = conn.createStatement();
			String sql="IF (db_id('"+database+"') IS NULL) CREATE DATABASE "+database;
			stmt.executeUpdate(sql);
			sql="USE "+database;
			stmt.executeUpdate(sql);
			System.out.println("Connected database successfully");
		} catch (ClassNotFoundException e) {
			System.err.println("Failed to load database. Please install the database driver.");
			System.out.println("Model run terminated.");
			System.exit(1);
		} catch (SQLException e) {
			System.err.println("Failed to connect to the database. Please check your database URL and user profile.");
			System.out.println("Model run terminated.");
			System.exit(1);
		}
	}
	
	public void setScenarioIndex(){
		System.out.println("Setting scenario index...");
		try {
			stmt = conn.createStatement();
			String sql = "IF (object_id('"+scenarioTableName+"', 'U') IS NULL) CREATE TABLE "+ scenarioTableName + " (ID Integer NOT NULL, Table_Name VarChar(40), Scenario VarChar(80), Part_A VarChar(20), Part_F VarChar(30), PRIMARY KEY(ID))";
			stmt.executeUpdate(sql);
			sql="select ID FROM "+scenarioTableName+" WHERE TABLE_NAME='"+tableName+"' AND SCENARIO='"+scenarioName+"' AND PART_A='"+ControlData.partA+"' AND PART_F='"+ControlData.svDvPartF+"'";
			ResultSet rs1 = stmt.executeQuery(sql);
			if (!rs1.next()){
				rs1.close();
				sql="select count(*) AS rc from "+scenarioTableName;
				ResultSet rs2 = stmt.executeQuery(sql);
				rs2.next();
				int count=rs2.getInt("rc");
				rs2.close();
				if (count==0){
					scenarioIndex=0;
				}else{
					sql="select Max(ID) as maxIndex from "+scenarioTableName;
					ResultSet rs3 = stmt.executeQuery(sql);
					rs3.next();
					int maxIndex=rs3.getInt("maxIndex");
					rs3.close();
					scenarioIndex=maxIndex+1;
				}
				sql="INSERT into "+scenarioTableName+" VALUES ("+scenarioIndex+", '" + tableName + "', '"+scenarioName+"', '"+ControlData.partA+"', '"+ControlData.svDvPartF+"')";
				stmt.executeUpdate(sql);
			}else{
				scenarioIndex=rs1.getInt("ID");
				rs1.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Set scenario index");
	}
	
	public void createTable(){
		System.out.println("Creating table in given database...");
		try {
			stmt = conn.createStatement();
			String sql = "IF (object_id('"+tableName+"', 'U') IS NULL) CREATE TABLE " + tableName + " (ID int, Timestep VarChar(8), Units VarChar(20), Date_Time smalldatetime, Variable VarChar(40), Kind VarChar(30), Value Float(8))";
			stmt.executeUpdate(sql);
			System.out.println("Created table in given database");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteOldData(){
		try {
			System.out.println("Deleting old data in table...");
			Statement statement = conn.createStatement();
			String sql="DELETE FROM "+tableName+" WHERE ID="+scenarioIndex;
			stmt.executeUpdate(sql);
			System.out.println("Deleted old data in table");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createCSV(){
		String line;
		System.out.println("Writing output to CSV file");
		int index = FilePaths.fullDvarDssPath.lastIndexOf(".");
		String csvFilePath = FilePaths.fullDvarDssPath.substring(0, index)+".csv";
		csvFilePath=csvFilePath.replaceFirst(":", "");
		csvLocalPath="G:\\tempCSV\\"+ControlData.USER+"\\"+csvFilePath;
		String host=URL.toLowerCase().replaceFirst("jdbc:sqlserver://", "");
		host="\\\\"+host.substring(0, host.lastIndexOf(":"));
		csvRemotePath=host+"\\tempCSV\\"+ControlData.USER+"\\"+csvFilePath;
		//csvRemotePath="\\\\dwrmsdb0113.ad.water.ca.gov\\tempCSV\\test.csv";
				
		try {
			csvFile= new File(csvRemotePath);
			csvFile.getParentFile().mkdirs();
			FileWriter fw = new FileWriter(csvFile);
			BufferedWriter bw = new BufferedWriter(fw, 8192);
			Set<String> keys = DataTimeSeries.dvAliasTS.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()){
				String name=it.next();
				if (!name.startsWith(slackPrefix) && !name.startsWith(surplusPrefix)){
					DssDataSetFixLength ts = DataTimeSeries.dvAliasTS.get(name);
					String timestep=ts.getTimeStep().toUpperCase();
					if (timestep.equals("1DAY")){
						Date date = ts.getStartTime();
						String unitsName=formUnitsName(ts);
						String variableName=formVariableName(name);
						String kindName=formKindName(ts.getKind());
						double[] data = ts.getData();
						for (int i=0; i<data.length; i++){
							line = scenarioIndex+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+data[i]+"\n";
							bw.write(line);
							date=addOneDay(date);
						}
					}else{
						Date date = ts.getStartTime();
						String unitsName=formUnitsName(ts);
						String variableName=formVariableName(name);
						String kindName=formKindName(ts.getKind());
						double[] data = ts.getData();
						for (int i=0; i<data.length; i++){
							line = scenarioIndex+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+data[i]+"\n";
							bw.write(line);
							date=addOneMonth(date);
						}
					}
				}
			}
			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Wrote output to CSV file");
	}
	
	public void writeData(){
		try {
			System.out.println("Importing output into table...");
			stmt = conn.createStatement();
			String sql = "Bulk INSERT "+tableName+" From '"+csvLocalPath+"' WITH (FIELDTERMINATOR=',', ROWTERMINATOR='\n')";
			stmt.executeUpdate(sql);
			System.out.println("Imported output into table");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String formUnitsName(DssDataSetFixLength ts){
		String units=ts.getUnits().replaceAll("/", "_").replaceAll("-", "_");
		return units;
	}
	
	public String formVariableName(String name){
		String variableName = DssOperation.getTSName(name).replaceAll("-", "_");
		return variableName;
	}
	
	public String formKindName(String name){
		String kindName = name.replaceAll("-", "_");
		return kindName;
	}
	
	public String formDateData(Date date){
		int year=date.getYear()+1900;
		int month=date.getMonth()+1;
		int day = date.getDate();
		return year+"-"+TimeOperation.monthNameNumeric(month)+"-"+TimeOperation.dayName(day)+" 00:00:00";
	}
	
	public Date addOneMonth(Date date){
		int month=date.getMonth()+1;
		int year=date.getYear();
		if (month>11){
			month=month-12;
			year=year+1;
		}
		int day=TimeOperation.numberOfDays(month+1, year+1900);
		Date newDate = new Date(year, month, day);
		return newDate;
	}
	
	public Date addOneDay(Date date){
		long newTime=date.getTime()+1 * 24 * 60 * 60 * 1000l;
		Date newDate = new Date (newTime);
		return newDate;
	}
	
	public void close(){
		try {
			stmt.close();
			conn.close();
			if (csvFile.exists()) csvFile.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
