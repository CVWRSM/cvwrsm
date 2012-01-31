package wrimsv2.commondata.wresldata_v2;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.ValueEvaluatorParser;


public class Alias implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String scope;
	public String kind;
	public String units;
	public String expression;
	public ValueEvaluatorParser expressionParser;
	public String fromWresl;
	public IntDouble data;
	public Set<String> dependants;
	public Set<String> neededVarInCycleSet;
	public boolean needVarFromEarlierCycle;

	public Alias(){
		scope=Param.undefined;
		kind=Param.undefined;
		units=Param.undefined;
		expression=Param.undefined;
		fromWresl=Param.undefined;
		dependants = new HashSet<String>();
		expressionParser = null;
		data = null;
		neededVarInCycleSet = new HashSet<String>();
		needVarFromEarlierCycle = false;
	}
	
	public void setData(IntDouble data){
		this.data=data;
	}
	
	public IntDouble getData(){
		return data;
	}
}
	