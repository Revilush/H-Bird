package contracts;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ContractTypeIdentifier {
	
	private static final String attrName_contractNameAlgo = "contractNameAlgo";
	private static final String attrName_contractLongName = "contractLongName";
	private static final String attrName_fSize = "fSize";
	

	/**
	 * Tells if the given meta-data satisfies requirements of any of the kType names given in possibleKTypes.
	 * @param metaData meta data of a contract.
	 * @param possibleKTypes list of contract-types to identify from.
	 * @return kType name (from the provided list) that matches the meta-data, else null. 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public static String getMatchingContractType(Map<String, Object> metaData, List<String> possibleKTypes) 
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method mth;
		Object methodCallResp;
		for (String kt : possibleKTypes) {
			mth = getMethodByName(ContractTypeIdentifier.class, "is"+kt);
			if (null == mth) {
				//throw new RuntimeException("Method not found: is" + kt);		//TODO: should it throw error here?
				System.err.println("Method not found: is" + kt);
				continue;
			}
			methodCallResp = mth.invoke(null, metaData);
			if (null != methodCallResp  &&  (methodCallResp instanceof Boolean)  &&  Boolean.TRUE.equals(methodCallResp) )
				return kt;
		}
		return null;
	}
	
	
	
	/**
	 * Just for initial testing..
	 */
	public static void main(String[] arg) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Map<String, Object> metaData = new HashMap<>();
		metaData.put(attrName_contractNameAlgo, "agreement to test mortgaged collateral junk indenture Trust and more text");
		metaData.put(attrName_fSize, 80000);
		
		String[] kTypes = { "Collateral_and_Security_Agreements" , "Test-kType"};
		System.out.println(getMatchingContractType(metaData, Arrays.asList(kTypes)));
	}
	
	// **********************************************************************************************
	// **********************************************************************************************
	
	private static Method getMethodByName(Class<?> clazz, String methodName) {
		Method[] methods = clazz.getMethods();
		for (Method m: methods) {
			if (m.getName().equals(methodName)) {
				return m;
			}
		}
		return null;
	}
	
	// **********************************************************************************************
		/*
		 * Below all individual methods check if given meta-data satisfies that corresponding kType conditions.
		 * 
		 * ###### Method Name/Signature Convention: 	boolean is{kType}(Map<String,Object>) 		 ###### 
		 * 
		 */
	// **********************************************************************************************
	
	
	/**
	 * Based on procedure/filename: masterIdx_Collateral_and_Security_Agreements.sql
	 * @param metaData
	 * @return
	 */
	public static boolean isCollateral_and_Security_Agreements(Map<String, Object> metaData) {
		/*
(
	(  contractNameAlgo RLIKE 'collateral|security' and contractNameAlgo RLIKE 'trust|agreement' )
 or	(  contractLongName RLIKE 'collateral|security' and contractLongName RLIKE 'trust|agreement' )
)
AND
contractNameAlgo not rlike 'english|holder|represent|restrict|forth|mortgag|paper|private|provision|pursuant|execu|purchas|loan| credit|rents|subjec|negativ' 
AND
contractLongName not rlike 'english|holder|represent|restrict|forth|mortgag|paper|private|provision|pursuant|execu|purchas|loan| credit|rents|subjec|negativ' 
AND fSize>75000
		 */
		String contractNameAlgo = (String)metaData.get(attrName_contractNameAlgo);
		String contractLongName = (String)metaData.get(attrName_contractLongName);
		int fSize = (Integer)metaData.get(attrName_fSize);
		String nameAlgo_LongName = new StringBuilder(StringUtils.defaultIfBlank(contractNameAlgo,"")).append(" ").append(StringUtils.defaultIfBlank(contractLongName,"")).toString();
		// minimum conditions
		if (fSize <= 75000  ||  
				nameAlgo_LongName.matches("(?i).*?(english|holder|represent|restrict|forth|mortgag|paper|private|provision|pursuant|execu|purchas|loan| credit|rents|subjec|negativ).*")  
				) {
			return false;
		}
		// one last to test
		if ( (nameAlgo_LongName.matches("(?i).*?(collateral|security).*")  &&  nameAlgo_LongName.matches("(?i).*?(trust|agreement).*")) ) {
			return true;
		}
		// its a fail
		return false;
	}
	
	
}
