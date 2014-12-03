package wrimsv2_plugin.debugger.dialog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import wrimsv2_plugin.debugger.core.DebugCorePlugin;
import wrimsv2_plugin.debugger.exception.WPPException;
import wrimsv2_plugin.debugger.menuitem.EnableMenus;
import wrimsv2_plugin.debugger.view.WPPVarDetailView;

public class WPPLoadZipFileDialog extends Dialog {
	private int flag=1;
	private Text fileText;
	private	String fileName="";
	private final int BUFFER_SIZE = 4096;
	private File headDir;
	private boolean projectExist;
	
	public WPPLoadZipFileDialog(Shell parentShell) {
		super(parentShell);
		// TODO Auto-generated constructor stub
	}

	public void open(int flag){
		this.flag=flag;
		create();
		getShell().setSize(600, 200);
		getShell().setText("Load Zip File");
		open();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		FillLayout fl = new FillLayout(SWT.VERTICAL);
		dialogArea.setLayout(fl);
		fl.marginWidth=10;
		fl.marginHeight=15;
		
		Label label1=new Label(dialogArea, SWT.NONE);
		label1.setText("Please select a zip file to load:");
		
		Composite fileSelection = new Composite(dialogArea, SWT.NONE);
		GridLayout layout = new GridLayout(15, true);
		fileSelection.setLayout(layout);
		fileText = new Text(fileSelection, SWT.SINGLE | SWT.BORDER);
		GridData gd1 = new GridData(GridData.FILL_HORIZONTAL);
		gd1.horizontalSpan = 12;
		fileText.setLayoutData(gd1);
		fileText.setText(fileName);
		
		Button browserButton = new Button(fileSelection, SWT.PUSH);
		browserButton.setText("Browser");
		GridData gd2 = new GridData(GridData.FILL_HORIZONTAL);
		gd2.horizontalSpan = 3;
		browserButton.setLayoutData(gd2);
		browserButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final IWorkbench workbench=PlatformUI.getWorkbench();
				workbench.getDisplay().asyncExec(new Runnable(){
					public void run(){
						Shell shell=workbench.getActiveWorkbenchWindow().getShell();
						FileDialog dlg=new FileDialog(shell, SWT.OPEN);
						dlg.setFilterNames(new String[]{"Zip Files (*.zip)", "All Files (*.*)"});
						dlg.setFilterExtensions(new String[]{"*.zip", "*.*"});
						dlg.setFileName(fileText.getText());
						String file=dlg.open();
						if (file !=null){
							fileText.setText(file);
						}
					}
				});
			}
		});
		
		return dialogArea;
	}
	
	@Override
	public void okPressed(){
		fileName=fileText.getText();
		if (unzipFile()){
			loadStudy();
		}else{
			showUnzipFailed();
		}
		close();
	}
	
	public boolean unzipFile(){
		File zipFile = new File(fileName);
		if (zipFile.exists()){
			try{
				ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile));
				ZipEntry entry = zipIn.getNextEntry();
				String destPath=zipFile.getParent();
				String headPath=zipFile.getPath().replaceFirst("[.][^.]+$", "");
				headDir = new File(headPath);
				if (!headDir.exists()){
					headDir.mkdir();
				}
				while (entry != null) {
					String filePath = destPath + File.separator + entry.getName();
					if (!entry.isDirectory()) {
						extractFile(zipIn, filePath);
					} else {
						File dir = new File(filePath);
						dir.mkdir();
					}
					zipIn.closeEntry();
					entry = zipIn.getNextEntry();
				}
				zipIn.close();
				return true;
			}catch (Exception e){
				WPPException.handleException(e);
				return false;
			}
		}else{
			return false;
		}
	}
	
	private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}
	
	public void loadStudy(){
		projectExist=false;
		
		walk(headDir);

		if (!projectExist){
			showProjectNotFound();
		}
	}
		
	public void walk(File file){
		if (projectExist) return;
		
		File[] list = file.listFiles();
        if (list == null) {
        	return;
        }

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk(f);
            }
            else {
                if (f.getName().toLowerCase().equals(".project")){
                	IProjectDescription description;
					try {
						description = ResourcesPlugin
								   .getWorkspace().loadProjectDescription(new Path(f.getAbsolutePath()));
						IProject project = ResourcesPlugin.getWorkspace()
	                			   .getRoot().getProject(description.getName());
	                			project.create(description, null);
	                			project.open(null);
	                	projectExist=true;
	                	return;
					} catch (CoreException e) {
					}
                }
            }
        }
	}
	
	public void showProjectNotFound(){
		final IWorkbench workbench=PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(new Runnable(){
			public void run(){
				Shell shell=workbench.getActiveWorkbenchWindow().getShell();
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING);
				messageBox.setText("Warning");
				messageBox.setMessage("Project/study is not found in the zip file.");
				messageBox.open();
			}
		});
	}
	
	public void showUnzipFailed(){
		final IWorkbench workbench=PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(new Runnable(){
			public void run(){
				Shell shell=workbench.getActiveWorkbenchWindow().getShell();
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR);
				messageBox.setText("Error");
				messageBox.setMessage("File unzip failed");
				messageBox.open();
			}
		});
	}
}