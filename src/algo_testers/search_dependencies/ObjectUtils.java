package algo_testers.search_dependencies;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectUtils {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map deepMergeMaps(Map<?,?> map2Merge, Map<?,?> map2MergeInto, boolean uniqueValuesInLists) {
		if (null == map2Merge  ||  map2Merge.size() == 0)
			return map2MergeInto;
		Map map = map2MergeInto;
		Object v1, v2;
		for (Object k : map2Merge.keySet()) {
			v1 = map2Merge.get(k);
			if (map.containsKey(k)) {
				// merge deep now
				v2 = map.get(k);
				if (v1 instanceof Map) {
					if (((Map)v1).size() > 0)
						map.put(k, deepMergeMaps((Map)v1, (Map)v2, uniqueValuesInLists));
				} else if (v1 instanceof List) {
					if (((List)v1).size() > 0)
						map.put(k, deepMergeLists((List)v1, (List)v2, uniqueValuesInLists));
				} else {
					// replace value
					map.put(k, v1);
				}
			} else {
				// new key - insert now
				map.put(k, v1);
			}
		}
		return map;
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List deepMergeLists(List<?> list2Merge, List<?> list2MergeInto, boolean uniqueValuesInLists) {
		if (null == list2Merge  ||  list2Merge.size() == 0)
			return list2MergeInto;
		List list = list2MergeInto;
		Object v;
		for (int i=0; i < list2Merge.size(); i++) {
			v = list2Merge.get(i);
			if (v instanceof List) {
				if ( ((List)v).size() > 0)
					list.set(i, deepMergeLists((List)v, (List)list.get(i), uniqueValuesInLists));
			} else if (v instanceof Map) {
				list.set(i, deepMergeMaps((Map)v, (Map)list.get(i), uniqueValuesInLists) );
			} else {
				if (list.indexOf(v) < 0)
					list.add(v);
			}
		}
		if (uniqueValuesInLists) {
			Set set = new LinkedHashSet(list);
			list = new ArrayList(set);
		}
		return list;
	}
	
}
