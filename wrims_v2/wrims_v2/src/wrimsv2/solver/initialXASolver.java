package wrimsv2.solver;

import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;

import com.sunsetsoft.xa.Optimizer;

public class initialXASolver {
	public initialXASolver(){
		ControlData.xasolver=new Optimizer(25000);
		ControlData.xasolver.setActivationCodes( 234416483 , 19834525 ) ;
		ControlData.xasolver.openConnection();
		ControlData.xasolver.setModelSize(100, 100);
		ControlData.xasolver.setCommand("MAXIMIZE Yes MUTE yes FORCE No wait no matlist v set visible no");
		//ControlData.xasolver.setCommand("set sortName Yes FileName d:\\temp Output v2%d.log MatList V MPSX Yes ToRcc Yes");    //rcc code
		//ControlData.xasolver.setCommand( "FileName  "+FilePaths.mainDirectory+"  Output "+FilePaths.mainDirectory+"\\xa.log set sortName Yes MatList V MPSX Yes ToRcc Yes set debug Yes  ListInput Yes")    //xa debug ;
		if (ControlData.solverName.equalsIgnoreCase("XALOG") ) ControlData.xasolver.setCommand( "FileName  "+FilePaths.mainDirectory+"  Output "+FilePaths.mainDirectory+"\\xa.log set sortName W MPSX Yes ToRcc Yes") ;
	}
}
