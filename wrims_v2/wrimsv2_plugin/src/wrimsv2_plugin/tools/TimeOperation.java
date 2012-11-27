package wrimsv2_plugin.tools;

import wrimsv2_plugin.debugger.core.DebugCorePlugin;

public class TimeOperation {
	public static boolean isLeapYear(int year){
		if (year % 4 == 0) {
		    if (year % 100 != 0) {
		    	return true;
		    }else if (year % 400 == 0) {
		    	return true;
		    }else {
		    	return false;
		    }
		}else{
			return false;
		}
	}
	
	public static int numberOfDays(int month, int year){
		int days;
		if (month==1 || month==3 || month==5 || month==7 
				||month==8 || month==10 ||month==12){
			days=31;
		}else if (month==4|| month==6 || month==9 || month==11){ 
			days=30;
		}else {
			if (TimeOperation.isLeapYear(year)){
				days=29;
			}else{
				days=28;
			}
		}
		return days;
	}
	
	public static int[] searchYearMonth(int noOfMonths, int startYear, int startMonth){
		int detYear=noOfMonths/12;
		int detMonth=noOfMonths%12;
		int debugMonth=startMonth+detMonth;
		int debugYear=startYear+detYear;
		if (debugMonth>12){
			debugMonth=debugMonth-12;
			debugYear=debugYear+1;
		}
		int[] yearMonth=new int[2];
		yearMonth[0]=debugYear;
		yearMonth[1]=debugMonth;
		return yearMonth;
	}
	
	public static int findMonthInBetween(int startYear, int startMonth, int endYear, int endMonth){
		return (endYear-startYear)*12+endMonth-startMonth+1;
	}
	
	
	public static int monthValue(String month){
		if (month.equalsIgnoreCase("jan")){
			return 1;
		}else if (month.equalsIgnoreCase("feb")){
			return 2;
		}else if (month.equalsIgnoreCase("mar")){
			return 3;
		}else if (month.equalsIgnoreCase("apr")){
			return 4;
		}else if (month.equalsIgnoreCase("may")){
			return 5;
		}else if (month.equalsIgnoreCase("jun")){
			return 6;
		}else if (month.equalsIgnoreCase("jul")){
			return 7;
		}else if (month.equalsIgnoreCase("aug")){
			return 8;
		}else if (month.equalsIgnoreCase("sep")){
			return 9;
		}else if (month.equalsIgnoreCase("oct")){
			return 10;
		}else if (month.equalsIgnoreCase("nov")){
			return 11;
		}else{
			return 12;
		}
	}
	
	public static String createTimewindow (int year, int month, int day, String timestep){
		String monthStr=getMonthText(month);
		if (timestep.equals("1MON")){
			int lastDay=numberOfDays(month, year);
			return lastDay+monthStr+year+" 2300 "+lastDay+monthStr+year+" 2400";
		}else{
			if (day<10){
				String dayStr="0"+day;
				return dayStr+monthStr+year+" 2300 "+dayStr+monthStr+year+" 2400";
			}else{
				return day+monthStr+year+" 2300 "+day+monthStr+year+" 2400"; 
			}
		}
	}
	
	public static String createStartTime (String detailDate, String timestep){
		String[] date=detailDate.split("-");
		return createStartTime(Integer.parseInt(date[2]), Integer.parseInt(date[0]), Integer.parseInt(date[1]), timestep);
	}
	
	public static String createEndTime (String detailDate, String timestep){
		String[] date=detailDate.split("-");
		return createEndTime(Integer.parseInt(date[2]), Integer.parseInt(date[0]), Integer.parseInt(date[1]), timestep);
	}
	
	public static String createStartTime (int year, int month, int day, String timestep){
		String monthStr=getMonthText(month);
		if (timestep.equals("1MON")){
			int lastDay=numberOfDays(month, year);
			return lastDay+monthStr+year+" 2300";
		}else{
			if (day<10){
				String dayStr="0"+day;
				return dayStr+monthStr+year+" 2300";
			}else{
				return day+monthStr+year+" 2300"; 
			}
		}
	}
	
	public static String createEndTime (int year, int month, int day, String timestep){
		String monthStr=getMonthText(month);
		if (timestep.equals("1MON")){
			int lastDay=numberOfDays(month, year);
			return lastDay+monthStr+year+" 2400";
		}else{
			if (day<10){
				String dayStr="0"+day;
				return dayStr+monthStr+year+" 2400";
			}else{
				return day+monthStr+year+" 2400"; 
			}
		}
	}
	
	public static String getMonthText(int month){
		switch (month){
		case 1:
			return "JAN";
		case 2:
			return "FEB";
		case 3:
			return "MAR";
		case 4:
			return "APR";
		case 5:
			return "MAY";
		case 6:
			return "JUN";
		case 7:
			return "JUL";
		case 8:
			return "AUG";
		case 9:
			return "SEP";
		case 10:
			return "OCT";
		case 11:
			return "NOV";
		}
		return "DEC";
	}
}
