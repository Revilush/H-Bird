package algo_testers.search_dependencies;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JSonUtils {
	public static ObjectMapper jsonObjectMapper = new ObjectMapper(); // can reuse, share globally
	public static ObjectMapper jsonObjectMapperPrettyPrinter = new ObjectMapper(); // can reuse, share globally
	static {
		jsonObjectMapperPrettyPrinter.enable(SerializationFeature.INDENT_OUTPUT);
		
		jsonObjectMapper = jsonObjectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		//jsonObjectMapper = jsonObjectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);	//TODO: throwing err - look later
		jsonObjectMapper = jsonObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		//jsonObjectMapper = jsonObjectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
	}

	public static ObjectMapper xmlObjectMapper = new ObjectMapper(); // can reuse, share globally
	static {
	   // xmlObjectMapper.getSerializationConfig().with(secondaryIntropsector);	//.setAnnotationIntrospector(pair);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> json2Map(String json) throws JsonParseException, JsonMappingException, IOException {
		if (null == json)
			return null;
		 return jsonObjectMapper.readValue(json, Map.class);
	}
	public static <K,V> Map<K, V> json2MapOfType(String json, Class<K> keyClass, Class<V> valueClass) throws JsonParseException, JsonMappingException, IOException {
		if (null == json)
			return null;
		 return jsonObjectMapper.readValue(json, new TypeReference<Map<K,V>>() {});
	}
	public static Object json2Object(String json) throws JsonParseException, JsonMappingException, IOException {
		 return json2Object(json, Object.class);
	}
	public static <T> T json2Object(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		if (null == json)
			return null;
		 return jsonObjectMapper.readValue(json, clazz);
	}
	public static <T> T jsonFile2Object(File jsonFile, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		 return jsonObjectMapper.readValue(jsonFile, clazz);
	}
	public static <T> T inputStream2Object(InputStream is, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		if (null == is)
			return null;
		 return jsonObjectMapper.readValue(is, clazz);
	}
	public static <T> T bytes2Object(byte[] bytes, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		if (null == bytes)
			return null;
		 return jsonObjectMapper.readValue(bytes, clazz);
	}
	
	public static <T> List<T> json2List(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		if (null == json)
			return null;
		return jsonObjectMapper.readValue(json, new TypeReference<List<T>>() {});
	    //List<PlanDetailJson> basePlans = mapper.readValue(responseJson, new TypeReference<List<PlanDetailJson>>() {});
	}
	@SuppressWarnings("rawtypes")
	public static <T> List<T> json2ListOfClass(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		if (null == json)
			return null;
		List<Map> maps = jsonObjectMapper.readValue(json, new TypeReference<List<Map>>() {});
		List<T> objs = new ArrayList<T>();
		for (Map m: maps)
			objs.add(map2Object(m, clazz));
		return objs;
	}

	public static <T> T json2Type(String json, TypeReference<T> type) throws JsonParseException, JsonMappingException, IOException {
		//JavaType listType = jsonObjectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
		//return (ArrayList<T>) jsonObjectMapper.readValue(json, listType);
		if (null == json)
			return null;
		return jsonObjectMapper.readValue(json, type);
	}

	
	public static void object2File(File targetFile, Object obj) throws JsonGenerationException, JsonMappingException, IOException {
		jsonObjectMapper.writeValue(targetFile, obj);
	}
	public static void object2Stream(OutputStream os, Object obj) throws JsonGenerationException, JsonMappingException, IOException {
		jsonObjectMapper.writeValue(os, obj);
	}
	public static void object2Writer(Writer writer, Object obj) throws JsonGenerationException, JsonMappingException, IOException {
		jsonObjectMapper.writeValue(writer, obj);
	}
	public static String object2JsonString(Object obj) throws JsonProcessingException {
		if (null == obj)
			return null;
		return jsonObjectMapper.writeValueAsString(obj);
	}
	public static byte[] object2Bytes(Object obj) throws JsonGenerationException, JsonMappingException, IOException {
		return jsonObjectMapper.writeValueAsBytes(obj);
	}
	
	
	@SuppressWarnings("unchecked")
	public static Map<Object, Object> object2Map(Object srcObj) {
		if (null == srcObj)
			return null;
		Map<Object,Object> map = jsonObjectMapper.convertValue(srcObj, Map.class);
		return map;
	}
	@SuppressWarnings("rawtypes")
	public static <T> T map2Object(Map srcMap, Class<T> clazz) {
		if (null == srcMap)
			return null;
		T obj = jsonObjectMapper.convertValue(srcMap, clazz);
		return obj;
	}
	
	public static String prettyPrint(Object object) throws JsonProcessingException {
		if (null == object)
			return null;
		return jsonObjectMapperPrettyPrinter.writeValueAsString(object);
	}
	public static void prettyPrintInto(Object json, File file) throws IOException {
		jsonObjectMapper.writerWithDefaultPrettyPrinter().writeValue(file, json);
	}
	
	public static String object2XML(Object obj) throws JsonProcessingException {
		return xmlObjectMapper.writeValueAsString(obj);
	}

	public static String escapeJson(String text) {
		if (StringUtils.isBlank(text))
			return "";
		// escape the 'escape' char, double quotes and any new-line/form-feed/back etc chars
		return text.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll("[\r\n\f\b]", "");
	}
}
