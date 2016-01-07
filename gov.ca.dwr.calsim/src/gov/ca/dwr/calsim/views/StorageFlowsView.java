package gov.ca.dwr.calsim.views;

import gov.ca.dwr.calsim.CalSimPluginCore;
import gov.ca.dwr.calsim.ReportCheckBoxAction;
import hec.io.DataContainer;

import java.awt.Component;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.eclipse.swt.widgets.Composite;

public class StorageFlowsView extends AbstractCalSimView{

	public StorageFlowsView(){
	
	}

	
	public void createPartControl(Composite parent){
		super.createPartControl(parent);
		JPanel panel = (JPanel)CalSimPluginCore.swix.find("presets");
		contentPane.add(new JScrollPane(panel));
		Component[] components = panel.getComponents();
		for (int i = 0; i < components.length; i++) {
			if (components[i] instanceof JCheckBox) {
				JCheckBox c = (JCheckBox) components[i];
				c.addActionListener(new ReportCheckBoxAction());
			}
		}
	}
	
}
