package wrimsv2_plugin.debugger.launcher;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import wrimsv2_plugin.debugger.core.DebugCorePlugin;
import wrimsv2_plugin.debugger.exception.WPPException;

public class WPPConfigTab extends AbstractLaunchConfigurationTab {
	
	private Button wpButton;
	private Button xaButton;
	private Button allowSvTsInitButton;
	private Button DSSHDF5ConversionButton;
	private ILaunchConfiguration currConfiguration;
	
	@Override
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout();
		topLayout.verticalSpacing = 0;
		topLayout.numColumns = 7;
		comp.setLayout(topLayout);
		comp.setFont(font);
		
		createVerticalSpacer(comp, 3);
		
		Label wpLabel = new Label(comp, SWT.NONE);
		wpLabel.setText("&WRESL Plus:");
		GridData gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan=2;
		wpLabel.setLayoutData(gd);
		wpLabel.setFont(font);
		
		wpButton = new Button(comp, SWT.CHECK);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		wpButton.setLayoutData(gd);
		wpButton.setFont(font);
		wpButton.addSelectionListener(new SelectionListener(){
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();	
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();			
			}
		});
		
		Label xaLabel = new Label(comp, SWT.NONE);
		xaLabel.setText("&XA Free Limited License:");
		gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan=2;
		xaLabel.setLayoutData(gd);
		xaLabel.setFont(font);
		
		xaButton = new Button(comp, SWT.CHECK);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		xaButton.setLayoutData(gd);
		xaButton.setFont(font);
		xaButton.setEnabled(false);
		xaButton.addSelectionListener(new SelectionListener(){
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();	
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();			
			}
		});
		
		Label allowSvTsInitLabel = new Label(comp, SWT.NONE);
		allowSvTsInitLabel.setText("&Allow SV File Provide Timeseries Initial Data:");
		gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan=2;
		allowSvTsInitLabel.setLayoutData(gd);
		allowSvTsInitLabel.setFont(font);
		
		allowSvTsInitButton = new Button(comp, SWT.CHECK);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		allowSvTsInitButton.setLayoutData(gd);
		allowSvTsInitButton.setFont(font);
		allowSvTsInitButton.addSelectionListener(new SelectionListener(){
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();	
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();			
			}
		});
		
		DSSHDF5ConversionButton = new Button(comp, SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		DSSHDF5ConversionButton.setLayoutData(gd);
		DSSHDF5ConversionButton.setText("DSS HDF5 Conversion");
		DSSHDF5ConversionButton.setFont(font);
		DSSHDF5ConversionButton.addSelectionListener(new SelectionListener(){
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				String launchFileName=currConfiguration.getLocation().toOSString();
				try {
					String conversionFileName="DssHDF5Converter_Launch.bat";
					FileWriter conversionFile = new FileWriter(conversionFileName);
					PrintWriter out = new PrintWriter(conversionFile);
					out.println("@echo off");
					out.println();
					out.println("set path=lib;%path%");
					out.println("set temp_wrims2=jre\\bin");
					out.println();
					out.println("jre\\bin\\java -Xmx4096m -Xss1024K -Duser.timezone=UTC -Djava.library.path=\"lib\" -cp \"lib\\external;lib\\WRIMSv2.jar;lib\\jep-3.8.2.jar;lib\\jna-3.5.1.jar;lib\\commons-io-2.1.jar;lib\\XAOptimizer.jar;lib\\lpsolve55j.jar;lib\\coinor.jar;lib\\gurobi.jar;lib\\heclib.jar;lib\\jnios.jar;lib\\jpy.jar;lib\\misc.jar;lib\\pd.jar;lib\\vista.jar;lib\\guava-11.0.2.jar;lib\\javatuples-1.2.jar;lib\\kryo-2.24.0.jar;lib\\minlog-1.2.jar;lib\\objenesis-1.2.jar;lib\\jarh5obj.jar;lib\\jarhdf-2.10.0.jar;lib\\jarhdf5-2.10.0.jar;lib\\jarhdfobj.jar;lib\\slf4j-api-1.7.5.jar;lib\\slf4j-nop-1.7.5.jar;lib\\mysql-connector-java-5.1.42-bin.jar;lib\\sqljdbc4-2.0.jar\" wrimsv2.hdf5.DSSHDF5Converter -launch="+launchFileName);
					out.close();
					Runtime.getRuntime().exec(new String[] {"cmd.exe", "/c", "start", "/w", conversionFileName}, 
							null, null); 
				} catch (IOException ex) {
					WPPException.handleException(ex);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}

		});
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(DebugCorePlugin.ATTR_WPP_WRESLPLUS, "no");
		configuration.setAttribute(DebugCorePlugin.ATTR_WPP_ALLOWSVTSINIT, "no");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		currConfiguration=configuration;
		String wreslPlus = null;
		try {
			wreslPlus = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_WRESLPLUS, "no");
			if (wreslPlus.equalsIgnoreCase("yes")){
				wpButton.setSelection(true);
			}else{
				wpButton.setSelection(false);
			}
		} catch (CoreException e) {
			WPPException.handleException(e);
		}
		
		String freeXA = null;
		try {
			freeXA = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_FREEXA, "no");
			if (freeXA.equalsIgnoreCase("yes")){
				xaButton.setSelection(true);
			}else{
				xaButton.setSelection(false);
			}
		} catch (CoreException e) {
			WPPException.handleException(e);
		}
		
		String allowSvTsInit = null;
		try {
			allowSvTsInit = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_ALLOWSVTSINIT, "no");
			if (allowSvTsInit.equalsIgnoreCase("yes")){
				allowSvTsInitButton.setSelection(true);
			}else{
				allowSvTsInitButton.setSelection(false);
			}
		} catch (CoreException e) {
			WPPException.handleException(e);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String wreslPlus="no";
		if (wpButton.getSelection()){
			wreslPlus="yes";
		}else{
			wreslPlus="no";
		}
		configuration.setAttribute(DebugCorePlugin.ATTR_WPP_WRESLPLUS, wreslPlus);
		
		String freeXA="no";
		if (xaButton.getSelection()){
			freeXA="yes";
		}else{
			freeXA="no";
		}
		configuration.setAttribute(DebugCorePlugin.ATTR_WPP_FREEXA, freeXA);
		
		String allowSvTsInit="no";
		if (allowSvTsInitButton.getSelection()){
			allowSvTsInit="yes";
		}else{
			allowSvTsInit="no";
		}
		configuration.setAttribute(DebugCorePlugin.ATTR_WPP_ALLOWSVTSINIT, allowSvTsInit);
	}

	@Override
	public String getName() {
		return "Configuration";
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		return true;
	}
}
