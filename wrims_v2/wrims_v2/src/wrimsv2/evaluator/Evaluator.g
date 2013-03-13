grammar Evaluator;

options {
  language = Java;
}

@header {
  package wrimsv2.evaluator;
    
  import org.antlr.runtime.ANTLRFileStream;
  import org.antlr.runtime.CharStream;
  import org.antlr.runtime.CommonTokenStream;
  import org.antlr.runtime.RecognitionException;
  import org.antlr.runtime.TokenStream;
  
  import java.util.HashMap;
  import wrimsv2.components.Error;
  import wrimsv2.components.IntDouble;
}

@lexer::header {
  package wrimsv2.evaluator;
}

@members {
  public static IntDouble evalValue;
  public static EvalExpression evalExpression;
  public static EvalConstraint evalConstraint;
  public static boolean evalCondition;
  
  @Override
  public void reportError(RecognitionException e) {
       Error.addEvaluationError(getErrorMessage(e, tokenNames));
  }
}

evaluator returns [String result]
	:	 goalInput|
	expressionInput |
	softConstraint|
	conditionInput 
	;

///////////////////
/// input rules ///
///////////////////

goalInput: 'g:' constraintStatement {evalConstraint = $constraintStatement.ec;};
expressionInput: 'v:' expressionCollection{evalValue=Evaluation.expressionInput($expressionCollection.ee);};
softConstraint: 's:' expressionCollection{evalExpression=$expressionCollection.ee;};
conditionInput: 'c:' conditionStatement {evalCondition=$conditionStatement.result;};

///////////////////
/// basic rules ///
///////////////////
lhsrhs: expression|CONSTRAIN;

//weight	:	(allnumber|(allnumber '*' TAFCFS)) (('+' allnumber)|('-' allnumber))?;

units: IDENT|(IDENT '/' IDENT);

fileName
  : (':'|';'|'.'|'|'|SYMBOLS|'-'|'+'|BACKSLASH|IDENT|IDENT1|IDENT2|INTEGER|FLOAT|usedKeywords)+{
  }
  ;
  
externalFile
  : (';'|'.'|'|'|SYMBOLS|'-'|'+'|INTEGER|FLOAT|IDENT|usedKeywords)+
  ;
	
text	:	LETTER (LETTER | DIGIT )*;
	
expressionCollection returns [EvalExpression ee]
	:	(expression{ee=$expression.ee;})
	|(tableSQL){ee=$tableSQL.ee;}
	|(timeseriesWithUnits)
	|((timeseries){ee=$timeseries.ee;})
	| sumExpression {ee=$sumExpression.ee;}
	|(UPPERUNBOUNDED{ee=new EvalExpression(new IntDouble(1e38,true));})
	|(LOWERUNBOUNDED{ee=new EvalExpression(new IntDouble(-1e38,true));})
	;

func returns[EvalExpression ee]: 
  (max_func{ee=$max_func.ee;})|
  (min_func{ee=$min_func.ee;})|
  (int_func{ee=$int_func.ee;})|
  (real_func{ee=$real_func.ee;})|
  (abs_func{ee=$abs_func.ee;})|
  (exp_func{ee=$exp_func.ee;})|
  (log_func{ee=$log_func.ee;})|
  (log10_func{ee=$log10_func.ee;})|
  (pow_func{ee=$pow_func.ee;})|
  (mod_func{ee=$mod_func.ee;})
  ;

mod_func returns[EvalExpression ee]
  : MOD '(' (e1=expression) (';' (e2=expression)) ')'{
     ee=Evaluation.mod($e1.ee, $e2.ee);
  }
  ;

max_func returns[EvalExpression ee] 
	: MAX '(' (e1=expression){ee=$e1.ee;}(';' (e2=expression{
     ee=Evaluation.max(ee, $e2.ee);
  }))+ ')'
	;

min_func returns[EvalExpression ee]
	: MIN '(' (e1=expression){ee=$e1.ee;}(';' (e2=expression{
     ee=Evaluation.min(ee, $e2.ee);
  }))+ ')'
	;
	
int_func returns[EvalExpression ee]
  : INT '(' (e=expression) ')'{
     ee=Evaluation.intFunc($e.ee);
  }
  ;

real_func returns[EvalExpression ee]
  : REAL '(' (e=expression) ')'{
    ee=Evaluation.realFunc($e.ee);
  }
  ;
  
abs_func returns[EvalExpression ee]
  : ABS '(' (e=expression) ')'{
     ee=Evaluation.abs($e.ee);
  }
  ;

exp_func returns[EvalExpression ee]
  : EXP '(' (e=expression) ')'{
     ee=Evaluation.exp($e.ee);
  }
  ;

log_func returns[EvalExpression ee]
  : LOG '(' (e=expression) ')'{
     ee=Evaluation.log($e.ee);
  }
  ;

log10_func returns[EvalExpression ee]
  : LOG10 '(' (e=expression) ')'{
    ee=Evaluation.log10($e.ee);
  }
  ;
  
pow_func returns[EvalExpression ee]
  : POW '(' (e1=expression) (';' (e2=expression)) ')'{
     ee=Evaluation.pow($e1.ee, $e2.ee);
  }
  ;
  
range_func returns [boolean result]
  : RANGE '(' MONTH ';' m1=MONTH_CONST ';' m2=MONTH_CONST ')' {Evaluation.range($m1.text, $m2.text);};

timeseriesWithUnits 
	: 'timeseries' 'kind' '=' partC 'units' '=' IDENT 
	;

timeseries returns [EvalExpression ee]
	: 'timeseries' {ee=Evaluation.timeseries();}
	;
	

	
partC: 	(IDENT|IDENT1|usedKeywords) ('-' (IDENT|IDENT1|usedKeywords))*;
  
usedKeywords: YEAR|MONTH|MONTH_CONST|DAY|PASTMONTH|RANGE|TAFCFS|DAYSIN|SUM|MAX|MIN|INT|REAL|ABS|EXP|LOG|LOG10|POW|MOD|SELECT|FROM|GIVEN|USE|WHERE
|CONSTRAIN|ALWAYS|NAME|DVAR|CYCLE|FILE|CONDITION|INCLUDE|LOWERBOUND|UPPERBOUND|INTEGERTYPE|UNITS|CONVERTUNITS|TYPE|OUTPUT
|CASE|ORDER|EXPRESSION|LHSGTRHS|LHSLTRHS|WEIGHT|FUNCTION|FROM_WRESL_FILE|UPPERUNBOUNDED|LOWERUNBOUNDED|AND|OR|NOT;

tableSQL returns [EvalExpression ee] @init{String table=null; String select=null; String use=null; HashMap<String, Number> given=null; HashMap<String, Number> where=null;}
	: SELECT ((i1=IDENT{select=$i1.text;})|(u1=usedKeywords{select=$u1.text;})) FROM i2=IDENT{table=$i2.text;} 
	  (GIVEN a=assignStatement{given=new HashMap<String, Number>(); given.put($a.assignIdent, $a.value);})? (USE i3=IDENT{use=$i3.text;})? 
	  (where_items{where=$where_items.where;})? {ee=Evaluation.tableSQL(table, select, where, given, use);}	  
	;

where_items returns [HashMap<String, Number> where]
	:	 WHERE  (r1=whereStatement{where=new HashMap<String, Number>(); where.put($r1.whereIdent, $r1.value);})
	        (';' r=whereStatement{where.put($r.whereIdent, $r.value);})*
	;


upperbound:	IDENT|allnumber|(allnumber '*' TAFCFS);

lowerbound:	IDENT|allnumber|(allnumber '*' TAFCFS);

//sumExpression 
//  : SUM '(' I '=' e1=expression_sum ';' e2=expression_sum (';' (s='-')? INTEGER )? ')' e3=expression
//  ;
//term_sum: (MONTH|MONTH_CONST|PASTMONTH|I|INTEGER|'(' expression_sum ')');

//unary_sum : ('-')? term_sum ;
//add_sum  :  unary_sum(('+' | '-') unary_sum)* ;
//expression_sum: add_sum ;

//sumExpression was redesign. If not work, switch back to the original design above

sumExpression returns [EvalExpression ee] @init{String s="";}
  : SUM '(' IDENT{Evaluation.sumExpression_IDENT($IDENT.text);} '=' e1=expression ';' e2=expression (';' (('-'{s=s+"-";})? INTEGER {s=s+$INTEGER.text;}))? ')' {Evaluation.initSumExpression($e1.ee, $e2.ee, s);} e3=expression{ee=Evaluation.sumExpression($e3.ee, $e3.text);}
  ;

term returns [EvalExpression ee]
	:	(IDENT {ee=Evaluation.term_IDENT($IDENT.text);})
	| (FLOAT {ee=Evaluation.term_FLOAT($FLOAT.text);}) 
	| ('(' (e=expression) ')' {ee=$e.ee;})
	| pastCycleValue{ee=Evaluation.term_knownTS($pastCycleValue.result);}
	| function{ee=$function.ee;}
	| func{ee=$func.ee;}
	| (INTEGER {ee=Evaluation.term_INTEGER($INTEGER.text);}) 
	| tafcfs_term{ee=$tafcfs_term.ee;}
	| YEAR{ee=Evaluation.term_YEAR();}
	| MONTH{ee=Evaluation.term_MONTH();}
	| DAY{ee=Evaluation.term_DAY();}
	| MONTH_CONST{ee=Evaluation.term_MONTH_CONST($MONTH_CONST.text);}
	| PASTMONTH{ee=Evaluation.term_PASTMONTH($PASTMONTH.text);}
	| DAYSIN{ee=Evaluation.daysIn();}
	| (SVAR{ee=Evaluation.term_SVAR($SVAR.text.replace("{","").replace("}",""));}) 
	| ARRAY_ITERATOR{ee=Evaluation.term_ARRAY_ITERATOR();}
	| '(' sumExpression ')'{ee=$sumExpression.ee;}
	;
	
tafcfs_term returns [EvalExpression ee]: TAFCFS ('(' expression ')')? {
    ee=Evaluation.tafcfs_term($TAFCFS.text, $expression.ee);
};
	  
pastCycleValue returns[IntDouble result]
  : (p1=pastCycleNoTimeArray{return $p1.result;})|(p2=pastCycleTimeArray{return $p2.result;})|(p3=pastCycleIndexNoTimeArray{return $p3.result;})|(p4=pastCycleIndexTimeArray{return $p4.result;})
  ;

pastCycleNoTimeArray returns [IntDouble result]
  : i1=IDENT '[' i2=IDENT ']'{result=Evaluation.pastCycleNoTimeArray($i1.text,$i2.text);}
  ; 
  
pastCycleTimeArray returns [IntDouble result]
  : i1=IDENT '[' i2=IDENT ']' '(' e1=expression ')' {result=Evaluation.pastCycleTimeArray($i1.text,$i2.text, $e1.ee);}
  ; 
  
pastCycleIndexNoTimeArray returns [IntDouble result]
  : i1=IDENT '[' '-' index=INTEGER ']'{result=Evaluation.pastCycleIndexNoTimeArray($i1.text,-Integer.parseInt($index.text));}
  ; 
  
pastCycleIndexTimeArray returns [IntDouble result]
  : i1=IDENT '[' '-' index=INTEGER ']' '(' e1=expression ')' {result=Evaluation.pastCycleIndexTimeArray($i1.text,-Integer.parseInt($index.text), $e1.ee);}
  ; 

function returns [EvalExpression ee]
  : (n=noArgFunction{ee=$n.ee;})|(a=argFunction{ee=$a.ee;})
  ;

noArgFunction returns [EvalExpression ee]
  : IDENT '(' ')' {ee=Evaluation.noArgFunction($IDENT.text);};

argFunction returns [EvalExpression ee] @init{ArrayList<EvalExpression> eeArray = new ArrayList<EvalExpression>();}
  : IDENT '(' (e1=expression {eeArray.add($e1.ee);}) (';' (e2=expression{eeArray.add($e2.ee);}))* ')'{ee=Evaluation.argFunction($IDENT.text,eeArray);};
  	
unary returns [EvalExpression ee] 
	:	(s=('+'|'-'))? term{ee=Evaluation.unary($s.text, $term.ee);
	};
	
allnumber 
	:	('-')? number;

mult returns [EvalExpression ee]  
	:	(u1=unary {ee=$u1.ee;}) (s=('*'| '/') (u2=unary){
	   if ($s.text.equals("*")){
	     ee=Evaluation.mult(ee, $u2.ee);
	   }else{
	     ee=Evaluation.divide(ee, $u2.ee);
	   }
  })*
	;
	
add  returns [EvalExpression ee]
	:	(m1=mult {ee=$m1.ee;}) ((s=('+'|'-')) (m2=mult){
     if ($s.text.equals("+")){
       ee=Evaluation.add(ee, $m2.ee);
     }else{
       ee=Evaluation.substract(ee, $m2.ee);
     }
	})*
	;

expression returns [EvalExpression ee]  
	:	i=add {$ee=$add.ee;} 
	;

relation
	: '=='
	| '<'
	| '>'
	| '>='
	| '<='
	;	

whereStatement returns [String whereIdent, Number value]
  : ((i=IDENT{$whereIdent=$i.text;})|(u=usedKeywords{$whereIdent=$u.text;})) '=' expression{$value=Evaluation.assignWhereStatement($expression.ee);} 
  ;

conditionStatement returns [boolean result]
	:	((r=relationUnary{result=$r.result;})|ALWAYS{result=true;})
	;

relationUnary returns [boolean result]
  : (n=NOT)? r=relationStatementSeries{
      if ($n==null){
        return $r.result;
      }else{
        if ($r.result){
          return false;
        }else{
          return true;
        }
      }
  }
  ; 
	
relationStatementSeries returns [boolean result] 
  : r1=relationRangeStatement {result=$r1.result;} 
    (((s=AND)|(s=OR)) r2=relationRangeStatement {result=Evaluation.relationStatementSeries(result, $r2.result, $s.text);})* ;

relationRangeStatement returns [boolean result]
  : (r1=relationStatement{result=$r1.result;})|(r2=range_func{result=$r2.result;})
  ;  

relationStatement returns [boolean result] 
	: (	( expression relation expression ) => (e1=expression) relation (e2=expression) {result=Evaluation.relationStatement($e1.ee, $e2.ee, $relation.text);} )
	| ( ( '('relationUnary')'  ) => '('r2=relationUnary')' {result=$r2.result;} )
	;

constraintStatement returns [EvalConstraint ec]
  : e1=expression ((s='=')|(s='>')|(s='<')) e2=expression{ec=Evaluation.constraintStatement($e1.ee, $e2.ee, $s.text);}
  ;

assignStatement returns [String assignIdent, Number value]  
  : IDENT '=' expression {$assignIdent=$IDENT.text; $value=Evaluation.assignWhereStatement($expression.ee);} 
  ;

number
	: INTEGER 
	| FLOAT
	;

MULTILINE_COMMENT : '/*' .* '*/' {$channel = HIDDEN;} ;

fragment LETTER : ('a'..'z' | 'A'..'Z') ;
fragment DIGIT : '0'..'9';
fragment SYMBOLS : '_';	

BACKSLASH : '\\';	 	 

INTEGER : DIGIT+ ;
FLOAT : INTEGER? '.' INTEGER 
	  | INTEGER '.' 
	  ;

//I: 'i';
YEAR: 'wateryear';
MONTH: 'month';
DAY: 'day';
MONTH_CONST: 'jan'|'feb'|'mar'|'apr'|'may'|'jun'|'jul'|'aug'|'sep'|'oct'|'nov'|'dec';
PASTMONTH: 'prevjan'|'prevfeb'|'prevmar'|'prevapr'|'prevmay'|'prevjun'|'prevjul'|'prevaug'|'prevsep'|'prevoct'|'prevnov'|'prevdec';
RANGE: 'range';

TAFCFS: 'taf_cfs'|'cfs_taf'|'cfs_af'|'af_cfs';
DAYSIN: 'daysin'|'daysinmonth';

ARRAY_ITERATOR : '$m' ;

AND: '.and.';
OR: '.or.';
NOT: '.not.';

SUM: 'sum';
MAX : 'max';
MIN : 'min';
INT : 'int';
REAL: 'real';
ABS: 'abs';
EXP: 'exp';
LOG: 'log';
LOG10: 'log10';
POW: 'pow';
MOD: 'mod';

SELECT: 'select';
FROM: 'from';
GIVEN: 'given';
USE: 'use';
WHERE : 'where';

CONSTRAIN: 'constrain';
ALWAYS: 'always';

NAME: 'name';
DVAR: 'dvar';
CYCLE: 'cycle';
FILE: 'file';
CONDITION: 'condition';
INCLUDE: 'include';
LOWERBOUND: 'lower_bound';
UPPERBOUND: 'upper_bound';
INTEGERTYPE: 'integer';
UNITS: 'units';
CONVERTUNITS: 'convert_to_units';
TYPE: 'type';
OUTPUT: 'output';
CASE: 'case';
ORDER: 'order';
EXPRESSION: 'expression';
LHSGTRHS: 'lhs_gt_rhs';
LHSLTRHS: 'lhs_lt_rhs';
WEIGHT: 'weight';
FUNCTION: 'function';
FROM_WRESL_FILE: 'from_wresl_file';
UPPERUNBOUNDED: 'upper_unbounded';
LOWERUNBOUNDED: 'lower_unbounded';

SVAR: '{' IDENT '}';
IDENT : LETTER (LETTER | DIGIT | SYMBOLS )*;
IDENT1 : DIGIT (LETTER | DIGIT | SYMBOLS )*;
IDENT2 : SYMBOLS (LETTER | DIGIT | SYMBOLS )*; 

WS : (' ' | '\t' | '\n' | '\r' | '\f')+ {$channel = HIDDEN;};
COMMENT : '!' .* ('\n'|'\r') {$channel = HIDDEN;};


	
