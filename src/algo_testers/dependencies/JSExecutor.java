package algo_testers.dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;

import algo_testers.search_dependencies.FileSystemUtils;


public class JSExecutor {
	
	private ScriptEngine engine = null;
	
	public JSExecutor(String initialContextJS) throws ScriptException {
		ScriptEngineManager mgr = new ScriptEngineManager();
	    engine = mgr.getEngineByName("nashorn");
	    if (StringUtils.isNotBlank(initialContextJS))
	    	engine.eval(initialContextJS);
	}
	
	public Object executeJs(String js) throws ScriptException {
		Object retVal = null;
		if (StringUtils.isNotBlank(js))
			retVal = engine.eval(js);
		return retVal;
	}
	
	public Object executeJs(String js, String jsFunctionName, Object... args) throws ScriptException, NoSuchMethodException {
		executeJs(js);
		Invocable inv = (Invocable)engine;
		return inv.invokeFunction(jsFunctionName, args);
	}
	
	
	public static void main(String[] arg) throws ScriptException, IOException, NoSuchMethodException {
		StringBuilder sb = new StringBuilder();
				
		
//		File f = new File(".");
//		System.out.println(f.getAbsolutePath());
		
		sb.append(FileSystemUtils.readTextFromFile("./src/algo_testers/dependencies/lcsDiff.js"));
		sb.append(FileSystemUtils.readTextFromFile("./src/algo_testers/dependencies/hl_algo.js"));
		
		JSExecutor jsEval = new JSExecutor(sb.toString());
		
		String sent1 = "Business Day means each day that is not a Saturday, Sunday or other day on which banking institutions in Houston, Texas, New York, New York or another place of payment are authorized or required by law to close.";
		
		String sent2 = "Business Day means, with respect to any series of Securities, any day other than a day on which federal or state banking institutions in the Borough of Manhattan, the City of New York, or in the city of the Corporate Trust Office of the Trustee, are authorized or obligated by law, executive order or regulation to close.";
		
		ArrayList<String> arList = new ArrayList<>(); 
		Collections.addAll(arList, "day", "not", "day", "bank", "institut", "anoth", "plac", "pay", "auth", "requir", "law","clos");
		
		ArrayList<String> arList2 = new ArrayList<>(); 
		Collections.addAll(arList2, "any", "any", "day", "day", "fed", "stat", "bank", "institut", "city", "auth", "oblig","law","execut","ord","reg","clos");
		
//		List<String> arr1 = {}; 
		
//		String[] arr2 = {}; 
		
		Object val = jsEval.executeJs(null, "hl_main",  sent1,sent2,arList,arList2);



		
//		String js = "function testM(arg1) { return arg1; }; testM('hello pk')";
		
//		Object val = jsEval.executeJs(js);
		System.out.println(val);
		System.out.println("Done");
		
//		val = jsEval.executeJs(null, "testM", "hello pk");		// pass method name, and args
		
//		System.out.println(val);
	}
	
	
	
	
	
	
	
	
}
