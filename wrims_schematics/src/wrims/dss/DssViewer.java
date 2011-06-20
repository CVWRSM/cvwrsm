package wrims.dss;

import hec.dataTable.HecDataTableFrame;
import hec.gfx2d.G2dDialog;
import hec.heclib.util.HecTime;
import hec.hecmath.DSS;
import hec.hecmath.DSSFile;
import hec.hecmath.DSSFileException;
import hec.hecmath.HecMath;
import hec.hecmath.HecMathException;
import hec.hecmath.TimeSeriesMath;
import hec.io.PairedDataContainer;
import hec.io.TimeSeriesContainer;
import hec.script.Plot;
import hec.script.Tabulate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import wrims.schematic.SchematicUtils;

/**
 * 
 * @author Tom Pruitt (primary)
 * @author Clay Booher
 * 
 */
public class DssViewer implements Outputer {
	public static boolean DEBUG = false;

	public static final int PLOT = 1;

	public static final int MONTHLY = 2;

	public static final int TABLE = 3;

	public static final int ALL = 4;

	public static final int EXCEEDENCE = 5; // CB added

	public static final int ANNUAL_TOTAL = 6; // CB added

	public static final int ANNUAL_TOTAL_EXCEEDENCE = 7; // CB added

	public static final int VALUES = 8; // CB added

	public static final int MONTHLY_AVERAGE = 9; // CB added

	public static final String PLOT_STRING = "Plot";

	public static final String EXCEEDENCE_STRING = "Exceed"; // CB added

	public static final String ANNUAL_TOTAL_STRING = "Annual"; // CB added

	public static final String ANNUAL_TOTAL_EXCEEDENCE_STRING = "Ann Exceed"; // CB
	// added

	public static final String MONTHLY_AVERAGE_STRING = "Monthly Avg"; // CB
	// added

	public static final String VALUES_STRING = "Period Values"; // CB added

	public static final String MONTHLY_STRING = "Monthly";

	public static final String TABLE_STRING = "Table";

	public static final String ALL_STRING = "All";

	private static double FACTOR = 1000. * 43560 / (24 * 60 * 60); // CB added
	// for
	// slight
	// speed
	// improvement

	public static final String months[] = { "Jan", "Feb", "Mar", "Apr", "May",
			"Jun", "Jul", "Aug", // CB moved to here from a method
			"Sep", "Oct", "Nov", "Dec" };

	public static final boolean DSS_DEBUG = false; // CB added

	private static Hashtable _outputHashtable = new Hashtable(6);

	String _mode = null;

	int _annualTypeMode = MessagePanel.WATERYEAR;

	String _timewindow = null;

	Vector<String> _paths = null; // CB TODO have two separate paths as well;
	// dvar and svar to speed up single file
	// loading????????????????????????

	Vector<DTSWrapper> _dtsWrappers; // CB added

	HashMap<Integer, Object> _DssFiles = null;

	HashMap<Integer, Object> _unselectedDssFiles = null;

	HashMap<Integer, Object> _Fparts = null;

	// CB String[] _keys; //CB added to pass in the checked file lines

	int[] _keys; // CB added to pass in the checked file lines

	ArrayList _months = null;

	private HecMath _baseSet = null; // CB added

	private boolean _keepVariableData = true; // CB added for updating variable
	// period value boxes

	private Hashtable<String, HecMath>[] _allVariableData; // CB added for
	// updating variable
	// period value
	// boxes for single
	// month periods

	// private Hashtable<String, Hashtable<String, HecMath>> _allVariableTWData;
	// //CB added for updating variable period value boxes for time window
	// periods

	private Hashtable<String, Hashtable<String, String[]>> _allVariableTWData; // CB
	// added
	// for
	// updating
	// variable
	// period
	// value
	// boxes
	// for
	// time
	// window
	// periods

	private Hashtable<String, Double> _longTermTafToCfsConversionFactors;

	String _initialUnits;

	// CB boolean _isAnnualTot = false;

	String _units = "CFS";

	private Preferences _userPrefs; // CB added

	// private Vector<String> _variables; //CB added for value box updating
	private Hashtable<String, Object> _variables; // CB added for value box
	// updating

	static {
		createOutputHashtable();
	}

	public DssViewer(String mode, int annualMode, String tw,
			Vector<String> paths, HashMap<Integer, Object> DssFiles,
			HashMap<Integer, Object> Fparts,
			// CB String[] keys, ArrayList<Integer> months) { //CB added annual
			// mode, keys, & derived timeseries
			int[] keys, ArrayList<Integer> months) { // CB added annual mode,
		// keys, & derived
		// timeseries
		this(mode, annualMode, tw, paths, null, DssFiles, Fparts, keys, months);
	}

	// CB public DssViewer(String mode, String tw, Vector paths, HashMap
	// DssFiles, HashMap Fparts,
	// CB ArrayList months) {
	// CB added annual mode, keys, & derived timeseries
	public DssViewer(String mode, int annualMode, String tw,
			Vector<String> paths,
			Vector<DTSWrapper> dtsWrappers,
			HashMap<Integer, Object> DssFiles,
			// CB HashMap<Integer, Object> Fparts, String[] keys,
			// ArrayList<Integer> months) {
			HashMap<Integer, Object> Fparts, int[] keys,
			ArrayList<Integer> months) {
		_mode = mode;
		_annualTypeMode = annualMode; // CB added
		_timewindow = tw;
		_paths = paths;
		_dtsWrappers = dtsWrappers;
		_DssFiles = DssFiles; // CB Two files per key here - GOOD
		_Fparts = Fparts;
		_keys = keys; // CB added
		// CB _months = months;
		_months = getCorrectedMonthsList(months, annualMode); // CB
		_units = null;
		// CB _isAnnualTot = false;
		_userPrefs = Preferences.userNodeForPackage(getClass()); // CB added
	}

	/**
	 * CB added for updating value boxes.
	 * 
	 * @param mode
	 * @param tw
	 * @param paths
	 * @param DssFiles
	 * @param Fparts
	 * @param variableNames
	 */
	// CB added annual mode, keys, & derived timeseries
	public DssViewer(String mode, int annualMode,
			String tw,
			Vector<String> paths,
			// CB HashMap<Integer,Object> DssFiles, HashMap<Integer, Object>
			// Fparts, String[] keys,
			HashMap<Integer, Object> DssFiles, HashMap<Integer, Object> Fparts,
			int[] keys, ArrayList months, Hashtable<String, Object> variables) {
		this(mode, annualMode, tw, paths, DssFiles, Fparts, keys, months);
		_variables = variables;
	}

	/**
	 * CB added.
	 * 
	 * @param months
	 * @param annualMode
	 * @return
	 */
	private ArrayList<Integer> getCorrectedMonthsList(
			ArrayList<Integer> months, int annualMode) {
		if (months == null) {
			return null;
		}
		ArrayList<Integer> list = new ArrayList<Integer>(months.size());
		int[] wyMonthOrder = { 10, 11, 12, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		if (annualMode == MessagePanel.WATERYEAR) {
			return months;
		} else {
			for (int i = 0; i < wyMonthOrder.length; ++i) {
				int newMonth = 0;
				if (annualMode == MessagePanel.CALENDAR_YEAR) {
					newMonth = wyMonthOrder[i] - 9;
				} else if (annualMode == MessagePanel.FEDERAL_CONTRACT_YEAR) {
					newMonth = wyMonthOrder[i] - 7;
				} else {
					return list; // return empty list early
				}
				if (newMonth < 1) {
					newMonth += 12;
				}
				if (months.contains(newMonth)) {
					list.add(newMonth);
				}
			}
			return list;
		}
	}

	private static void createOutputHashtable() {
		_outputHashtable.put(PLOT_STRING, new Integer(PLOT));
		_outputHashtable.put(EXCEEDENCE_STRING, new Integer(EXCEEDENCE)); // CB
		// added
		_outputHashtable.put(ANNUAL_TOTAL_STRING, new Integer(ANNUAL_TOTAL)); // CB
		// added
		_outputHashtable.put(ANNUAL_TOTAL_EXCEEDENCE_STRING, new Integer(
				ANNUAL_TOTAL_EXCEEDENCE)); // CB added
		_outputHashtable.put(MONTHLY_AVERAGE_STRING, new Integer(
				MONTHLY_AVERAGE)); // CB added
		_outputHashtable.put(MONTHLY_STRING, new Integer(MONTHLY));
		_outputHashtable.put(TABLE_STRING, new Integer(TABLE));
		_outputHashtable.put(ALL_STRING, new Integer(ALL));
		_outputHashtable.put(VALUES_STRING, new Integer(VALUES));
	}

	public static Integer getOutputType(String outputType) {
		return (Integer) _outputHashtable.get(outputType);
	}

	// CB void show(int type, String units, boolean exceedence, boolean
	// annualTotal) {
	void show(int type, String units) { // CB
		boolean exceedence = false; // CB
		boolean annualTotal = false; // CB
		boolean monthlyAverage = false; // CB

		// CB added block
		if (type == EXCEEDENCE) {
			exceedence = true;
			annualTotal = false;
		} else if (type == ANNUAL_TOTAL) {
			exceedence = false;
			annualTotal = true;
		} else if (type == ANNUAL_TOTAL_EXCEEDENCE) {
			exceedence = true;
			annualTotal = true;
		} else if (type == MONTHLY_AVERAGE) { // CB added MONTHLY_AVERAGE
			monthlyAverage = true;
		}

		if (DEBUG) {
			System.out.println("type:" + type);
			System.out.println("units:" + units);
			System.out.println("mode:" + _mode);
			System.out.println("tw:" + _timewindow);
			System.out.println("paths:" + _paths.size());
			System.out.println("paths:" + _paths);
			System.out.println("_DssFiles:" + _DssFiles);
			System.out.println("_Fparts:" + _Fparts);
			System.out.println("_months:" + _months);
		}
		_units = units;
		String title = getPlotTitle(exceedence, annualTotal, monthlyAverage);

		if (type == TABLE) {
			tabulate(title);
		} else if ((type == PLOT) || (type == EXCEEDENCE)
				|| (type == ANNUAL_TOTAL) || (type == ANNUAL_TOTAL_EXCEEDENCE)
				|| (type == MONTHLY_AVERAGE)) {
			// CB plot(title, exceedence, annualTotal);
			plot(title, type);
		} else if (type == MONTHLY) {
			monthly(title);
		} else if (type == ALL) {
			allOutputs(title);
			// else if (type == VALUES)
			// singleStepValues();
		}
	}

	/**
	 * CB added.
	 * 
	 * @param units
	 * @param periodDate
	 */
	// Hashtable<String, Double>[] show(String units, String periodDate) { //CB
	// or use list index
	Hashtable<String, String>[] show(String units, String periodDate) { // CB or
		// use
		// list
		// index
		if (periodDate.trim().equals("")) {
			return null;
		}
		if (_allVariableTWData == null) {
			JOptionPane.showMessageDialog(null, "Time period, " + periodDate
					+ ", data not loaded yet",
					"Monthly Data Not Finished Loading",
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
		// loadVariableData(units, -1); //TO DO: set _units using listener on
		// the units swing component?
		_units = units;
		// At this point, _allvariableData contains data sets of variables which
		// have associated units
		// Hashtable<String, Double>[] results = new
		// Hashtable[_allvariableData.length];
		Hashtable<String, String>[] results = new Hashtable[_allVariableData.length];
		Hashtable<String, String[]> twData = _allVariableTWData.get(periodDate); // long-term
		// average
		// (time
		// window)
		// data

		for (int i = 0; i < _allVariableData.length; ++i) {
			// results[i] = new Hashtable<String, Double>();
			results[i] = new Hashtable<String, String>();
		}
		// NumberFormat formatter = new DecimalFormat("#."); //"##" shows one
		// decimal place and so does "0"!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// NumberFormat formatter = new DecimalFormat("%6.0f");
		Enumeration<String> variableEnum = _variables.keys();
		while (variableEnum.hasMoreElements()) {
			String name = variableEnum.nextElement();
			// if (name.toUpperCase().equals("S_TRNTY"))
			// System.out.print("");
			for (int j = 0; j < _allVariableData.length; ++j) {
				if (_allVariableData[j].get(name) != null) {
					HecMath dataSet = _allVariableData[j].get(name);
					boolean isTAFSelected = (_units.equalsIgnoreCase("taf") ? true
							: false);
					dataSet = unitsConversion(dataSet, isTAFSelected);
					try {
						TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet
								.getData(); // will contain only one value
						// tsc.precision = 0; //CB does not appear to work
						HecTime hecStartTime = new HecTime();
						hecStartTime.set(tsc.startTime);
						HecTime ht = new HecTime();
						if (periodDate.indexOf("-") == -1) {
							ht.setDate(periodDate);
							int valueIndex = (ht.year() - hecStartTime.year())
									* 12 + (ht.month() - hecStartTime.month()); // offset
							// (index)
							// from
							// the
							// startdate
							if (valueIndex < 0) {
								System.out.println();
							}
							results[j].put(name, tsc.values[valueIndex] + " "
									+ dataSet.getUnits()); // j is 0 for alt. 1
							// OR 1 for alt. 2
						} else { // multiple-month time window LONG-TERM
							// AVERAGES
							if (twData.get(name) != null) {
								if (isTAFSelected
										&& _initialUnits.equals("CFS")
										&& (tsc.fullName.indexOf("STORAGE/") == -1)) {
									String value = twData.get(name)[j]
											.substring(0, twData.get(name)[j]
													.indexOf(" "));
									double val = Double.parseDouble(value)
											/ _longTermTafToCfsConversionFactors
													.get(periodDate);
									results[j].put(name, val + " taf"); // j is
									// 0 for
									// alt.
									// 1 OR
									// 1 for
									// alt.
									// 2
								} else if (!isTAFSelected
										&& _initialUnits.equals("TAF")
										&& (tsc.fullName.indexOf("STORAGE/") == -1)) {
									String value = twData.get(name)[j]
											.substring(0, twData.get(name)[j]
													.indexOf(" "));
									double val = Double.parseDouble(value)
											* _longTermTafToCfsConversionFactors
													.get(periodDate);
									results[j].put(name, val + " cfs"); // j is
									// 0 for
									// alt.
									// 1 OR
									// 1 for
									// alt.
									// 2
								} else {
									results[j].put(name, twData.get(name)[j]); // j
									// is
									// 0
									// for
									// alt.
									// 1
									// OR
									// 1
									// for
									// alt.
									// 2
								}
							}
						}
					} catch (HecMathException hme) {
						hme.printStackTrace();
					} catch (NullPointerException npe) {
						JOptionPane
								.showMessageDialog(
										null,
										"Long-term data not quite ready, please try equest again",
										"Too Soon!", JOptionPane.ERROR_MESSAGE);
						return results;
					}
				}
			}
		}
		return results;
	}

	/**
	 * CB added to load long-term averages in background This should be called
	 * from within <code>SwingUtilities.invokeLater</code>.
	 */
	/*
	 * public void calculateLongTermAverages_worksslower(Vector<String> periods)
	 * { long startTime = System.currentTimeMillis(); _initialUnits = _units;
	 * _longTermTafToCfsConversionFactors = new Hashtable<String, Double>();
	 * _allVariableTWData = new Hashtable<String, Hashtable<String,
	 * String[]>>(); //for time window periods Hashtable<String, String[]>
	 * results; // for one time window! Enumeration<String> periodEnum =
	 * periods.elements(); while (periodEnum.hasMoreElements()) { String period
	 * = periodEnum.nextElement(); if (period.indexOf("-") == -1) { break; //
	 * not a multi-month period, so go to next period } else { // multiple-month
	 * time window results = new Hashtable<String, String[]>(); // for one time
	 * window!
	 * 
	 * String[] split = period.split(" +"); String startMonth =
	 * split[0].substring(0, 3); String endMonth = split[2].substring(0, 3);
	 * String startYear = split[0].substring(3); String endYear =
	 * split[2].substring(3); int firstStartMonthIndex = -1;
	 * 
	 * int endMonthIndex = -1; for (int i = 0; i < DssViewer.months.length; ++i)
	 * { if (DssViewer.months[i].equalsIgnoreCase(startMonth))
	 * firstStartMonthIndex = i; if
	 * (DssViewer.months[i].equalsIgnoreCase(endMonth)) endMonthIndex = i; } int
	 * sYear = Integer.parseInt(startYear); int eYear =
	 * Integer.parseInt(endYear); String date = ""; int eMonth = -1;
	 * Enumeration<String> variableEnum = _variables.keys(); boolean
	 * isCalculatedConversions = false; boolean setCalculatedConversionsTrue =
	 * true; while (variableEnum.hasMoreElements()) { String name =
	 * variableEnum.nextElement(); // if (name.equals("S_SHSTA")) //
	 * System.out.print(""); String[] dataPair = new
	 * String[_allVariableData.length]; for (int j = 0; j <
	 * _allVariableData.length; ++j) { if (_allVariableData[j].get(name) !=
	 * null) { HecMath dataSet = (HecMath)_allVariableData[j].get(name); HecMath
	 * dataSetCopy = null; //to calculate conversion factor for the term
	 * TimeSeriesContainer tscOfCopy = null; //to calculate conversion factor
	 * for the term boolean isTAFSelected = (_units.equalsIgnoreCase("taf") ?
	 * true : false); try { if (!isCalculatedConversions) dataSetCopy =
	 * dataSet.copy(); dataSet = unitsConversion(dataSet, isTAFSelected);
	 * TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet.getData(); //
	 * will contain only one value
	 * 
	 * HecTime hecStartTime = new HecTime(); hecStartTime.set(tsc.startTime);
	 * HecTime ht = new HecTime();
	 * 
	 * if (!isCalculatedConversions) tscOfCopy = (TimeSeriesContainer)
	 * dataSetCopy.getData(); // will contain only one value if
	 * (!isCalculatedConversions) { if (tsc.units.equals(tscOfCopy.units)) { if
	 * (tscOfCopy.units.equalsIgnoreCase("CFS")) { dataSetCopy =
	 * unitsConversion(dataSetCopy, true); tscOfCopy = (TimeSeriesContainer)
	 * dataSetCopy.getData(); } else if
	 * (tscOfCopy.units.equalsIgnoreCase("TAF")) { dataSetCopy =
	 * unitsConversion(dataSetCopy, false); tscOfCopy = (TimeSeriesContainer)
	 * dataSetCopy.getData(); } } } int numValues = 0; //reset it double sum =
	 * 0.0; //reset it double sumOfCopy = 0.0; //reset it int startMonthIndex =
	 * firstStartMonthIndex; //reset it for (int year = sYear; year <= eYear;
	 * ++year) { if (year != sYear) startMonthIndex = 0; if (year != eYear)
	 * eMonth = 11; else eMonth = endMonthIndex; for (int month =
	 * startMonthIndex; month <= eMonth; ++month) { date =
	 * DssViewer.months[month] + String.valueOf(year); ht.setDate(date); int
	 * valueIndex = (ht.year() - hecStartTime.year()) * 12 + (ht.month() -
	 * hecStartTime.month()); //offset (index) from the startdate sum +=
	 * tsc.values[valueIndex]; if (!isCalculatedConversions) sumOfCopy +=
	 * tscOfCopy.values[valueIndex]; ++numValues; } } if
	 * (!isCalculatedConversions) { if (tscOfCopy.units.equalsIgnoreCase("CFS"))
	 * { _longTermTafToCfsConversionFactors.put(period, sumOfCopy/sum);
	 * setCalculatedConversionsTrue = true; } else if
	 * (tscOfCopy.units.equalsIgnoreCase("TAF")) {
	 * _longTermTafToCfsConversionFactors.put(period, sum/sumOfCopy);
	 * setCalculatedConversionsTrue = true; } } try { dataPair[j] =
	 * sum/numValues + " " + dataSet.getUnits(); //j is 0 for alt. 1 OR 1 for
	 * alt. 2 if (j == _allVariableData.length - 1) { //only need to add once
	 * results.put(name, dataPair); // System.out.println(name + ": " +
	 * dataPair[0] + ", " + dataPair[1]); } } catch (NumberFormatException nfe)
	 * { //TODO do anything? } if (setCalculatedConversionsTrue) {
	 * isCalculatedConversions = true; setCalculatedConversionsTrue = false; } }
	 * catch (HecMathException hme){ hme.printStackTrace(); } } else { } } }
	 * _allVariableTWData.put(period, results); } } long stopTime =
	 * System.currentTimeMillis();
	 * System.out.println("long term averages calculation time = " + (stopTime -
	 * startTime)); }
	 */

	/**
	 * CB added to load long-term averages in background This should be called
	 * from within <code>SwingUtilities.invokeLater</code>.
	 */
	public boolean calculateLongTermAverages(Vector<String> periods,
			int dssType, boolean isSelectedFiles) {
		// public Hashtable<String, Hashtable<String, String[]>>
		// calculateLongTermAverages(Vector<String> periods) {//trying to fix
		// GUI lockup
		long startTime = System.currentTimeMillis();
		_initialUnits = _units;
		_longTermTafToCfsConversionFactors = new Hashtable<String, Double>();
		// Hashtable<String, String[]>[] results = new
		// Hashtable[periods.size()];
		Hashtable<String, String[]>[] results = null;
		// Hashtable<String, Hashtable<String, String[]>> allVariableTWData =
		// new Hashtable<String, Hashtable<String, String[]>>(); //for time
		// window periods
		// _allVariableTWData = new Hashtable<String, Hashtable<String,
		// String[]>>(); //for time window periods
		if (_allVariableTWData == null) { // for time window periods
			_allVariableTWData = new Hashtable<String, Hashtable<String, String[]>>();
		}
		if (results == null) {
			results = new Hashtable[periods.size()];
		}
		String[] terms = new String[periods.size()];
		HecTime[] startTimes = new HecTime[periods.size()];
		HecTime[] endTimes = new HecTime[periods.size()];
		double[] sums = new double[periods.size()];
		double[] copySums = new double[periods.size()];
		int[] numbersOfValues = new int[periods.size()];
		String[][] dataGroup = new String[periods.size()][];

		int index = 0;
		int month = -1;
		int year = -1;

		HashMap<Integer, Object> DssFiles;
		if (isSelectedFiles) {
			DssFiles = _DssFiles;
		} else {
			DssFiles = _unselectedDssFiles;
		}

		Enumeration<String> periodEnum = periods.elements();
		// int periodIndex = 0;
		while (periodEnum.hasMoreElements()) {
			if (MainPanel.STOP_DSSVIEWER_METHODS) {
				return false;
			}
			String period = periodEnum.nextElement();
			HecTime startDate = new HecTime();
			HecTime endDate = new HecTime();
			if (period.indexOf("-") == -1) {
				break; // not a multi-month period, so go to next period
			} else { // multiple-month time window
				terms[index] = period;
				if (_allVariableTWData.get(period) == null) {
					results[index] = new Hashtable<String, String[]>(); // for
					// one
					// time
					// window
					// (term)
					// TODO
					// - set
					// size
					// based
					// on
					// _allVariableData[0]
					// size
				} else {
					results[index] = _allVariableTWData.get(period);
				}
				String[] split = period.split(" +");

				startDate.setDate(split[0]); // CB TODO comment out?
				startDate.setTime("2400");
				month = startDate.month() + 1;
				year = startDate.year();
				if (month == 13) {
					year = startDate.year() + 1;
				}
				String monthName = DssViewer.months[(month - 1) % 12];
				startDate.setDate(monthName + year);
				startDate.add(-1440);
				startTimes[index] = startDate;

				endDate.setDate(split[2]); // CB TODO comment out?
				endDate.setTime("2400");
				month = endDate.month() + 1;
				year = endDate.year();
				if (month == 13) {
					year = startDate.year() + 1;
				}
				monthName = DssViewer.months[(month - 1) % 12];
				endDate.setDate(monthName + year);
				endDate.add(-1440);
				endTimes[index++] = endDate;
			}
			// CB added block for progress bar in the Schematic - TODO this is
			// better than doing it repeatedly in while
			// (variableEnum.hasMoreElements()) block if OK
			/*
			 * numPeriods = 12 * (endDate.year() - startDate.year()) +
			 * (endDate.month() - startDate.month() + 1); ++periodIndex;
			 * System.out.println(numPeriods); totalPeriods += numPeriods;
			 */
		}
		Enumeration<String> variableEnum = _variables.keys();
		boolean isCalculatedConversions = false;
		boolean setCalculatedConversionsTrue = true;
		HecTime hecStartTime = new HecTime();
		HecTime hecEndTime = new HecTime();
		HecTime ht = new HecTime();
		int numberOfVariables = _variables.size(); // CB added for long-term
		// average progress bar in
		// the Schematic
		int variableCount = 0;
		int newPercentage = 0;
		int oldPercentage = 0;
		float factor = 0f;
		Set<Integer> keys = _Fparts.keySet(); // TODO NO? - do not need? WHAT IF
		// ONE FILE LINE IS
		// BLANK??????????????????????????????
		Iterator<Integer> keyIterator = null;
		Integer key = null;
		// Remove all other keys when a dv or sv file is opened
		if (dssType > -1) { // ????????????????????????????????????????
			// _DssFiles = dssFileNames; //Need to reset because a file
			// selection or
			// deselection was done (but we leave data in on deselection for
			// speed)
			Vector<Integer> keysToRemove = new Vector<Integer>();
			keyIterator = keys.iterator();

			while (keyIterator.hasNext()) {
				key = keyIterator.next();
				if (((Integer) key) != dssType / 4) {
					keysToRemove.add(key);
				}
			}
			Enumeration<Integer> keysToRemoveEnum = keysToRemove.elements();
			while (keysToRemoveEnum.hasMoreElements()) {
				keys.remove(keysToRemoveEnum.nextElement());
			}
		}
		int numberOfSelectedStudies = _DssFiles.size(); // CB added for updating
		// monthly progress bar
		int numberOfUnselectedStudies = _unselectedDssFiles.size(); // CB added
		// for
		// updating
		// monthly
		// progress
		// bar
		if (numberOfSelectedStudies + numberOfUnselectedStudies == 0) {
			return true;
		}
		if (isSelectedFiles) {
			oldPercentage = 0;
			factor = (float) numberOfSelectedStudies * 100
					/ (numberOfSelectedStudies + numberOfUnselectedStudies)
					/ _variables.size();
		} else {
			oldPercentage = (100 * numberOfSelectedStudies / (numberOfSelectedStudies + numberOfUnselectedStudies));
			factor = (float) numberOfUnselectedStudies * 100
					/ (numberOfSelectedStudies + numberOfUnselectedStudies)
					/ _variables.size();
		}
		float startPercentage = oldPercentage;
		while (variableEnum.hasMoreElements()) {
			String name = variableEnum.nextElement();
			++variableCount;
			if (dssType > -1) {
				System.out.print("");
			}
			if ((newPercentage = (int) (variableCount * factor + startPercentage)) > oldPercentage) {
				oldPercentage = newPercentage;
				// update Schematic's long-term average progress bar
				SchematicUtils.getSchematic()
						.setLongtermProgress(newPercentage);
				// System.out.println(newPercentage);
			}
			for (int j = 0; j < _allVariableData.length; ++j) { // CB TODO use
				// the keys to
				// only consider
				// the files
				// type
				// (selected OR
				// unselected)
				// TODO check the current files .get(j) first and continue if
				// not here
				if (DssFiles.get(j) == null) {
					continue;
				}
				if (MainPanel.STOP_DSSVIEWER_METHODS) {
					return false;
				}
				if (_allVariableData[j].get(name) != null) {
					HecMath dataSet = _allVariableData[j].get(name);
					HecMath dataSetCopy = null; // to calculate conversion
					// factor for the term
					TimeSeriesContainer tscOfCopy = null; // to calculate
					// conversion factor
					// for the term
					boolean isTAFSelected = (_units.equalsIgnoreCase("taf") ? true
							: false); // TODO: added method so use it
					try {
						if (!isCalculatedConversions) {
							dataSetCopy = dataSet.copy();
						}
						dataSet = unitsConversion(dataSet, isTAFSelected);
						TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet
								.getData(); // will contain only one value
						// HecTime hecStartTime = new HecTime(); //sped it up
						hecStartTime.set(tsc.startTime);
						// HecTime hecEndTime = new HecTime(); //sped it up
						hecEndTime.set(tsc.endTime);
						// HecTime ht = new HecTime(); //sped it up
						ht.setDate(hecStartTime.date());
						ht.setTime(hecStartTime.time());
						if (!isCalculatedConversions) {
							tscOfCopy = (TimeSeriesContainer) dataSetCopy
									.getData(); // will contain only one value
						}
						if (!isCalculatedConversions) {
							if (tsc.units.equals(tscOfCopy.units)) {
								if (tscOfCopy.units.equalsIgnoreCase("CFS")) {
									dataSetCopy = unitsConversion(dataSetCopy,
											true);
									tscOfCopy = (TimeSeriesContainer) dataSetCopy
											.getData();
								} else if (tscOfCopy.units
										.equalsIgnoreCase("TAF")) {
									dataSetCopy = unitsConversion(dataSetCopy,
											false);
									tscOfCopy = (TimeSeriesContainer) dataSetCopy
											.getData();
								}
							}
						}
						for (int i = 0; i < periods.size(); ++i) {
							sums[i] = 0;
							copySums[i] = 0;
							numbersOfValues[i] = 0;
							dataGroup[i] = new String[_allVariableData.length];
						}
						int valueIndex = 0;
						while (ht.compareTimes(hecEndTime) <= 0) { // can do
							// just once
							// above?????
							for (int i = 0; i < periods.size(); ++i) {
								if ((ht.compareTimes(startTimes[i]) >= 0)
										&& (ht.compareTimes(endTimes[i]) <= 0)) {
									// System.out.println("hecStartTime = " +
									// hecStartTime);
									if (i == 0) {
										valueIndex = (ht.year() - hecStartTime
												.year())
												* 12
												+ (ht.month() - hecStartTime
														.month()); // offset
										// (index)
										// from the
										// startdate
									}
									sums[i] += tsc.values[valueIndex];
									if (!isCalculatedConversions) {
										copySums[i] += tscOfCopy.values[valueIndex];
									}
									++numbersOfValues[i];
								}
							}
							// CB TODO: This block would be much easier if I
							// knew how to add 1 month exactly to an HecTime!!!!
							month = -1;
							if (ht.day() > 1) {
								month = ht.month() + 2;
								if (month >= 13) {
									year = ht.year() + 1;
								} else {
									year = ht.year();
								}
								ht.setDate(DssViewer.months[(month - 1) % 12]
										+ year);
							} else {
								month = ht.month() + 1;
								if (month == 13) {
									year = ht.year() + 1;
								} else {
									year = ht.year();
								}
								ht.setDate(DssViewer.months[(month - 1) % 12]
										+ year);
							}
							ht.add(-1440); // 86400 advanced ht from October 31,
							// 1921 at 2400 to December 30, 1921
							// at 2400.
						}

						if (numbersOfValues[0] < 984) {
							System.out.println("For variable = "
									+ name
									+ " number of values = " // debugging
									+ numbersOfValues[0]
									+ " which is less than 984");
						}

						if (!isCalculatedConversions) {
							for (int i = 0; i < periods.size(); ++i) {
								if (tscOfCopy.units.equalsIgnoreCase("CFS")) {
									_longTermTafToCfsConversionFactors.put(
											terms[i], copySums[i] / sums[i]);
									setCalculatedConversionsTrue = true;
								} else if (tscOfCopy.units
										.equalsIgnoreCase("TAF")) {
									_longTermTafToCfsConversionFactors.put(
											terms[i], sums[i] / copySums[i]);
									setCalculatedConversionsTrue = true;
								}
							}
						}
						String suffix = null;
						// if (name.equals("S_SHSTA"))
						// System.out.print("");
						for (int i = 0; i < periods.size(); ++i) {
							try {
								if (i == 0) {
									suffix = " " + dataSet.getUnits();
								}
								if (results[i].get(name) != null) {
									dataGroup[i] = results[i].get(name);
								}
								dataGroup[i][j] = sums[i] / numbersOfValues[i]
										+ suffix; // j is 0 for alt. 1 OR 1 for
								// alt. 2
								// if (results[i].get(name) == null) { // 19 +
								// seconds for entire method with this line (at
								// home)
								// if (j == DssFiles.size() - 1) { // 19 +
								// seconds for entire method with this line (at
								// home)
								// if (results[i].get(name) == null) { //TODO
								// put it if the last
								// i????????????????????????????????????????????????????
								// if (i == periods.size() - 1)
								results[i].put(name, dataGroup[i]);
							} catch (NumberFormatException nfe) {
								// TODO do anything?
							} catch (NullPointerException npe) {
								npe.printStackTrace();
								return false;
							}
						}
						// if (name.equals("S_SHSTA")) {
						// String[] shasta0results = results[0].get(name);
						// String[] shasta1results = results[1].get(name);
						// String[] shasta2results = results[2].get(name);
						// String[] shasta3results = results[3].get(name);
						// System.out.print("");
						// }
						if (setCalculatedConversionsTrue) {
							isCalculatedConversions = true;
							setCalculatedConversionsTrue = false;
						}
					} catch (HecMathException hme) {
						hme.printStackTrace();
					}
				} else {
				}
			}
		}
		for (int i = 0; i < periods.size(); ++i) {
			_allVariableTWData.put(terms[i], results[i]);
		}
		// String[] shastaResults =
		// _allVariableTWData.get(terms[0]).get("S_SHSTA");
		long stopTime = System.currentTimeMillis();
		if (isSelectedFiles) {
			System.out
					.println("Selected Files long term averages calculation time = "
							+ (stopTime - startTime));
		} else {
			System.out
					.println("Unselected Files long term averages calculation time = "
							+ (stopTime - startTime));
		}
		return true;
	}

	/**
	 * CB added to load long-term averages in background This should be called
	 * from within <code>SwingUtilities.invokeLater</code>.
	 */
	/*
	 * public void calculateLongTermAverages_firsttry(Vector<String> periods) {
	 * _initialUnits = _units; _longTermTafToCfsConversionFactors = new
	 * Hashtable<String, Double>(); _allVariableTWData = new Hashtable<String,
	 * Hashtable<String, String[]>>(); //for time window periods //
	 * Hashtable<String, String[]> results = new
	 * Hashtable[_allVariableData.length]; // for one time window!
	 * Hashtable<String, String[]> results = new Hashtable<String, String[]>();
	 * // for one time window! // for (int i = 0; i < _allVariableData.length;
	 * ++i) // results[i] = new Hashtable<String, String>();
	 * 
	 * // for (int i = 0; i < _allVariableTWData.size(); ++i) { //
	 * _allVariableTWData.put(MessagePanel._twSelections[i], new
	 * Hashtable<String, HecMath>()); // } Enumeration<String> variableEnum =
	 * _variables.keys(); boolean isCalculatedConversions = false; boolean
	 * setCalculatedConversionsTrue = true; while
	 * (variableEnum.hasMoreElements()) { String name =
	 * variableEnum.nextElement(); String[] dataPair = new
	 * String[_allVariableData.length]; for (int j = 0; j <
	 * _allVariableData.length; ++j) { if (_allVariableData[j].get(name) !=
	 * null) { HecMath dataSet = (HecMath)_allVariableData[j].get(name); HecMath
	 * dataSetCopy = null; //to calculate conversion factor for the term
	 * TimeSeriesContainer tscOfCopy = null; //to calculate conversion factor
	 * for the term boolean isTAFSelected = (_units.equalsIgnoreCase("taf") ?
	 * true : false); try { if (!isCalculatedConversions) dataSetCopy =
	 * dataSet.copy(); dataSet = unitsConversion(dataSet, isTAFSelected);
	 * TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet.getData(); //
	 * will contain only one value if (!isCalculatedConversions) tscOfCopy =
	 * (TimeSeriesContainer) dataSetCopy.getData(); // will contain only one
	 * value if (!isCalculatedConversions) { if
	 * (tsc.units.equals(tscOfCopy.units)) { if
	 * (tscOfCopy.units.equalsIgnoreCase("CFS")) { dataSetCopy =
	 * unitsConversion(dataSetCopy, true); tscOfCopy = (TimeSeriesContainer)
	 * dataSetCopy.getData(); } else if
	 * (tscOfCopy.units.equalsIgnoreCase("TAF")) { dataSetCopy =
	 * unitsConversion(dataSetCopy, false); tscOfCopy = (TimeSeriesContainer)
	 * dataSetCopy.getData(); } } }
	 * 
	 * // tsc.precision = 0; //CB does not appear to work HecTime hecStartTime =
	 * new HecTime(); hecStartTime.set(tsc.startTime); HecTime ht = new
	 * HecTime(); Enumeration<String> periodEnum = periods.elements(); while
	 * (periodEnum.hasMoreElements()) { String period =
	 * periodEnum.nextElement(); if (period.indexOf("-") == -1) { break; // not
	 * a multi-month period, so go to next period } else { // multiple-month
	 * time window String[] split = period.split(" +"); String startMonth =
	 * split[0].substring(0, 3); String endMonth = split[2].substring(0, 3);
	 * String startYear = split[0].substring(3); String endYear =
	 * split[2].substring(3); int startMonthIndex = -1; int endMonthIndex = -1;
	 * for (int i = 0; i < DssViewer.months.length; ++i) { if
	 * (DssViewer.months[i].equalsIgnoreCase(startMonth)) startMonthIndex = i;
	 * if (DssViewer.months[i].equalsIgnoreCase(endMonth)) endMonthIndex = i; }
	 * System.out.print(""); try { int sYear = Integer.parseInt(startYear); int
	 * eYear = Integer.parseInt(endYear); int numValues = 0; double sum = 0.0;
	 * double sumOfCopy = 0.0; String date = ""; int eMonth = -1; for (int year
	 * = sYear; year <= eYear; ++year) { if (year != sYear) startMonthIndex = 0;
	 * if (year != eYear) eMonth = 11; else eMonth = endMonthIndex; for (int
	 * month = startMonthIndex; month <= eMonth; ++month) { date =
	 * DssViewer.months[month] + String.valueOf(year); ht.setDate(date); int
	 * valueIndex = (ht.year() - hecStartTime.year()) * 12 + (ht.month() -
	 * hecStartTime.month()); //offset (index) from the startdate sum +=
	 * tsc.values[valueIndex]; if (!isCalculatedConversions) sumOfCopy +=
	 * tscOfCopy.values[valueIndex]; ++numValues; } } if
	 * (!isCalculatedConversions) { if (tscOfCopy.units.equalsIgnoreCase("CFS"))
	 * { _longTermTafToCfsConversionFactors.put(period, sumOfCopy/sum);
	 * setCalculatedConversionsTrue = true; } else if
	 * (tscOfCopy.units.equalsIgnoreCase("TAF")) {
	 * _longTermTafToCfsConversionFactors.put(period, sum/sumOfCopy);
	 * setCalculatedConversionsTrue = true; } } // results[j].put(name,
	 * sum/numValues + " " + dataSet.getUnits()); //j is 0 for alt. 1 OR 1 for
	 * alt. 2 dataPair[j] = sum/numValues + " " + dataSet.getUnits(); //j is 0
	 * for alt. 1 OR 1 for alt. 2
	 * 
	 * if (j == _allVariableData.length - 1) { //only need to add once
	 * results.put(name, dataPair); _allVariableTWData.put(period, results); }
	 * 
	 * } catch (NumberFormatException nfe) { //TODO do anything? } } } if
	 * (setCalculatedConversionsTrue) { isCalculatedConversions = true;
	 * setCalculatedConversionsTrue = false; } } catch (HecMathException hme){
	 * hme.printStackTrace(); } } else { } }
	 * 
	 * } }
	 */
	/**
	 * CB added.
	 * 
	 * @param studyNumber
	 */
	/*
	 * CB void loadVariableData(int dssType, HashMap<Integer, Object>
	 * dssFileNames, boolean selectedFiles) { if (!_keepVariableData) return;
	 * //CB TO DO: need this variable and statement????
	 * generateVariableData(dssType, dssFileNames, selectedFiles);
	 * System.out.print(""); }
	 */

	/**
	 * CB added.
	 * 
	 * @param units
	 * @param studyNumber
	 */
	boolean loadVariableData(String units, int dssType, boolean selectedFiles) {
		_units = units; // TO DO: set _units using listener on the units swing
		// component?
		// if (!_keepVariableData) return; //CB TO DO: need this variable and
		// statement????
		return generateVariableData(dssType, selectedFiles);
	}

	/**
	 * CB added.
	 */
	private boolean generateVariableData(int dssType, boolean isSelectedFiles) { // CB
		// changed
		long startTime = System.currentTimeMillis();
		if (_paths == null) {
			JOptionPane.showMessageDialog(null, "The paths variable is null",
					"NULL paths!", JOptionPane.ERROR_MESSAGE);
		} else { // TODO
			if (_allVariableData == null) {
				_allVariableData = new Hashtable[4]; // CB TODO use Java
				// constant instead
				for (int i = 0; i < _allVariableData.length; ++i) {
					_allVariableData[i] = new Hashtable<String, HecMath>();
				}
			}
			HashMap<Integer, Object> DssFiles;
			if (isSelectedFiles) {
				DssFiles = _DssFiles;
			} else {
				DssFiles = _unselectedDssFiles;
			}
			String thePath;
			try {
				if (MainPanel.STOP_DSSVIEWER_METHODS) {
					return false;
				}
				DSSFile theFile = null;
				// Transfer base file(s) F-part(s) to a String array of size 1
				// or 2 for use later
				String[] baseF = null; // CB added capability of more than one
				// fpart for each key
				if (_Fparts.get(FilterPanel.BASE_DV_FILE_INT / 4) instanceof Vector) { // CB
					baseF = new String[((Vector<String>) _Fparts
							.get(FilterPanel.BASE_DV_FILE_INT / 4)).size()];
					for (int i = 0; i < baseF.length; ++i) {
						baseF[i] = ((Vector<String>) _Fparts
								.get(FilterPanel.BASE_DV_FILE_INT / 4)).get(i); // CB
					}
				} else { // it is a String
					baseF = new String[1]; // CB 5/2008 bug fix - was missing
					baseF[0] = (String) _Fparts
							.get(FilterPanel.BASE_DV_FILE_INT / 4); // CB
				}
				String F = null;
				HecMath dataSet = new TimeSeriesMath();
				// CB TODO replace constant (4) everywhere it occurs for dssType
				// with a constant final int # of studies (in MainPanel?)
				int allDataIndex = -1;

				Set<Integer> keys = _Fparts.keySet();
				Iterator<Integer> keyIterator = null;
				Integer key = null;
				// Remove all other keys when a dv or sv file is opened
				if (dssType > -1) {
					// _DssFiles = dssFileNames; //Need to reset because a file
					// selection or deselection was done (but we leave data in
					// on deselection for speed)
					Vector<Integer> keysToRemove = new Vector<Integer>();
					keyIterator = keys.iterator();

					while (keyIterator.hasNext()) {
						key = keyIterator.next();
						if (((Integer) key) != dssType / 4) {
							keysToRemove.add(key);
						}
					}
					Enumeration<Integer> keysToRemoveEnum = keysToRemove
							.elements();
					while (keysToRemoveEnum.hasMoreElements()) {
						keys.remove(keysToRemoveEnum.nextElement());
					}
				}
				keyIterator = keys.iterator();
				int numberOfSelectedFiles = 0; // CB added for updating monthly
				// progress bar
				int numberOfUnselectedFiles = 0; // CB added for updating
				// monthly progress bar
				int fileCount = 0; // CB added for updating monthly progress bar
				// int numberOfPaths = _paths.size(); //CB added for updating
				// monthly progress bar
				int pathAccesses = 0; // CB added for updating monthly progress
				// bar - "accesses" means accesses +
				// skips
				int newPercentage = 0; // CB added for updating monthly progress
				// bar
				int oldPercentage; // CB added for updating monthly progress bar
				// Count the number of selected files for use in updating the
				// progress bar
				while (keyIterator.hasNext()) {
					key = keyIterator.next();
					if (_DssFiles.get(key) instanceof Vector) { // TODO need
						// null check
						// first here?
						numberOfSelectedFiles += ((Vector) _DssFiles.get(key))
								.size();
					} else if (_DssFiles.get(key) != null) {
						++numberOfSelectedFiles; // CB TODO use numberOfFiles
						// for _unselectedDssFiles
						// too.
					}
				}
				// Count the number of unselected files for use in updating the
				// progress bar
				keyIterator = keys.iterator(); // CB reset the iterator; added
				// for updating monthly progress
				// bar
				while (keyIterator.hasNext()) {
					key = keyIterator.next();
					if (_unselectedDssFiles.get(key) instanceof Vector) {
						numberOfUnselectedFiles += ((Vector) _unselectedDssFiles
								.get(key)).size();
					} else if (_unselectedDssFiles.get(key) != null) {
						++numberOfUnselectedFiles; // CB TODO use numberOfFiles
						// for _unselectedDssFiles
						// too.
					}
				}
				// Because this method is called from both selected files and
				// unselected files threads,
				// and the selected files thread is first, // the initial "old"
				// percentage for the
				// unselected files thread must be figured from the last
				// percentage reached during the
				// selected files thread.
				if (isSelectedFiles) {
					oldPercentage = 0;
				} else {
					oldPercentage = (numberOfSelectedFiles / (numberOfSelectedFiles + numberOfUnselectedFiles));
				}
				keyIterator = keys.iterator(); // CB reset the iterator; added
				// for updating monthly progress
				// bar
				while (keyIterator.hasNext()) {
					key = keyIterator.next();
					allDataIndex = key;
					if ((DssFiles == null)
							|| !DssFiles.containsKey(allDataIndex)) {
						continue;
					}
					Vector<String> files = null;
					if (DssFiles.get(key) instanceof String) {
						files = new Vector<String>();
						if (!((String) DssFiles.get(key)).trim().equals("")) {
							files.add((String) DssFiles.get(key));
						}
					} else if (DssFiles.get(String.valueOf(allDataIndex)) instanceof String) {
						files = new Vector<String>();
						files.add((String) DssFiles.get(String
								.valueOf(allDataIndex)));
					} else {
						if (DssFiles.get(key) instanceof Vector) {
							files = (Vector<String>) DssFiles.get(key);
						}
					}
					if (files == null) {
						continue;
					}
					Enumeration<String> fileElements = files.elements(); // CB
					// added
					// files
					// is
					// null
					// now
					// if
					// row
					// is
					// unchecked!!!!!
					int fileElementCount = -1; // CB added
					float percentFilesDone = 0f;
					float factor = 100f
							/ (numberOfSelectedFiles + numberOfUnselectedFiles)
							/ _paths.size();
					while (fileElements.hasMoreElements()) {
						if (MainPanel.STOP_DSSVIEWER_METHODS) {
							return false;
						}
						++fileElementCount; // CB added
						if (dssType > -1) {
							System.out.print("");
						}
						// Was trying to optimize, but not sucessful and had to
						// wrap it up quickly (for now).
						// if (dssType > -1 && files.size() == 2 &&
						// (fileElementCount != dssType % 4)) { //CB added check
						// for dssType arg being even or odd to reduce one file
						// from processing
						// // if (dssType > -1 && files.size() == 2 &&
						// (fileElementCount == 0)) { //CB added check for
						// dssType arg being even or odd to reduce one file from
						// processing
						// if (dssType > -1 && (fileElementCount == 1 && dssType
						// % 4 == 1)) { //CB to reduce one file from processing
						// (IFF dv and sv paths are separate)
						// // fileElements.nextElement(); //skip the current
						// file
						// // factor *= 2;
						// // continue;
						// / } else {
						// ++fileCount;
						// // }
						++fileCount;
						theFile = DSS.open(fileElements.nextElement(),
								_timewindow); // CB added
						if (MainPanel.STOP_DSSVIEWER_METHODS) {
							return false;
						}
						// CB TODO use isSelected and numberOfSelectedFiles and
						// numberOfUnselectedFiles to calculate newpercentage
						if (isSelectedFiles) {
							percentFilesDone = 100
									* ((float) fileCount - 1)
									/ (numberOfSelectedFiles + numberOfUnselectedFiles);
						} else {
							percentFilesDone = 100
									* ((float) (numberOfSelectedFiles
											+ fileCount - 1))
									/ (numberOfSelectedFiles + numberOfUnselectedFiles);
						}
						pathAccesses = 0;
						for (int i = 0; i < _paths.size(); i++) { // CB _paths
							// size is
							// the
							// number of
							// variables
							// for which
							// data is
							// to be
							// retrieved.
							thePath = _paths.get(i);
							++pathAccesses;
							// if (pathAccesses == 1222) //debugged - looked for
							// SVs
							// System.out.print("");
							// only update the progress bar if the percentage
							// that should be shown is different from that shown
							if ((newPercentage = (int) (pathAccesses * factor + percentFilesDone)) > oldPercentage) {
								oldPercentage = newPercentage;
								// update Schematic's long-term average progress
								// bar
								SchematicUtils.getSchematic()
										.setMonthlyProgress(newPercentage);
							} else if ((oldPercentage == 99)
									&& (i == _paths.size() - 1)) {
								SchematicUtils.getSchematic()
										.setMonthlyProgress(100);
							}
							boolean wasFoundInFile = false;
							for (int k = 0; k < baseF.length; ++k) {
								if (MainPanel.STOP_DSSVIEWER_METHODS) {
									return false;
								}
								if (_Fparts.get(key) instanceof String) { // CB
									// added
									// the
									// check
									F = (String) _Fparts.get(key);
								} else { // must be a Vector, Victor
									Enumeration<String> fpartenum = ((Vector<String>) _Fparts
											.get(key)).elements();
									int fpartenumCount = -1;
									while (fpartenum.hasMoreElements()) {
										fpartenumCount++;
										if (fileElementCount == fpartenumCount) {
											F = fpartenum.nextElement();
											break;
										}
									}
								}
								if (F == null) {
									continue;
								}
								int index1 = thePath.indexOf('/');
								int index2 = thePath.indexOf('/', index1 + 1);
								int index3 = thePath.indexOf('/', index2 + 1);
								// if variable not in the dv file, look in the
								// sv file for it
								// if
								// (_allVariableData[allDataIndex].get(thePath.substring(index2
								// + 1, index3)) == null) { // NO, may need to
								// overwrite
								if (!baseF[k].equalsIgnoreCase(F)) {
									thePath = thePath.replace(baseF[k], F); // CB
									// TO
									// DO:
									// ALTER
									// FOR
									// WRIMS
									// 2
								}
								try {
									dataSet = theFile.read(thePath); // CB TO
									// DO:
									// check
									// the
									// F-parts
									// of
									// the
									// SV
									// file
									// paths
									// units conversAtion CFS->TAF
									dataSet = unitsConversion(dataSet, false);
									_allVariableData[allDataIndex].put(thePath
											.substring(index2 + 1, index3),
											dataSet);
									wasFoundInFile = true; // CB added
								} catch (DSSFileException e) {
									// path not found probably, message from
									// elsewhere; continue with the rest so do
									// nothing
								}
								if (wasFoundInFile) {
									break;
									// }
								}
							}
						}
						theFile.close();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		long stopTime = System.currentTimeMillis();
		if (isSelectedFiles) {
			System.out.println("Selected Files monthly data loading time = "
					+ (stopTime - startTime));
		} else {
			System.out.println("Unselected Files monthly data loading time = "
					+ (stopTime - startTime));
		}
		return true;
	}

	/**
	 * CB added to increase code reusability
	 */
	public G2dDialog plot(String title, int type) {
		G2dDialog dialog = loadG2d(title, type);
		dialog.initPlot();
		dialog.setVisible(true);
		return dialog;
	}

	/**
	 * CB added to increase code reusability
	 */
	public MonthlyTableFrame monthly(String title) {
		MonthlyTableFrame frame = loadMonthlyTable(title, _annualTypeMode); // CB
		// TODO:
		// add
		// checkbox
		// check;
		// replace
		// boolean
		// with
		// constant;
		// add
		// contract
		// year
		frame.getTable().setupModel();
		frame.showTable();
		return frame;
	}

	/**
	 * CB added to increase code reusability
	 */
	public HecDataTableFrame tabulate(String title) {
		HecDataTableFrame frame = loadTable(title);
		frame.showTable(); // CB added
		// CB - added; needs listeners added? (extend or use G2dDialog and
		// TableFrame?)
		frame.setCommasState(getTableCommaPreference());
		return frame;
	}

	/**
	 * CB added to increase code reusability
	 */
	public JFrame[] allOutputs(String title) {
		JFrame[] frames = new JFrame[3];
		frames[0] = plot(title, PLOT);
		frames[1] = monthly(title);
		frames[2] = tabulate(title);
		return frames;
	}

	public String getPlotTitle(boolean exceedence, boolean isAnnualTot,
			boolean isMonthlyAverage) {
		int wyMonthOrder[] = { 9, 10, 11, 12, 1, 2, 3, 4, 5, 6, 7, 8 };
		String title = "";
		if (_mode.equals("Comp")) {
			title += "Compare ";
		} else {
			title += "Difference ";
		}

		if (exceedence) {
			title += "Exceedence ";
		}

		if (isAnnualTot) {
			if (_annualTypeMode == MessagePanel.WATERYEAR) {
				title += "Wateryear Total ";
			} else if (_annualTypeMode == MessagePanel.CALENDAR_YEAR) {
				title += "Calendar Total ";
			} else if (_annualTypeMode == MessagePanel.FEDERAL_CONTRACT_YEAR) {
				title += "Federal Contract Year Total ";
			}
		}

		if (isMonthlyAverage) {
			title += "Monthly Averages ";
		}

		if (_months.size() == 12) {
			if (_annualTypeMode == MessagePanel.WATERYEAR) {
				title += "(Oct-Sep)";
			} else if (_annualTypeMode == MessagePanel.CALENDAR_YEAR) {
				title += "(Jan-Dec)";
			} else if (_annualTypeMode == MessagePanel.FEDERAL_CONTRACT_YEAR) {
				title += "(Mar-Feb)";
			}
			return title;
		}

		title += "(";

		int mo;
		if (_annualTypeMode == MessagePanel.WATERYEAR) { // CB added annual type
			// stuff
			for (int element : wyMonthOrder) {
				mo = element;
				if (_months.contains(mo)) {
					title += months[mo - 1] + ",";
				}
			}
		} else if (_annualTypeMode == MessagePanel.CALENDAR_YEAR) {
			for (int element : wyMonthOrder) {
				mo = element - 8;
				if (mo < 1) {
					mo += 12;
				}
				if (_months.contains(mo)) {
					title += months[mo - 1] + ",";
				}
			}
		} else if (_annualTypeMode == MessagePanel.FEDERAL_CONTRACT_YEAR) {
			for (int element : wyMonthOrder) {
				mo = element - 6;
				if (mo < 1) {
					mo += 12;
				}
				if (_months.contains(mo)) {
					title += months[mo - 1] + ",";
				}
			}
		}

		title = title.substring(0, title.length() - 1);
		title += ")";

		return title;
	}

	// CB G2dDialog loadG2d(String title, boolean isExceedence, boolean
	// isAnnualTotal) {
	/*
	 * G2dDialog loadG2d_originalversion(String title, int type) { //CB String
	 * thePath = null; //CB String keys[] = {"Base", "1", "2", "3"}; G2dDialog
	 * thePlot = null;
	 * 
	 * DSSFile theFile = null; thePlot = Plot.newPlot(title); //CB String baseF
	 * = (String) _Fparts.get("Base"); String[] baseF = null; //CB added
	 * capability of more than one f-part for each key if (_Fparts.get("Base")
	 * instanceof Vector) { baseF = new
	 * String[((Vector<String>)_Fparts.get("Base")).size()]; for (int i = 0; i <
	 * baseF.length; ++i) { baseF[i] =
	 * (String)((Vector<String>)_Fparts.get("Base")).get(i); } } else { // it is
	 * a String baseF = new String[1]; //CB 5/2008 bug fix - was missing
	 * baseF[0] = (String)_Fparts.get("Base"); } String F = null; HecMath
	 * baseSet = null; HecMath dataSet = null; try { for (int j = 0; j <
	 * _keys.length; j++) { if (!_DssFiles.containsKey(_keys[j])) continue;
	 * Vector<String> files = null; //CB added check for multiple files (Vector)
	 * option if (_DssFiles.get(_keys[j]) instanceof String) { files = new
	 * Vector<String>(); if
	 * (!((String)_DssFiles.get(_keys[j])).trim().equals("")) //CB 5/2008 added
	 * check to prevent empty String file name!
	 * files.add((String)_DssFiles.get(_keys[j])); } else { if
	 * (_DssFiles.get(_keys[j]) instanceof Vector) files =
	 * (Vector<String>)_DssFiles.get(_keys[j]); //TO DO: check if instanceof? }
	 * Enumeration<String> fileElements = files.elements(); //CB added int
	 * fileElementCount = 0; //CB added while (fileElements.hasMoreElements()) {
	 * //CB theFile = DSS.open((String) _DssFiles.get(keys[j]), _timewindow);
	 * theFile = DSS.open((String) fileElements.nextElement(), _timewindow);
	 * //CB fileElementCount++; //CB added for (int i = 0; i < _paths.size();
	 * i++) { thePath = (String) _paths.get(i); boolean wasFoundInFile = false;
	 * for (int k = 0; k < baseF.length; ++k) { //CB if (_Fparts.get(_keys[j])
	 * instanceof String) { //CB added the check //
	 * System.out.println("String fpart = " + (String) _Fparts.get(keys[j]));
	 * //CB debugging F = (String) _Fparts.get(_keys[j]); } else { //CB added
	 * section; it must be a Vector Enumeration<String> fpartenum =
	 * ((Vector<String>)_Fparts.get(_keys[j])).elements(); int fpartenumCount =
	 * 0; while (fpartenum.hasMoreElements()) { fpartenumCount++; if
	 * (fileElementCount == fpartenumCount) { F =
	 * (String)fpartenum.nextElement(); break; } } } //CB F = (String)
	 * _Fparts.get(keys[j]); thePath = thePath.replace(baseF[k], F); try { if
	 * (_mode.equals("Diff")) { if (_keys[j].equals("Base")) { baseSet =
	 * theFile.read(thePath); continue; } dataSet = theFile.read(thePath);
	 * wasFoundInFile = true; dataSet = dataSet.subtract(baseSet); } else {
	 * dataSet = theFile.read(thePath); wasFoundInFile = true; }
	 * 
	 * // units conversion CFS->TAF //CB if (isAnnualTotal) { // to force the
	 * units as TAF or not if (type == ANNUAL_TOTAL || type ==
	 * ANNUAL_TOTAL_EXCEEDENCE) { //CB boolean forceTAF = true; dataSet =
	 * unitsConversion(dataSet, forceTAF); } else { String [] parts =
	 * thePath.split("/"); if (parts[3].toLowerCase().indexOf("storage") > -1)
	 * dataSet = unitsConversion(dataSet, true); else dataSet =
	 * unitsConversion(dataSet, false); }
	 * 
	 * //CB if (isAnnualTotal && isExceedence) { if (type ==
	 * ANNUAL_TOTAL_EXCEEDENCE) { //CB if (_months.size() < 12)
	 * thePlot.addData(getExceedence(getAnnualTotals(getMonthlyData(dataSet,
	 * _months)))); else thePlot.addData(getExceedence(getAnnualTotals
	 * ((TimeSeriesContainer)dataSet.getData())));
	 * 
	 * //CB } else if (isExceedence) { } else if (type == EXCEEDENCE) { //CB if
	 * (_months.size() < 12)
	 * thePlot.addData(getExceedence(getMonthlyData(dataSet, _months))); else
	 * thePlot.addData(getExceedence((TimeSeriesContainer) dataSet.getData()));
	 * 
	 * //CB } else if (isAnnualTotal) { } else if (type == ANNUAL_TOTAL) { //CB
	 * if (_months.size() < 12)
	 * thePlot.addData(getAnnualTotals(getMonthlyData(dataSet, _months))); else
	 * thePlot.addData(getAnnualTotals((TimeSeriesContainer)dataSet
	 * .getData())); } else if (type == MONTHLY_AVERAGE) { //CB added section if
	 * (_months.size() < 12)
	 * thePlot.addData(getMonthlyAverages(getMonthlyData(dataSet, _months)));
	 * else thePlot.addData(getMonthlyAverages((TimeSeriesContainer)dataSet
	 * .getData())); } else { if (_months.size() < 12)
	 * thePlot.addData(getMonthlyData(dataSet, _months)); else
	 * thePlot.addData(dataSet.getData()); } // if (dataSet != null) break; }
	 * catch (DSSFileException dssFE) { // Do nothing as we don't want one bad
	 * path to kick us out. } if (wasFoundInFile) break; } } theFile.close(); }
	 * } } catch (Exception e) { e.printStackTrace(); } return thePlot; }
	 */
	// CB G2dDialog loadG2d(String title, boolean isExceedence, boolean
	// isAnnualTotal) {
	G2dDialog loadG2d(String title, int type) { // CB
		G2dDialog thePlot = Plot.newPlot(title);
		// CB String thePath = null;
		// CB String keys[] = { "Base", "1", "2", "3" };
		DSSFile theFile = null;
		Vector<DSSFile> theFilePair = null; // CB added
		// CB String baseF = (String) _Fparts.get("Base");
		String[] baseF = null; // CB added capability of more than one fpart for
		// each key
		// CB if (_Fparts.get("Base") instanceof Vector) {
		if (_Fparts.get(FilterPanel.BASE_DV_FILE_INT) instanceof Vector) {
			// CB baseF = new String[((Vector)_Fparts.get("Base")).size()];
			baseF = new String[((Vector) _Fparts
					.get(FilterPanel.BASE_DV_FILE_INT)).size()];
			for (int i = 0; i < baseF.length; ++i) {
				// CB baseF[i] = (String)((Vector)_Fparts.get("Base")).get(i);
				baseF[i] = (String) ((Vector) _Fparts
						.get(FilterPanel.BASE_DV_FILE_INT)).get(i);
			}
		} else { // it is a String
			baseF = new String[1]; // CB 5/2008 bug fix - was missing
			// CB baseF[0] = (String)_Fparts.get("Base");
			baseF[0] = (String) _Fparts.get(FilterPanel.BASE_DV_FILE_INT);
		}
		String F = null;
		// HecMath baseSet = null;
		HecMath dataSet = null;
		boolean isDiff = _mode.equals("Diff"); // CB added
		try {
			for (int j = 0; j < _keys.length; j++) {
				if (!_DssFiles.containsKey(_keys[j])) {
					continue; // CB _DssFiles contains the number of files that
					// are checked!
				}
				Vector<String> files = null;
				// CB added check for multiple files (Vector) option
				if (_DssFiles.get(_keys[j]) instanceof String) {
					files = new Vector<String>();
					if (!((String) _DssFiles.get(_keys[j])).trim().equals("")) {
						files.add((String) _DssFiles.get(_keys[j]));
					}
				} else {
					if (_DssFiles.get(_keys[j]) instanceof Vector) {
						files = (Vector<String>) _DssFiles.get(_keys[j]); // TO
						// DO:
						// check
						// if
						// instanceof?
					}
				}
				Enumeration<String> fileElements = files.elements(); // CB added
				// CB int fileElementCount = 0; //CB added
				theFilePair = new Vector<DSSFile>(2);
				while (fileElements.hasMoreElements()) {
					// CB theFile = DSS.open((String) _DssFiles.get(keys[j]),
					// _timewindow);
					theFile = DSS.open(fileElements.nextElement(), _timewindow); // CB
					// CB fileElementCount++; //CB added
					theFilePair.add(theFile);
				}
				// CB added block for derived timeseries addition
				Vector<Object> pathsAndDTSs = null;
				if (_paths != null) {
					pathsAndDTSs = new Vector<Object>();
					pathsAndDTSs.addAll(_paths);
					if (_dtsWrappers != null) {
						pathsAndDTSs.addAll(_dtsWrappers);
					}
				} else {
					if (_dtsWrappers != null) {
						pathsAndDTSs = new Vector<Object>();
						pathsAndDTSs.addAll(_dtsWrappers);
					}
				}
				// CB for (int i = 0; i < _paths.size(); i++) {
				for (int i = 0; i < pathsAndDTSs.size(); i++) {
					// CB thePath = (String) _paths.get(i);
					for (int k = 0; k < baseF.length; ++k) { // CB
						if (_Fparts.get(_keys[j]) instanceof String) { // CB
							// added
							// the
							// check
							F = (String) _Fparts.get(_keys[j]);
						} else { // CB added section; it must be a Vector
							Enumeration<String> fpartenum = ((Vector<String>) _Fparts
									.get(_keys[j])).elements();
							int fpartenumCount = 0;
							while (fpartenum.hasMoreElements()) {
								fpartenumCount++;
								// if (fileElementCount == fpartenumCount) {
								F = fpartenum.nextElement();
								break;
								// }
							}
						}
						// dataSet = retrieveDataSet(pathsAndDTSs.get(i),
						// baseF[k], F, theFile, _keys[j]); //CB added
						dataSet = retrieveDataSet(pathsAndDTSs.get(i),
								baseF[k], F, theFilePair, _keys[j]); // CB added
						if (dataSet != null) {
							if (!isDiff || ((j > 0) && isDiff)) { // CB added
								// CB if (isAnnualTotal && isExceedence) {
								if (type == ANNUAL_TOTAL_EXCEEDENCE) { // CB
									if (_months.size() < 12) {
										thePlot
												.addData(getExceedence(getAnnualTotals(getMonthlyData(
														dataSet, _months))));
									} else {
										thePlot
												.addData(getExceedence(getAnnualTotals((TimeSeriesContainer) dataSet
														.getData())));
										// CB } else if (isExceedence) {
									}
								} else if (type == EXCEEDENCE) { // CB
									if (_months.size() < 12) {
										thePlot
												.addData(getExceedence(getMonthlyData(
														dataSet, _months)));
									} else {
										thePlot
												.addData(getExceedence((TimeSeriesContainer) dataSet
														.getData()));
										// CB } else if (isAnnualTotal) {
									}
								} else if (type == ANNUAL_TOTAL) { // CB
									if (_months.size() < 12) {
										thePlot
												.addData(getAnnualTotals(getMonthlyData(
														dataSet, _months)));
									} else {
										thePlot
												.addData(getAnnualTotals((TimeSeriesContainer) dataSet
														.getData()));
									}
								} else if (type == MONTHLY_AVERAGE) { // CB
									// added
									// section
									if (_months.size() < 12) {
										thePlot
												.addData(getMonthlyAverages(getMonthlyData(
														dataSet, _months)));
									} else {
										thePlot
												.addData(getMonthlyAverages((TimeSeriesContainer) dataSet
														.getData()));
									}
								} else {
									if (_months.size() < 12) {
										thePlot.addData(getMonthlyData(dataSet,
												_months));
									} else {
										thePlot.addData(dataSet.getData());
									}
								}
							}
							break;
						}
					}
					// CB replaced file close with this block
					Enumeration<DSSFile> pairfiles = theFilePair.elements();
					while (pairfiles.hasMoreElements()) {
						DSSFile file = pairfiles.nextElement();
						file.close();
					}
					// CB theFile.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// CB the following black was done to investigate turning off the lower
		// part of the x-axis for Monthly Average plots
		// (i.e., the year labels). After 3 hours, I gave up.
		/*
		 * Component[] components = thePlot.getComponents(); for (int i = 0; i <
		 * components.length; ++i) { Object component = components[i]; if
		 * (component instanceof JRootPane) { Component[] rpComponents =
		 * ((JRootPane)component).getComponents(); for (int j = 0; j <
		 * rpComponents.length; ++j) { Object rpComponent = rpComponents[j];
		 * Component[] jpComponents = null; if (rpComponent instanceof JPanel) {
		 * //or JLayeredPane jpComponents =
		 * ((JPanel)rpComponent).getComponents(); } else if (rpComponent
		 * instanceof JLayeredPane) { jpComponents =
		 * ((JLayeredPane)rpComponent).getComponents(); } for (int k = 0; k <
		 * jpComponents.length; ++k) { Object jpComponent = jpComponents[k]; if
		 * (jpComponent instanceof JPanel) { Component[] jp2Components =
		 * ((JPanel)jpComponent).getComponents(); for (int l = 0; l <
		 * jp2Components.length; ++l) { Object jp2Component = jp2Components[l];
		 * if (jp2Component instanceof G2dPanel) { Component[] g2dpComponents =
		 * ((JPanel)jp2Component).getComponents(); for (int m = 0; m <
		 * g2dpComponents.length; ++m) { Object g2dpComponent =
		 * g2dpComponents[m]; System.out.println("g2dpComponent class = " +
		 * g2dpComponent.getClass()); // if (g2dpComponent instanceof
		 * hec.gfx2d.AxisLabel) { //all 3 are empty Strings if (g2dpComponent
		 * instanceof hec.gfx2d.AxisTics) { // hec.gfx2d.G2dSpacer //
		 * hec.gfx2d.AxisTics // hec.gfx2d.AxisScrollbar // hec.gfx2d.Viewport
		 * // hec.gfx2d.TitlePanel // hec.gfx2d.ViewportToolbar Component[]
		 * atComponents = ((hec.gfx2d.AxisTics)g2dpComponent).getComponents();
		 * //0 components Object axis =
		 * ((hec.gfx2d.AxisTics)g2dpComponent).getAxis(); axis = null; } else if
		 * (g2dpComponent instanceof hec.gfx2d.Viewport) { Object viewport =
		 * (hec.gfx2d.Viewport)g2dpComponent; System.out.print(""); } else if
		 * (g2dpComponent instanceof hec.gfx2d.TitlePanel) { Object titlePanel =
		 * (hec.gfx2d.TitlePanel)g2dpComponent; System.out.print(""); } } } } }
		 * } } } }
		 */
		// thePlot.setEditable(true); //no effect?
		// thePlot.setXAxisColor(Color.BLUE); //no effect
		return thePlot;
	}

	// CB added
	/*
	 * TableFrame loadTable_originalversion(String title) { TableFrame theTable
	 * = Tabulate.newTable(title); //No effect on title
	 * theTable.setTableTitleText(title); //No effect on title; puts title just
	 * below menubar
	 * 
	 * String thePath = null; //CB String keys[] = { "Base", "1", "2", "3" };
	 * DSSFile theFile = null; //CB String baseF = (String) _Fparts.get("Base");
	 * String[] baseF = null; //CB added capability of more than one fpart for
	 * each key if (_Fparts.get("Base") instanceof Vector) { baseF = new
	 * String[((Vector)_Fparts.get("Base")).size()]; for (int i = 0; i <
	 * baseF.length; ++i) { baseF[i] =
	 * (String)((Vector)_Fparts.get("Base")).get(i); } } else { // it is a
	 * String baseF = new String[1]; //CB 5/2008 bug fix - was missing baseF[0]
	 * = (String)_Fparts.get("Base"); } String F = null; HecMath baseSet = null;
	 * HecMath dataSet = null;
	 * 
	 * try { for (int j = 0; j < _keys.length; j++) { if
	 * (!_DssFiles.containsKey(_keys[j])) continue; //CB _DssFiles contains the
	 * number of files that are checked! Vector<String> files = null; //CB added
	 * check for multiple files (Vector) option if (_DssFiles.get(_keys[j])
	 * instanceof String) { files = new Vector<String>(); if
	 * (!((String)_DssFiles.get(_keys[j])).trim().equals("")) //CB 5/2008 added
	 * check to prevent empty String file name!
	 * files.add((String)_DssFiles.get(_keys[j])); } else { if
	 * (_DssFiles.get(_keys[j]) instanceof Vector) files =
	 * (Vector<String>)_DssFiles.get(_keys[j]); //TO DO: check if instanceof? }
	 * Enumeration<String> fileElements = files.elements(); //CB added int
	 * fileElementCount = 0; //CB added while (fileElements.hasMoreElements()) {
	 * //CB theFile = DSS.open((String) _DssFiles.get(keys[j]), _timewindow);
	 * theFile = DSS.open((String) fileElements.nextElement(), _timewindow);
	 * //CB fileElementCount++; //CB added
	 * 
	 * //CB added block for derived timeseries addition Vector<Object>
	 * pathsAndDTSs = null; if (_paths != null) { pathsAndDTSs = new
	 * Vector<Object>(); pathsAndDTSs.addAll(_paths); if (_dtsWrappers != null)
	 * { pathsAndDTSs.addAll(_dtsWrappers); } } else { if (_dtsWrappers != null)
	 * { pathsAndDTSs = new Vector<Object>(); pathsAndDTSs.addAll(_dtsWrappers);
	 * } } //CB for (int i = 0; i < _paths.size(); i++) { for (int i = 0; i <
	 * pathsAndDTSs.size(); i++) { //CB thePath = (String) _paths.get(i);
	 * boolean wasFoundInFile = false; for (int k = 0; k < baseF.length; ++k) {
	 * //CB if (_Fparts.get(_keys[j]) instanceof String) { //CB added the check
	 * // System.out.println("String fpart = " + (String) _Fparts.get(keys[j]));
	 * //CB debugging F = (String) _Fparts.get(_keys[j]); } else { //CB added
	 * section; it must be a Vector Enumeration<String> fpartenum =
	 * ((Vector<String>)_Fparts.get(_keys[j])).elements(); int fpartenumCount =
	 * 0; while (fpartenum.hasMoreElements()) { fpartenumCount++; if
	 * (fileElementCount == fpartenumCount) { F =
	 * (String)fpartenum.nextElement(); break; } } } //CB F = (String)
	 * _Fparts.get(keys[j]); try { //CB added to prevent exit after one bad
	 * path. Then moved to here from below if (pathsAndDTSs.get(i) instanceof
	 * String) { thePath = (String) _paths.get(i); thePath =
	 * thePath.replace(baseF[k], F); dataSet = theFile.read(thePath); //CB moved
	 * to here from below // units conversion CFS->TAF CB moved here from below
	 * even though it required less calcs there String [] parts =
	 * thePath.split("/");
	 * //NO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! if
	 * (parts[3].toLowerCase().indexOf("storage") > -1) dataSet =
	 * unitsConversion(dataSet, true); else dataSet = unitsConversion(dataSet,
	 * false); } else if (pathsAndDTSs.get(i) instanceof DtsWrapper) { dataSet =
	 * calculateDTSDataset((DtsWrapper)pathsAndDTSs.get(i), theFile, baseF[k],
	 * F); }
	 * 
	 * //CB try { //CB added to prevent exit after one bad path. //CB dataSet =
	 * theFile.read(thePath); if (_mode.equals("Diff")) { if
	 * (_keys[j].equals("Base")) { //CB baseSet = theFile.read(thePath); baseSet
	 * = dataSet; //Cb replaced the above continue; } //CB dataSet =
	 * theFile.read(thePath); dataSet = dataSet.subtract(baseSet);
	 * wasFoundInFile = true; //CB added } else { //CB dataSet =
	 * theFile.read(thePath); wasFoundInFile = true; //CB added }
	 * 
	 * // units conversion CFS->TAF // moved up units block even though requires
	 * less calcs. here // String [] parts = thePath.split("/");
	 * //NO!!!!!!!!!!!!
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * !!!!!!!!!!!!!!!!! // if (parts[3].toLowerCase().indexOf("storage") > -1)
	 * // dataSet = unitsConversion(dataSet, true); // else // dataSet =
	 * unitsConversion(dataSet, false);
	 * 
	 * if (_months.size() < 12) theTable.addData(getMonthlyData(dataSet,
	 * _months)); else theTable.addData(dataSet.getData()); } catch
	 * (DSSFileException dssFE) { // path not found probably - message given
	 * from elsewhere // we want to continue with the rest so do nothing } // if
	 * (wasFoundInFile) break; } } theFile.close(); } } } catch (Exception e) {
	 * e.printStackTrace(); } return theTable; }
	 */

	// CB added
	HecDataTableFrame loadTable(String title) {
		HecDataTableFrame theTable = Tabulate.newTable(title); // No
		// effect
		// on
		// title
		theTable.setTableTitleText(title); // No effect on title; puts title
		// just below menubar
		// CB String thePath = null;
		// CB String keys[] = { "Base", "1", "2", "3" };
		DSSFile theFile = null;
		Vector<DSSFile> theFilePair = null; // CB added
		// CB String baseF = (String) _Fparts.get("Base");
		String[] baseF = null; // CB added capability of more than one fpart for
		// each key
		// CB if (_Fparts.get("Base") instanceof Vector) {
		if (_Fparts.get(FilterPanel.BASE_DV_FILE_INT / 4) instanceof Vector) { // CB
			// CB baseF = new String[((Vector)_Fparts.get("Base")).size()];
			baseF = new String[((Vector) _Fparts
					.get(FilterPanel.BASE_DV_FILE_INT / 4)).size()];
			for (int i = 0; i < baseF.length; ++i) {
				// CB baseF[i] = (String)((Vector)_Fparts.get("Base")).get(i);
				baseF[i] = (String) ((Vector) _Fparts
						.get(FilterPanel.BASE_DV_FILE_INT / 4)).get(i);
			}
		} else { // it is a String
			baseF = new String[1]; // CB 5/2008 bug fix - was missing
			// CB baseF[0] = (String)_Fparts.get("Base");
			baseF[0] = (String) _Fparts.get(FilterPanel.BASE_DV_FILE_INT / 4);
		}
		String F = null;
		// CB HecMath baseSet = null;
		HecMath dataSet = null;
		boolean isDiff = _mode.equals("Diff"); // CB added
		try {
			for (int j = 0; j < _keys.length; j++) {
				if (!_DssFiles.containsKey(_keys[j])) {
					continue; // CB _DssFiles contains the number of files that
					// are checked!
				}
				Vector<String> files = null;
				// CB added check for multiple files (Vector) option
				if (_DssFiles.get(_keys[j]) instanceof String) {
					files = new Vector<String>();
					if (!((String) _DssFiles.get(_keys[j])).trim().equals("")) {
						files.add((String) _DssFiles.get(_keys[j]));
					}
				} else {
					if (_DssFiles.get(_keys[j]) instanceof Vector) {
						files = (Vector<String>) _DssFiles.get(_keys[j]); // TO
						// DO:
						// check
						// if
						// instanceof?
					}
				}
				Enumeration<String> fileElements = files.elements(); // CB added
				// CB int fileElementCount = 0; //CB added
				theFilePair = new Vector<DSSFile>(2);
				while (fileElements.hasMoreElements()) {
					// CB theFile = DSS.open((String) _DssFiles.get(keys[j]),
					// _timewindow);
					theFile = DSS.open(fileElements.nextElement(), _timewindow); // CB
					// CB fileElementCount++; //CB added
					theFilePair.add(theFile);
				}
				// CB added block for derived timeseries addition
				Vector<Object> pathsAndDTSs = null;
				if (_paths != null) {
					pathsAndDTSs = new Vector<Object>();
					pathsAndDTSs.addAll(_paths);
					if (_dtsWrappers != null) {
						pathsAndDTSs.addAll(_dtsWrappers);
					}
				} else {
					if (_dtsWrappers != null) {
						pathsAndDTSs = new Vector<Object>();
						pathsAndDTSs.addAll(_dtsWrappers);
					}
				}
				// CB for (int i = 0; i < _paths.size(); i++) {
				for (int i = 0; i < pathsAndDTSs.size(); i++) {
					// CB thePath = (String) _paths.get(i);
					for (int k = 0; k < baseF.length; ++k) { // CB
						if (_Fparts.get(_keys[j]) instanceof String) { // CB
							// added
							// the
							// check
							F = (String) _Fparts.get(_keys[j]);
						} else { // CB added section; it must be a Vector
							Enumeration<String> fpartenum = ((Vector<String>) _Fparts
									.get(_keys[j])).elements();
							int fpartenumCount = -1;
							while (fpartenum.hasMoreElements()) {
								fpartenumCount++;
								if (k == fpartenumCount) {
									F = fpartenum.nextElement();
									break;
								}
							}
						}
						// dataSet = retrieveDataSet(pathsAndDTSs.get(i),
						// baseF[k], F, theFile, _keys[j]); //CB added TODO
						// study why DIFFERENCE HERE!!!!!!!!
						dataSet = retrieveDataSet(pathsAndDTSs.get(i),
								baseF[k], F, theFilePair, _keys[j]); // CB added
						// TODO
						// study
						// why
						// DIFFERENCE
						// HERE!!!!!!!!
						if (dataSet != null) {
							if (!isDiff || ((j > 0) && isDiff)) { // CB added
								if (_months.size() < 12) {
									theTable.addData(getMonthlyData(dataSet,
											_months));
								} else {
									theTable.addData(dataSet.getData()); // CB
									// TODO
									// Reservoir
									// Non-recov
									// spills
									// had
									// NPE
									// here!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
								}
							}
							break;
						}
					}
					// CB replaced file close with this block
					Enumeration<DSSFile> pairfiles = theFilePair.elements();
					while (pairfiles.hasMoreElements()) {
						DSSFile file = pairfiles.nextElement();
						file.close();
					}
					// CB theFile.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return theTable;
	}

	// CB added
	/*
	 * MonthlyTableFrame loadMonthlyTable_originalversion(String title, boolean
	 * isWateryear) { String thePath = null; //CB String keys[] = {"Base", "1",
	 * "2", "3" }; MonthlyTableFrame theTable = null;
	 * 
	 * try { DSSFile theFile = null; theTable = new MonthlyTableFrame(this);
	 * //CB String baseF = (String) _Fparts.get("Base"); String[] baseF = null;
	 * //CB added capability of more than one fpart for each key if
	 * (_Fparts.get("Base") instanceof Vector) { baseF = new
	 * String[((Vector)_Fparts.get("Base")).size()]; for (int i = 0; i <
	 * baseF.length; ++i) { baseF[i] =
	 * (String)((Vector)_Fparts.get("Base")).get(i); } } else { // it is a
	 * String baseF = new String[1]; //CB 5/2008 bug fix - was missing baseF[0]
	 * = (String)_Fparts.get("Base"); } String F = null; // HecMath[] baseSet =
	 * new HecMath[_paths.size()]; // HecMath[] dataSet = new
	 * HecMath[_paths.size()]; HecMath baseSet = null; HecMath dataSet = null;
	 * 
	 * for (int j = 0; j < _keys.length; j++) { if
	 * (!_DssFiles.containsKey(_keys[j])) continue; //CB _DssFiles contains the
	 * number of files that are checked! Vector<String> files = null; //CB added
	 * check for multiple files (Vector) option if (_DssFiles.get(_keys[j])
	 * instanceof String) { files = new Vector<String>(); if
	 * (!((String)_DssFiles.get(_keys[j])).trim().equals("")) //CB 5/2008 added
	 * check to prevent empty String file name!
	 * files.add((String)_DssFiles.get(_keys[j])); } else { if
	 * (_DssFiles.get(_keys[j]) instanceof Vector) files =
	 * (Vector<String>)_DssFiles.get(_keys[j]); //TO DO: check if instanceof? }
	 * Enumeration<String> fileElements = files.elements(); //CB added int
	 * fileElementCount = 0; //CB added while (fileElements.hasMoreElements()) {
	 * //CB theFile = DSS.open((String) _DssFiles.get(keys[j]), _timewindow);
	 * theFile = DSS.open((String) fileElements.nextElement(), _timewindow);
	 * //CB fileElementCount++; //CB added
	 * 
	 * //CB added block for derived timeseries addition Vector<Object>
	 * pathsAndDTSs = null; if (_paths != null) { pathsAndDTSs = new
	 * Vector<Object>(); pathsAndDTSs.addAll(_paths); if (_dtsWrappers != null)
	 * { pathsAndDTSs.addAll(_dtsWrappers); } } else { if (_dtsWrappers != null)
	 * { pathsAndDTSs = new Vector<Object>(); pathsAndDTSs.addAll(_dtsWrappers);
	 * } } //CB for (int i = 0; i < _paths.size(); i++) { for (int i = 0; i <
	 * pathsAndDTSs.size(); i++) { //CB thePath = (String) _paths.get(i);
	 * boolean wasFoundInFile = false; for (int k = 0; k < baseF.length; ++k) {
	 * if (_Fparts.get(_keys[j]) instanceof String) { //CB added the check F =
	 * (String) _Fparts.get(_keys[j]); } else { //CB added section - it must be
	 * a Vector, Victor Enumeration<String> fpartenum =
	 * ((Vector<String>)_Fparts.get(_keys[j])).elements(); int fpartenumCount =
	 * 0; while (fpartenum.hasMoreElements()) { fpartenumCount++; if
	 * (fileElementCount == fpartenumCount) { F =
	 * (String)fpartenum.nextElement(); break; } } } //CB F = (String)
	 * _Fparts.get(keys[j]); if (F == null) continue; try { //CB added to
	 * prevent exit after one bad path. Then moved to here from below if
	 * (pathsAndDTSs.get(i) instanceof String) { thePath = (String)
	 * _paths.get(i); if (!baseF[k].equalsIgnoreCase(F)) thePath =
	 * thePath.replace(baseF[k], F); //CB TO DO: ALTER FOR WRIMS 2 dataSet =
	 * theFile.read(thePath); //CB moved to here from below // units conversion
	 * CFS->TAF CB moved here from below even though it required less calcs
	 * there String [] parts = thePath.split("/"); if
	 * (parts[3].toLowerCase().indexOf("storage") > -1) dataSet =
	 * unitsConversion(dataSet, true); else dataSet = unitsConversion(dataSet,
	 * false); } else if (pathsAndDTSs.get(i) instanceof DtsWrapper) { dataSet =
	 * calculateDTSDataset((DtsWrapper)pathsAndDTSs.get(i), theFile, baseF[k],
	 * F); }
	 * 
	 * if (_mode.equals("Diff")) { if (_keys[j].equals("Base")) { //CB baseSet =
	 * theFile.read(thePath); baseSet = dataSet; //Cb replaced the above
	 * continue; } //CB dataSet = theFile.read(thePath); dataSet =
	 * dataSet.subtract(baseSet); wasFoundInFile = true; //CB added } else {
	 * //CB dataSet = theFile.read(thePath); wasFoundInFile = true; //CB added }
	 * 
	 * // units conversion CFS->TAF // moved up units block even though requires
	 * less calcs. here // String [] parts = thePath.split("/");
	 * //NO!!!!!!!!!!!!
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * !!!!!!!!!!!!!!!!! // if (parts[3].toLowerCase().indexOf("storage") > -1)
	 * // dataSet = unitsConversion(dataSet, true); // else // dataSet =
	 * unitsConversion(dataSet, false);
	 * 
	 * //CB allow??? if (_months.size() < 12) //CB
	 * theTable.addData(getMonthlyData(dataSet, _months)); //CB else //CB
	 * theTable.addData(dataSet.getData()); // TO DO: check out the return value
	 * "status" if (isWateryear) // theTable.addData(dataSet[i].getData(), 0);
	 * // TO DO: check out the return value "status"
	 * theTable.addData(dataSet.getData(), 0); // TO DO: check out the return
	 * value "status" else // theTable.addData(dataSet[i].getData(), 3); // TO
	 * DO: calendar year theTable.addData(dataSet.getData(), 3); // TO DO:
	 * calendar year } catch (DSSFileException e) { //
	 * System.out.println(thePath + " is NOT in file: " +
	 * _DssFiles.get(keys[j])); // path not found probably - message given from
	 * elsewhere // we want to continue with the rest so do nothing } if
	 * (wasFoundInFile) break; } } theFile.close(); } } } catch (Exception e) {
	 * e.printStackTrace(); } return theTable; }
	 */

	// CB added
	MonthlyTableFrame loadMonthlyTable(String title, int annualType) {
		MonthlyTableFrame theTable = new MonthlyTableFrame(this);
		// String thePath = null;
		// CB String keys[] = { "Base", "1", "2", "3" };
		DSSFile theFile = null;
		Vector<DSSFile> theFilePair = null;
		// CB String baseF = (String) _Fparts.get("Base");
		String[] baseF = null; // CB added capability of more than one fpart for
		// each key
		// CB if (_Fparts.get("Base") instanceof Vector) {
		if (_Fparts.get(FilterPanel.BASE_DV_FILE_INT) instanceof Vector) {
			// CB baseF = new String[((Vector)_Fparts.get("Base")).size()];
			baseF = new String[((Vector) _Fparts
					.get(FilterPanel.BASE_DV_FILE_INT)).size()];
			for (int i = 0; i < baseF.length; ++i) {
				// baseF[i] = (String)((Vector)_Fparts.get("Base")).get(i);
				baseF[i] = (String) ((Vector) _Fparts
						.get(FilterPanel.BASE_DV_FILE_INT)).get(i);
			}
		} else { // it is a String
			baseF = new String[1]; // CB 5/2008 bug fix - was missing
			// baseF[0] = (String)_Fparts.get("Base");
			baseF[0] = (String) _Fparts.get(FilterPanel.BASE_DV_FILE_INT);
		}
		String F = null;
		// CB HecMath baseSet = null;
		HecMath dataSet = null;
		boolean isDiff = _mode.equals("Diff"); // CB added
		try {
			for (int j = 0; j < _keys.length; j++) {
				if (!_DssFiles.containsKey(_keys[j])) {
					continue; // CB _DssFiles contains the number of files that
					// are checked!
				}
				Vector<String> files = null;
				// CB added check for multiple files (Vector) option
				if (_DssFiles.get(_keys[j]) instanceof String) {
					files = new Vector<String>();
					if (!((String) _DssFiles.get(_keys[j])).trim().equals("")) {
						files.add((String) _DssFiles.get(_keys[j]));
					}
				} else {
					if (_DssFiles.get(_keys[j]) instanceof Vector) {
						files = (Vector<String>) _DssFiles.get(_keys[j]); // TO
						// DO:
						// check
						// if
						// instanceof?
					}
				}
				Enumeration<String> fileElements = files.elements(); // CB added
				// int fileElementCount = 0; //CB added
				theFilePair = new Vector<DSSFile>(2);
				while (fileElements.hasMoreElements()) {
					// CB theFile = DSS.open((String) _DssFiles.get(keys[j]),
					// _timewindow);
					theFile = DSS.open(fileElements.nextElement(), _timewindow); // CB
					// fileElementCount++; //CB added
					theFilePair.add(theFile);
				}

				// CB added block for derived timeseries addition
				Vector<Object> pathsAndDTSs = null;
				if (_paths != null) {
					pathsAndDTSs = new Vector<Object>();
					pathsAndDTSs.addAll(_paths);
					if (_dtsWrappers != null) {
						pathsAndDTSs.addAll(_dtsWrappers);
					}
				} else {
					if (_dtsWrappers != null) {
						pathsAndDTSs = new Vector<Object>();
						pathsAndDTSs.addAll(_dtsWrappers);
					}
				}
				// CB for (int i = 0; i < _paths.size(); i++) {
				for (int i = 0; i < pathsAndDTSs.size(); i++) {
					// CB thePath = (String) _paths.get(i);
					for (int k = 0; k < baseF.length; ++k) { // CB
						if (_Fparts.get(_keys[j]) instanceof String) { // CB
							// added
							// the
							// check
							F = (String) _Fparts.get(_keys[j]);
						} else { // CB added section; it must be a Vector
							Enumeration<String> fpartenum = ((Vector<String>) _Fparts
									.get(_keys[j])).elements();
							int fpartenumCount = -1;
							while (fpartenum.hasMoreElements()) {
								fpartenumCount++;
								if (k == fpartenumCount) {
									F = fpartenum.nextElement();
									break;
								}
							}
						}
						// dataSet = retrieveDataSet(pathsAndDTSs.get(i),
						// baseF[k], F, theFile, _keys[j]); //CB added
						dataSet = retrieveDataSet(pathsAndDTSs.get(i),
								baseF[k], F, theFilePair, _keys[j]); // CB added
						if (dataSet != null) {
							if (!isDiff || ((j > 0) && isDiff)) { // CB added
								int firstPossibleMonth;
								if (_annualTypeMode == MessagePanel.WATERYEAR) {
									firstPossibleMonth = 0;
								} else if (_annualTypeMode == MessagePanel.CALENDAR_YEAR) {
									firstPossibleMonth = 3;
								} else if (_annualTypeMode == MessagePanel.FEDERAL_CONTRACT_YEAR) {
									firstPossibleMonth = 5;
								} else {
									return null;
								}

								if (_months.size() < 12) {
									theTable.addData(getMonthlyData(dataSet,
											_months), firstPossibleMonth);
								} else {
									// theTable.addData(dataSet[i].getData(),
									// firstPossibleMonth); // TO DO: check out
									// the return value "status"
									theTable.addData(dataSet.getData(),
											firstPossibleMonth); // TO DO: check
									// out the
									// return
									// value
									// "status"
								}
							}
							break;
						}
					}
				}
				// CB replaced file close with this block
				Enumeration<DSSFile> pairfiles = theFilePair.elements();
				while (pairfiles.hasMoreElements()) {
					DSSFile file = pairfiles.nextElement();
					file.close();
				}
				// theFile.close();
				// altered to send dv-sv pair of files }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return theTable;
	}

	/**
	 * CB added to do the work common to all three output types.
	 * 
	 * @param pathOrDTS
	 * @param baseF
	 * @param F
	 * @param file
	 * @param key
	 * @return
	 */
	// CB private HecMath retrieveDataSet(Object pathOrDTS, String baseF, String
	// F, DSSFile file, String key) {
	// private HecMath retrieveDataSet(Object pathOrDTS, String baseF, String F,
	// DSSFile file, int key) {
	private HecMath retrieveDataSet(Object pathOrDTS, String baseF, String F,
			Vector<DSSFile> files, int key) {
		// HecMath baseSet = null;
		HecMath dataSet = null;
		if (pathOrDTS instanceof String) {
			Enumeration<DSSFile> filesEnum = files.elements();
			while (filesEnum.hasMoreElements()) {
				DSSFile file = filesEnum.nextElement();
				String path = ((String) pathOrDTS).replace(baseF, F);
				try { // CB added to prevent exit after one bad path. Then moved
					// to here from below
					dataSet = file.read(path); // CB moved to here from below
					// units conversion CFS->TAF CB moved here from below even
					// though it required less calcs there
					String[] parts = path.split("/"); // NO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
					if (dataSet != null) {
						if (parts[3].toLowerCase().indexOf("storage") > -1) {
							dataSet = unitsConversion(dataSet, true);
						} else {
							dataSet = unitsConversion(dataSet, false);
						}
						break;
					}
				} catch (HecMathException hme) {
					hme.printStackTrace();
				} catch (DSSFileException dfe) {
					// path not found probably - message given from elsewhere
					// we want to continue with the rest so do nothing
				}
			}
		} else if (pathOrDTS instanceof DTSWrapper) {
			// dataSet = calculateDTSDataset((DTSWrapper)pathOrDTS, file, baseF,
			// F);
			dataSet = calculateDTSDataset((DTSWrapper) pathOrDTS, files, baseF,
					F);
		}
		if (dataSet != null) {
			if (_mode.equals("Diff")) {
				if (key == _keys[0]) {
					_baseSet = dataSet;
				} else {
					try {
						dataSet = dataSet.subtract(_baseSet);
					} catch (HecMathException hme) {
						hme.printStackTrace();
					}
				}
			}
		}
		return dataSet;
	}

	/**
	 * CB added.
	 * 
	 * @param dtsWrapper
	 * @param file
	 * @param baseFpart
	 * @param fPart
	 * @return
	 */
	/*
	 * private HecMath calculateDTSDataset_oldandwrong(DTSWrapper dtsWrapper,
	 * DSSFile file, String baseFpart, String fPart) { HecMath totalDataSet =
	 * null; DerivedTimeSeries dts = dtsWrapper.getDerivedTimeSeries();
	 * Hashtable<String, String> varnameToPathMap =
	 * dtsWrapper.getVariableNameToDssPathMap(); for (int i = 0; i <
	 * dts.getNumberOfDataReferences(); ++i) { HecMath dataSet = null; try { if
	 * (dts.getBPartAt(i) != null && !dts.getBPartAt(i).trim().equals("")) {
	 * //TODO: test when row has DTS (bpart may be blank?)!!!! String path =
	 * varnameToPathMap.get((String)dts.getBPartAt(i)); // TODO: may be null!
	 * System.out.println(dts.getBPartAt(i)); try { if (path == null) {
	 * JOptionPane.showMessageDialog(null, "path, " + path +
	 * " is null for dts, " + dtsWrapper.getDTSName(),
	 * "Null Path - DTS not calculated", JOptionPane.ERROR_MESSAGE); return
	 * null; } path = path.replace(baseFpart, fPart); //CB TODO: add check for
	 * sameness first dataSet = file.read(path); // if (i ==
	 * dts.getNumberOfDataReferences() - 1) { // units conversion CFS->TAF
	 * String [] parts = path.split("/"); if
	 * (parts[3].toLowerCase().indexOf("storage") > -1) dataSet =
	 * unitsConversion(dataSet, true); else dataSet = unitsConversion(dataSet,
	 * false); // } } catch (HecMathException hme) { hme.printStackTrace();
	 * System.out.println("Error calculating dataset for derived timeseries, " +
	 * dts.getName()); } } else { //row must contain a DerivedTimeSeries instead
	 * if (dts.getDTSNameAt(i) != null) { //use dtsList Vector<DTSWrapper>
	 * dtsList = dtsWrapper.getDTSList(); Enumeration<DTSWrapper> dtsWrappers =
	 * dtsList.elements(); while (dtsWrappers.hasMoreElements()) { DTSWrapper
	 * wrapper = dtsWrappers.nextElement(); if (dataSet == null) dataSet =
	 * calculateDTSDataset(wrapper, file, baseFpart, fPart); // grabs dataset
	 * for each line in DTS table (ops handled in called method) else { //
	 * HecMath dataSet2 = null; // temp?????? // dataSet2 =
	 * calculateDTSDataset(wrapper, file, baseFpart, fPart); // temp??????
	 * dataSet = dataSet.add(calculateDTSDataset(wrapper, file, baseFpart,
	 * fPart)); // grabs and adds dataset for each line in DTS table (ops
	 * handled in called method) } } // DTSWrapper dtsWrapper2 = new
	 * DTSWrapper(dts.getDTSNameAt(i), // new
	 * DerivedTimeSeries(dts.getDTSNameAt(i)), varnameToPathMap); //TODO:
	 * calculateDTSDataset must handle unit conversion for "storage" variables!
	 * // dataSet = calculateDTSDataset(dtsWrapper2, file, baseFpart, fPart); }
	 * } if (i == 0) { // same as if (totalDataSet == null) totalDataSet =
	 * dataSet; } String operator = dts.getOperationAt(i); char op =
	 * operator.trim().charAt(0); switch (op) { case '+': if (i != 0)
	 * totalDataSet = totalDataSet.add(dataSet); break; case '-': if (i != 0)
	 * totalDataSet = totalDataSet.subtract(dataSet); else totalDataSet =
	 * totalDataSet.inverse(); //TODO: check if correct!!!!!!!!!!!!! break; case
	 * '*': if (i != 0) totalDataSet = totalDataSet.multiply(dataSet); else
	 * JOptionPane.showMessageDialog(null, "Operator for " + dts.getBPartAt(i) +
	 * " row must be + or -", "Error in First Row of DerivedTimeSeries",
	 * JOptionPane.ERROR_MESSAGE); break; case '/': if (i != 0) totalDataSet =
	 * totalDataSet.divide(dataSet); else JOptionPane.showMessageDialog(null,
	 * "Operator for " + dts.getBPartAt(i) + " row must be + or -",
	 * "Error in First Row of DerivedTimeSeries", JOptionPane.ERROR_MESSAGE);
	 * break; default: System.out.println("Operator, " + op +
	 * " is not allowed at this time"); } } catch (HecMathException hme) {
	 * hme.printStackTrace();
	 * System.out.println("Error calculating dataset for derived timeseries, " +
	 * dts.getName()); }
	 * 
	 * } try { totalDataSet.setLocation(dtsWrapper.getDTSName()); } catch
	 * (HecMathException hme) { hme.printStackTrace();
	 * System.out.println("Error renaming dataset to DTS name"); } return
	 * totalDataSet; }
	 */

	/**
	 * CB added.
	 * 
	 * @param dtsWrapper
	 * @param file
	 * @param baseFpart
	 * @param fPart
	 * @return
	 */
	// CB private HecMath calculateDTSDataset(DTSWrapper dtsWrapper, DSSFile
	// file, String baseFpart,
	private HecMath calculateDTSDataset(DTSWrapper dtsWrapper,
			Vector<DSSFile> files, String baseFpart, String fPart) {
		HecMath totalDataSet = null;
		DerivedTimeSeries dts = dtsWrapper.getDerivedTimeSeries();
		Hashtable<String, String> varnameToPathMap = dtsWrapper
				.getVariableNameToDssPathMap();
		for (int i = 0; i < dts.getNumberOfDataReferences(); ++i) {
			HecMath dataSet = null;
			try {
				if ((dts.getBPartAt(i) != null)
						&& !dts.getBPartAt(i).trim().equals("")) { // ith row
					// has a
					// b-part
					// (variable)
					String path = varnameToPathMap.get(dts.getBPartAt(i)); // TODO:
					// may
					// be
					// null!
					Enumeration<DSSFile> fileEnum = files.elements();
					while (fileEnum.hasMoreElements()) {
						DSSFile file = fileEnum.nextElement();
						System.out.println("DTS row path = " + path);
						try {
							if (path == null) {
								JOptionPane.showMessageDialog(null, "path, "
										+ path + " is null for dts, "
										+ dtsWrapper.getDTSName(),
										"Null Path - DTS not calculated",
										JOptionPane.ERROR_MESSAGE);
								return null;
							}
							path = path.replace(baseFpart, fPart); // CB TODO:
							// add check
							// for
							// sameness
							// first
							dataSet = file.read(path);
							// if (i == dts.getNumberOfDataReferences() - 1) {
							// units conversion CFS->TAF
							if (dataSet != null) {
								String[] parts = path.split("/");
								if (parts[3].toLowerCase().indexOf("storage") > -1) {
									dataSet = unitsConversion(dataSet, true);
								} else {
									dataSet = unitsConversion(dataSet, false);
									// }
								}
							}
						} catch (HecMathException hme) {
							hme.printStackTrace();
							System.out
									.println("Error calculating dataset for derived timeseries, "
											+ dts.getName());
						} catch (DSSFileException dfe) {
							// System.out.println(path + " not found in file, "
							// + file.toString()); //DSSFile class sucks - no
							// way to print file path (absolute filename)
						}
					}
				} else { // ith row must contain a DerivedTimeSeries instead
					if (dts.getDTSNameAt(i) != null) {
						System.out.println("dts\'s dts name at index, " + i
								+ ", is " + dts.getDTSNameAt(i));
						Vector<DTSWrapper> dtsList = dtsWrapper.getDTSList();
						Enumeration<DTSWrapper> dtsWrappers = dtsList
								.elements();
						while (dtsWrappers.hasMoreElements()) { // NO - need
							// only ith
							// wrapper.
							DTSWrapper wrapper = dtsWrappers.nextElement();
							if (wrapper.getDTSName()
									.equals(dts.getDTSNameAt(i))) {
								// if (dataSet == null)
								// dataSet = calculateDTSDataset(wrapper, file,
								// baseFpart, fPart); // grabs dataset for each
								// line in DTS table (ops handled in called
								// method)
								dataSet = calculateDTSDataset(wrapper, files,
										baseFpart, fPart); // grabs dataset for
								// each line in DTS
								// table (ops
								// handled in called
								// method)
								// else {
								// dataSet =
								// dataSet.add(calculateDTSDataset(wrapper,
								// file, baseFpart, fPart)); // grabs and adds
								// dataset for each line in DTS table (ops
								// handled in called method)
								// }
								break;
							}
						}
						// DTSWrapper dtsWrapper2 = new
						// DTSWrapper(dts.getDTSNameAt(i),
						// new DerivedTimeSeries(dts.getDTSNameAt(i)),
						// varnameToPathMap);
						// TODO: calculateDTSDataset must handle unit conversion
						// for "storage" variables!
						// dataSet = calculateDTSDataset(dtsWrapper2, file,
						// baseFpart, fPart);
					}
				}
				boolean isFirstNonNullSet = false;
				if (dataSet != null) {
					if ((i == 0) || (totalDataSet == null)) { // same as if
						// (totalDataSet
						// == null)
						totalDataSet = dataSet;
						isFirstNonNullSet = true;
					}
					String operator = dts.getOperationAt(i);
					char op = operator.trim().charAt(0);
					switch (op) {
					case '+':
						if (!isFirstNonNullSet) {
							totalDataSet = totalDataSet.add(dataSet);
						}
						break;
					case '-':
						if (!isFirstNonNullSet) {
							totalDataSet = totalDataSet.subtract(dataSet);
						} else {
							totalDataSet = totalDataSet.inverse(); // TODO:
							// check if
							// correct!!!!!!!!!!!!!
						}
						break;
					case '*':
						if (!isFirstNonNullSet) {
							totalDataSet = totalDataSet.multiply(dataSet);
						} else {
							JOptionPane.showMessageDialog(null,
									"Operator for " + dts.getBPartAt(i)
											+ " row must be + or -",
									"Error in First Row of DerivedTimeSeries",
									JOptionPane.ERROR_MESSAGE);
						}
						break;
					case '/':
						if (!isFirstNonNullSet) {
							totalDataSet = totalDataSet.divide(dataSet);
						} else {
							JOptionPane.showMessageDialog(null,
									"Operator for " + dts.getBPartAt(i)
											+ " row must be + or -",
									"Error in First Row of DerivedTimeSeries",
									JOptionPane.ERROR_MESSAGE);
						}
						break;
					default:
						System.out.println("Operator, " + op
								+ " is not allowed at this time");
					}
				}
			} catch (HecMathException hme) {
				hme.printStackTrace();
				System.out
						.println("Error calculating dataset for derived timeseries, "
								+ dts.getName());
			}

		}
		try {
			if (totalDataSet != null) {
				totalDataSet.setLocation(dtsWrapper.getDTSName());
			}
		} catch (HecMathException hme) {
			hme.printStackTrace();
			System.out.println("Error renaming dataset to DTS name");
		}
		return totalDataSet;
	}

	/**
	 * CB added.
	 * 
	 * @param path
	 * @param baseF
	 * @param key
	 * @param fileElementCount
	 * @param file
	 * @return
	 */
	HecMath loadBasesetForPath(String path, String[] baseF, String key,
			int fileElementCount, DSSFile file) {
		HecMath dataSet = null;
		boolean wasFoundInFile = false;
		String F = null;
		for (int k = 0; k < baseF.length; ++k) {
			if (_Fparts.get(key) instanceof String) { // CB added the check
				F = (String) _Fparts.get(key);
			} else { // must be a Vector, Victor
				Enumeration<String> fpartenum = ((Vector<String>) _Fparts
						.get(key)).elements();
				int fpartenumCount = 0;
				while (fpartenum.hasMoreElements()) {
					fpartenumCount++;
					if (fileElementCount == fpartenumCount) {
						F = fpartenum.nextElement();
						break;
					}
				}
			}
			if (F == null) {
				continue;
			}
			if (!baseF[k].equalsIgnoreCase(F)) {
				path = path.replace(baseF[k], F); // CB TO DO: ALTER FOR WRIMS 2
			}
			try {
				dataSet = file.read(path);
				// units conversion CFS->TAF
				String[] parts = path.split("/");
				if (parts[3].toLowerCase().indexOf("storage") > -1) {
					dataSet = unitsConversion(dataSet, true);
				} else {
					dataSet = unitsConversion(dataSet, false);
				}
				wasFoundInFile = true; // CB added
			} catch (DSSFileException e) {
				// path not found probably - message given from elsewhere
				// we want to continue with the rest so do nothing
			} catch (HecMathException hme) {

			}
			if (wasFoundInFile) {
				break;
			}
		}
		return dataSet;
	}

	/**
	 * CB added.
	 * 
	 * @param dts
	 * @param nameToPathMap
	 * @param baseF
	 * @param key
	 * @param fileElementCount
	 * @param file
	 * @return
	 */
	HecMath getDataSetForDTS(DerivedTimeSeries dts, Hashtable nameToPathMap,
			String[] baseF, String key, int fileElementCount, DSSFile file) {
		HecMath cumulativeDataSet = null;
		Enumeration<String[]> rows = dts.getRowsCopy().elements();
		while (rows.hasMoreElements()) {
			HecMath dataSet = null;
			String name = (rows.nextElement())[3];
			if (name.trim().equals("")) { // DTS row
				String dtsName = (rows.nextElement())[1]; // CB TODO change 0,
				// 1, 2, and 3
				// indices to
				// DTSTable?
				// constants.
				dataSet = getDataSetForDTS(new DerivedTimeSeries(dtsName),
						nameToPathMap, baseF, key, fileElementCount, file);
			} else { // b-part row
				String path = (String) nameToPathMap.get(name);
				dataSet = loadBasesetForPath(path, baseF, key,
						fileElementCount, file);
			}
			try {
				String op = (rows.nextElement())[0].trim();
				if (op.equals("+")) {
					cumulativeDataSet.add(dataSet);
				} else if (op.equals("-")) {
					cumulativeDataSet.subtract(dataSet);
				} else if (op.equals("*")) {
					cumulativeDataSet.multiply(dataSet);
				} else if (op.equals("/")) {
					cumulativeDataSet.divide(dataSet);
				}
			} catch (HecMathException hme) {
				hme.printStackTrace();
			}
		}
		return cumulativeDataSet;
	}

	HecMath unitsConversion(HecMath dataSet, boolean force) {
		if (DEBUG) {
			System.out.println("target Units: " + _units);
			System.out.println("dataSet Units: " + dataSet.getUnits());
			System.out.println("force: " + force);
		}
		String[] parts = dataSet.getPath().split("/");

		boolean isStorage = parts[3].trim().equalsIgnoreCase("storage");

		// if (isStorage)
		// System.out.print(""); //CB debugging

		if ((_units.equals("TAF") || force || isStorage)
				&& dataSet.getUnits().equals("CFS")) {
			// CB dataSet = adjustMonthlyData(dataSet, 1.9834631 / 1000.);
			dataSet = adjustMonthlyData(dataSet); // CB added constant IV FACTOR
			// so do not need to pass a
			// factor
			dataSet.setUnits("TAF");

		} else if (_units.equals("CFS") && dataSet.getUnits().equals("TAF")
				&& !force && !isStorage) { // CB added section
			dataSet = adjustMonthlyData(dataSet, false); // CB added constant IV
			// FACTOR so do not
			// need to pass a
			// factor
			dataSet.setUnits("CFS");
		}
		return dataSet;
	}

	/**
	 * 
	 * CB added.
	 */
	HecMath adjustMonthlyData(HecMath dataSet) {
		return adjustMonthlyData(dataSet, true);
	}

	// CB HecMath adjustMonthlyData(HecMath dataSet, double factor) {
	/**
	 * 
	 * @param isCFStoTAF
	 *            is true if units are to be converted from CFS to TAF, false if
	 *            from TAF to CFS.
	 */
	HecMath adjustMonthlyData(HecMath dataSet, boolean isCFStoTAF) { // CB added
		// the
		// boolean
		// and
		// made
		// a
		// constant
		// IV
		// factor
		HecTime ht = null;
		try {
			TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet.getData();
			// double[] values = tsc.values;
			double[] nvalues = new double[tsc.values.length];
			int[] times = tsc.times;
			int ndays = 0;
			ht = new HecTime();
			for (int i = 0; i < times.length; i++) {
				ht.set(times[i]);
				// FIX: subtract 1 min
				ht.add(-1);
				ndays = ht.day();
				/*
				 * HecTime BUG: sometimes ht.day() returns a day later than *
				 * should???? I wasted a lot of time on this!!
				 * 
				 * returns ht.day() == 1, ht.hour() == 0 AS OPPOSED TO ht.day()
				 * == 28,30,31 (depending on month), ht.hour() == 24
				 */

				// if (DEBUG) System.out.println(ht.year()+" "+ht.month()+
				// " "+ht.day()+ht.hour()+" "+ht.minute()+" "+ht.second()+" "+times[i]+"
				// "+values[i]);
				// CB nvalues[i] = tsc.values[i] * ndays * factor;
				if (isCFStoTAF) {
					nvalues[i] = tsc.values[i] * ndays / FACTOR;
				} else {
					nvalues[i] = tsc.values[i] / ndays * FACTOR;
				}
			}
			tsc.values = nvalues;
			dataSet.setData(tsc);

		} catch (Exception e) {
			System.out.println("Exception adjustMonthlyData: " + e);
		}
		return dataSet;
	}

	PairedDataContainer getExceedence(TimeSeriesContainer tsc) {

		PairedDataContainer pdc = null;
		try {
			double[] values = tsc.values;

			// System.out.println("value"+values[0]);
			Arrays.sort(values);
			// System.out.println("value"+values[0]);

			double[] xvalues = new double[values.length];
			double[][] yvalues = new double[1][values.length];
			for (int i = 0; i < values.length; i++) {
				xvalues[i] = (double) (values.length - i) / values.length;
				yvalues[0][i] = values[i];
				// System.out.println(xvalues[i]+" "+values[i]);
			}

			// make an exceedence container
			pdc = new PairedDataContainer();

			// pdc.parameter = tsc.parameter;
			pdc.fullName = tsc.fullName;
			pdc.version = tsc.version;
			pdc.location = tsc.location;

			pdc.xOrdinates = xvalues;
			pdc.yOrdinates = yvalues;

			pdc.numberOrdinates = xvalues.length;
			pdc.numberCurves = 1;

			pdc.xunits = "Probability";
			pdc.yunits = tsc.units;

			pdc.xparameter = "Exceedence";
			pdc.yparameter = tsc.parameter;

		} catch (Exception e) {
		}

		return pdc;
	}

	TimeSeriesContainer getMonthlyData(HecMath dataSet, ArrayList months) {
		TimeSeriesContainer ntsc = null;

		try {
			List<Double> lvalues = new ArrayList<Double>();
			List<Integer> ltimes = new ArrayList<Integer>();

			TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet.getData();
			double[] values = tsc.values;
			int[] times = tsc.times;

			HecTime ht = new HecTime();
			// ht.set(tsc.startTime);

			for (int i = 0; i < values.length; i++) {
				// System.out.println(times[i]+" "+ht.month()+" "+months[1]+" "+values[i]);

				ht.set(times[i]);
				// WRONG MONTH FIX: subtract 1 min
				ht.add(-1);

				if (months.contains(ht.month())) {
					ltimes.add(times[i]);
					lvalues.add(values[i]);
					// values[i] = Constants.UNDEFINED;
				}
			}

			double[] nvalues = new double[lvalues.size()];
			int[] ntimes = new int[ltimes.size()];

			for (int i = 0; i < lvalues.size(); i++) {
				ntimes[i] = ltimes.get(i).intValue();
				nvalues[i] = lvalues.get(i).doubleValue();
				// System.out.println(ntimes[i]+" "+nvalues[i]);
			}

			ntsc = (TimeSeriesContainer) tsc.clone();
			ntsc.values = nvalues;
			ntsc.numberValues = nvalues.length;
			ntsc.times = ntimes;
			ntsc.startTime = ntimes[0];
			ntsc.endTime = ntimes[ntimes.length - 1];
			ntsc.interval = -1; // irregular time-series

		} catch (Exception e) {
		}

		return ntsc;

	}

	TimeSeriesContainer getAnnualTotals(TimeSeriesContainer tsc) {
		TimeSeriesContainer ntsc = null;
		HecTime ht = null;
		double[] values = null;
		int[] times = null;

		try {
			List<Double> lvalues = new ArrayList<Double>();
			List<Integer> ltimes = new ArrayList<Integer>();

			values = tsc.values;
			times = tsc.times;

			ht = new HecTime();

			int year = 0;
			int year_prev = 0;
			// int time_prev = times[0];
			double total = 0.0;
			// CB added block to handle cases where first month is not first
			// month of year type (i.e., partial years not including first
			// month)
			// CB maximum of 12 periods to check (for monthly data only) TODO
			// fix if daily or other is added
			int firstMonth = 24;
			int firstPossibleMonth;
			if (_annualTypeMode == MessagePanel.WATERYEAR) {
				firstPossibleMonth = 10;
			} else if (_annualTypeMode == MessagePanel.CALENDAR_YEAR) {
				firstPossibleMonth = 1;
			} else if (_annualTypeMode == MessagePanel.FEDERAL_CONTRACT_YEAR) {
				firstPossibleMonth = 3;
			} else {
				return null;
			}

			HecTime htTemp = new HecTime();
			int month;
			for (int j = 0; j < 12; j++) {
				htTemp.set(times[j]);
				// WRONG MONTH FIX: subtract 1 min
				htTemp.add(-1);
				month = htTemp.month();
				if (month < firstPossibleMonth) {
					month += 12;
				}
				if (month < firstMonth) {
					firstMonth = month;
				}
			}
			if (firstMonth > 12) {
				firstMonth -= 12;
			}

			for (int i = 0; i < values.length; i++) {
				ht.set(times[i]);
				// if (DEBUG)
				// System.out.println(ht.year()+" "+ht.month()+" "+values[i]);

				// WRONG MONTH FIX: subtract 1 min
				ht.add(-1);
				if (_annualTypeMode == MessagePanel.WATERYEAR) { // CB added
					// annual
					// types
					if ((year_prev == 0) && (ht.month() != firstMonth)) {
						continue; // CB added to not use incomplete first
						// wateryear (i.e., data start is NOT Oct.)
					}
					if ((ht.month() == 10) || (ht.month() == 11)
							|| (ht.month() == 12)) {
						year = ht.year() + 1;
					} else {
						year = ht.year();
					}
					if ((year_prev != 0) && (year > year_prev)) {
						lvalues.add(total);
						// End of water year
						ht.set("30 September " + year_prev, "24:00");
						ltimes.add(ht.value());
						total = 0.0;
					}
					total += values[i];
					year_prev = year;
				} else if (_annualTypeMode == MessagePanel.CALENDAR_YEAR) { // CB
					// added
					// this
					// section
					if ((year_prev == 0) && (ht.month() != firstMonth)) {
						continue; // CB added to not use incomplete first
						// calendar year (i.e., data start is NOT
						// Jan.)
					}
					year = ht.year();
					if ((year_prev != 0) && (year > year_prev)) {
						lvalues.add(total);
						// End of calendar year
						ht.set("31 December " + year_prev, "24:00");
						ltimes.add(ht.value());
						total = 0.0;
					}
					total += values[i];
					year_prev = year;
				} else if (_annualTypeMode == MessagePanel.FEDERAL_CONTRACT_YEAR) { // CB
					// added
					// this
					// section
					if ((year_prev == 0) && (ht.month() != firstMonth)) {
						continue; // CB added to not use incomplete first
						// wateryear (i.e., data start is NOT March)
					}
					if ((ht.month() == 1) || (ht.month() == 2)) {
						year = ht.year() - 1;
					} else {
						year = ht.year();
					}
					if ((year_prev != 0) && (year > year_prev)) {
						lvalues.add(total);
						// End of contract year
						if (SchematicUtils.daysInMonth(1, year) == 29) {
							ht.set("29 February " + year, "24:00");
						} else {
							ht.set("28 February " + year, "24:00");
						}
						ltimes.add(ht.value());
						total = 0.0;
					}
					total += values[i];
					year_prev = year;
				}
			}
			// CB added block to handle cases where last month is not last month
			// of year type (i.e., partial years not including last month)
			// CB because we need to add the last "year" of data if it is a
			// "full" year
			// CB maximum of 12 periods to check (for monthly data only) TODO
			// fix if daily or other is added
			int lastMonth = 0;
			int lastPossibleMonth = firstPossibleMonth - 1;
			if (lastPossibleMonth < 1) {
				lastPossibleMonth += 12;
			}
			htTemp = new HecTime();
			for (int j = values.length - 1; j >= 0; --j) {
				htTemp.set(times[j]);
				// WRONG MONTH FIX: subtract 1 min
				htTemp.add(-1);
				month = htTemp.month();
				if (month > lastPossibleMonth) {
					month -= 12;
				}
				if (month > lastMonth) {
					lastMonth = month;
				}
			}
			if (lastMonth < 1) {
				lastMonth += 12;
			}
			if (ht.month() == lastMonth) { // if a "full" year (actually if the
				// last year's data size is the same
				// size as an interior year's data
				// size)
				lvalues.add(total);
				if (lastPossibleMonth == 9) {
					ht.set("30 September " + year_prev, "24:00");
				} else if (lastPossibleMonth == 12) {
					ht.set("31 December " + year_prev, "24:00");
				} else if (lastPossibleMonth == 2) {
					if (SchematicUtils.daysInMonth(1, year) == 29) {
						ht.set("29 February " + year, "24:00");
					} else {
						ht.set("28 February " + year, "24:00");
					}
				}
				ltimes.add(ht.value());
				total = 0.0;
			}

			// Build the annual time-series container
			double[] nvalues = new double[lvalues.size()];
			int[] ntimes = new int[ltimes.size()];

			for (int i = 0; i < lvalues.size(); i++) {
				ntimes[i] = ltimes.get(i).intValue();
				nvalues[i] = lvalues.get(i).doubleValue();
				// System.out.println(ntimes[i]+" "+nvalues[i]);
			}

			if (DEBUG) {
				System.out.println("interval: " + tsc.interval + " parameter: "
						+ tsc.parameter + " fullName: " + tsc.fullName
						+ " supplementalInfo: " + tsc.supplementalInfo
						+ " watershed: " + tsc.watershed + " watershed: "
						+ tsc.watershed + " version: " + tsc.version
						+ " subVersion: " + tsc.subVersion + " location: "
						+ tsc.location

				);
			}

			ntsc = (TimeSeriesContainer) tsc.clone();
			ntsc.values = nvalues;
			ntsc.numberValues = nvalues.length;
			ntsc.times = ntimes;
			ntsc.startTime = ntimes[0];
			ntsc.endTime = ntimes[ntimes.length - 1];
			ntsc.fullName = tsc.fullName.replace("1MON", "1YEAR");
			ntsc.type = "ANN-TOT";
			ntsc.interval = -1; // irregular time-series

		} catch (Exception e) {
		}

		return ntsc;
	}

	TimeSeriesContainer getMonthlyAverages(TimeSeriesContainer tsc) {
		TimeSeriesContainer ntsc = null;
		HecTime ht = null;
		double[] values = null;
		int[] times = null;

		try {
			List<Double>[] lvalues; // = new ArrayList<Double>[];
			List<Integer>[] ltimes; // = new ArrayList<Integer>[];

			values = tsc.values;
			times = tsc.times;

			ht = new HecTime();

			// int year = 0;
			// int year_prev = 0;
			// int time_prev = times[0];
			int firstMonthIndex = -1;
			double[] totals;
			// CB added block to handle cases where first month is not first
			// month of year type (i.e., partial years not including first
			// month)
			// CB maximum of 12 periods to check (for monthly data only) TODO
			// fix if daily or other is added
			int firstMonth = 24;
			int firstPossibleMonth;
			if (_annualTypeMode == MessagePanel.WATERYEAR) {
				firstPossibleMonth = 10;
			} else if (_annualTypeMode == MessagePanel.CALENDAR_YEAR) {
				firstPossibleMonth = 1;
			} else if (_annualTypeMode == MessagePanel.FEDERAL_CONTRACT_YEAR) {
				firstPossibleMonth = 3;
			} else {
				return null;
			}

			HecTime htTemp = new HecTime();
			int month;
			// int monthOrder[];
			Hashtable<Integer, Double> monthlyTotals = new Hashtable<Integer, Double>(); // Hashtable
			// of
			// <index,
			for (int j = 0; j < 12; j++) {
				htTemp.set(times[j]);
				// WRONG MONTH FIX: subtract 1 min
				htTemp.add(-1);
				month = htTemp.month();
				if (month < firstPossibleMonth) {
					month += 12;
				}
				if (month < firstMonth) {
					firstMonth = month;
				}
			}
			if (firstMonth > 12) {
				firstMonth -= 12;
			}

			int numUniqueMonths = 0;
			int firstDataMonth = 0;
			boolean stopCount = false;
			Hashtable<Integer, Integer> monthToIndexMapping = new Hashtable<Integer, Integer>();
			for (int j = 0; j < 12; j++) { // TODO nedd to consider consider
				// less than 12 months ON THIS LINE
				// ???????
				htTemp.set(times[j]);
				// WRONG MONTH FIX: subtract 1 min
				htTemp.add(-1);
				if (j == 0) {
					firstDataMonth = htTemp.month();
				} else if (firstDataMonth == htTemp.month()) {
					break;
				}
				monthToIndexMapping.put(htTemp.month(), j);
				++numUniqueMonths;
				if (htTemp.month() == firstMonth) {
					firstMonthIndex = j;
					// break;
				}
			}

			lvalues = new ArrayList[numUniqueMonths];
			ltimes = new ArrayList[numUniqueMonths];
			for (int i = 0; i < lvalues.length; ++i) {
				lvalues[i] = new ArrayList<Double>();
				ltimes[i] = new ArrayList<Integer>();
			}

			for (int i = 0; i < values.length; ++i) {
				ht.set(times[i]);
				// if (DEBUG)
				// System.out.println(ht.year()+" "+ht.month()+" "+values[i]);
				// WRONG MONTH FIX: subtract 1 min
				ht.add(-1);
				int index = monthToIndexMapping.get(ht.month());
				lvalues[index].add(values[i]);
				ltimes[index].add(ht.value());
			}

			// Build the annual time-series container
			double[] averages = new double[lvalues.length];
			int[] ntimes = new int[ltimes.length];

			for (int i = 0; i < lvalues.length; ++i) {
				for (int j = 0; j < ltimes[i].size(); ++j) {
					// int total = 0;
					if (i >= lvalues.length - numUniqueMonths - 1) {
						;
					}
					ntimes[i] = ltimes[i].get(j);
					averages[i] += lvalues[i].get(j).doubleValue();
					// System.out.println(ntimes[i]+" "+nvalues[i]);
				}
				averages[i] /= lvalues[i].size();
			}

			if (DEBUG) {
				System.out.println("interval: " + tsc.interval + " parameter: "
						+ tsc.parameter + " fullName: " + tsc.fullName
						+ " supplementalInfo: " + tsc.supplementalInfo
						+ " watershed: " + tsc.watershed + " watershed: "
						+ tsc.watershed + " version: " + tsc.version
						+ " subVersion: " + tsc.subVersion + " location: "
						+ tsc.location);
			}
			ntsc = (TimeSeriesContainer) tsc.clone();
			// ntsc.values = nvalues;
			ntsc.values = averages;
			// ntsc.numberValues = nvalues.length;
			ntsc.numberValues = averages.length;
			ntsc.times = ntimes;
			ntsc.startTime = ntimes[0];
			ntsc.endTime = ntimes[ntimes.length - 1];
			// ntsc.fullName = tsc.fullName.replace("1MON", "1YEAR");
			ntsc.type = "MON-AVG";
			ntsc.interval = -1; // irregular time-series

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ntsc;
	}

	// CB added
	public void setTableCommaPreference(boolean showCommas) {
		_userPrefs.putBoolean("showTableCommas", showCommas);
	}

	// CB added
	public boolean getTableCommaPreference() {
		return _userPrefs.getBoolean("showTableCommas", false);
	}

	/**
	 * CB added.
	 * 
	 * @return
	 */
	public boolean isTAFSelected() {
		return (_units.equalsIgnoreCase("taf") ? true : false);
	}

	// CB added following getters and setter which are necessary now with
	// schematic value box data updating when individual dss files opened

	public HashMap<Integer, Object> getDssFiles() {
		return _DssFiles;
	}

	void setDssFiles(HashMap<Integer, Object> dssFiles) {
		_DssFiles = dssFiles;
	}

	public HashMap<Integer, Object> getUncheckedDssFiles() {
		return _unselectedDssFiles;
	}

	void setUncheckedDssFiles(HashMap<Integer, Object> unselectedDssFiles) {
		_unselectedDssFiles = unselectedDssFiles;
	}

	public HashMap<Integer, Object> getFparts() {
		return _Fparts;
	}

	public void setFparts(HashMap<Integer, Object> fparts) {
		_Fparts = fparts;
	}

	public int[] getKeys() {
		return _keys;
	}

	public void setKeys(int[] keys) {
		_keys = keys;
	}

	public Vector<String> getPaths() {
		return _paths;
	}

	public void setPaths(Vector<String> paths) {
		_paths = paths;
	}

	public Hashtable<String, Object> getVariables() {
		return _variables;
	}

	public void setVariables(Hashtable<String, Object> variables) {
		_variables = variables;
	}
}
