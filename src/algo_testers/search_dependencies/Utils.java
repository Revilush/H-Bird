package algo_testers.search_dependencies;

public class Utils {

	public static boolean isFalseOrNull(Boolean val) {
		return (null == val  ||  !val);
	}

	public static boolean isTrue(Boolean val) {
		return !isFalseOrNull(val);
	}

		
}
