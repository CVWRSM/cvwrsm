package wvscript.app;

import java.util.prefs.Preferences;

public class UserPrefs {
	
  static Preferences prefs = Preferences.userNodeForPackage(UserPrefs.class);	
  static final String RUNDIR = "run_dir";
  static final String CONFIG = "config";
  
  
  public static void main(String args[]) throws Exception {


    prefs.put("A", "a");
    prefs.put("B", "b");
    prefs.put("C", "c");
   // prefs.putInt(RUNDIR, 123); // int
    prefs.put(RUNDIR, "c:\\"); 
    
    for (String s : prefs.keys()) {
        System.out.print(s + ":"+prefs.get(s, "not_found"));
      }
    
    System.out.print("thisdef" + ":"+prefs.get("thisdef", "not_found"));
    
  }
}