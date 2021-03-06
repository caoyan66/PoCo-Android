package org.wordpress.android.poco.policy.staticAnalysis.scanPolicies;

public final class ParsFlgConsts {
    //for _currFlag
    public static final String IF_CONDITION = "IF_CONDITION";
    public static final String THEN_BRANCH  = "THEN_BRANCH";
    public static final String ELSE_BRANCH  = "ELSE_BRANCH";
    public static final String ENDOFIF      = "ENDOFIF";

    public static final String SWITCH       = "SWITCH";
    public static final String SWITCHCASE   = "SWITCHCASE";
    public static final String END_SWITCH   = "END_SWITCH";

    public static final String TRY = "TRY";
    public static final String TRY_RESOURCE = "TRY_RESOURCE";
    public static final String TRY_BLOCK = "TRY_BLOCK";
    public static final String CATCH_BLOCK = "TRY_CATCH";
    public static final String TRY_FINAL = "TRY_FINAL";
    public static final String ENDTRY = "ENDTRY";

    public static final String WHILE = "WHILE_CONDITION";
    public static final String WHILE_BLOCK = "WHILE_BLOCK";
    public static final String END_WHILE = "END_WHILE";

    public static final String FOR = "FOR";
    public static final String FOR_CONDITION = "FOR_CONDITION";
    public static final String FOR_BLOCK = "FOR_BLOCK";
    public static final String END_FOR = "END_FOR";

    public static final String ENHANCED_FOR = "ENHANCED_FOR";
    public static final String ENHANCED_FOR_BLOCK = "ENHANCED_FOR_BLOCK";
    public static final String END_ENHANCED_FOR = "END_ENHANCED_FOR";

    public static final String[] ALL_FLAGS = new String[]{IF_CONDITION, THEN_BRANCH, ELSE_BRANCH, ENDOFIF,
            SWITCH, END_SWITCH, SWITCHCASE,
            TRY, TRY_RESOURCE, TRY_BLOCK, CATCH_BLOCK, TRY_FINAL, ENDTRY,
            WHILE, WHILE_BLOCK,END_WHILE,
            FOR, FOR_CONDITION, FOR_BLOCK, END_FOR,
            ENHANCED_FOR,ENHANCED_FOR_BLOCK, END_ENHANCED_FOR};

    public static final boolean IS_STATEMENT_FLAG(String str){
    	if(str==null) return false;
    	
    	for(String flag: ALL_FLAGS) 
    		if(flag.equals(str))  return true; 
    	
    	return false;
    }
   
    public static boolean isIFCondition(String str) 	{ return str.equals(IF_CONDITION) ? true : false; }
    public static boolean isEndIf(String str)       	{ return str.equals(ENDOFIF)  	  ? true : false; }
    public static boolean isTryStatement(String str)	{ return str.equals(TRY)    	  ? true : false; }
    public static boolean isCatchStatement(String str)	{ return str.equals(CATCH_BLOCK)  ? true : false; }
    
  //common throwable: Casting; Arrays; vector, ArrayList, HashMap,IO ; etc
    public static final String[] THROWABLE_IO_EXCEPTION = 
    		new String[] {"java.io.FileWriter.<init>(*)", "java.io.FileWriter.write(*)", 
    				      "java.io.FileWriter.flush(null)", "java.io.FileWriter.close(null),",
    				      "java.io.File.createNewFile(null)"};
    
    public static final String[] CLOSEABLE_RESOURCES = new String[] {"java.io.FileWriter.<init>(*)"};
}
