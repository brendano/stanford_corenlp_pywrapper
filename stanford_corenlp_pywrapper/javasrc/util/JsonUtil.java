package util;

import java.io.IOException;
import java.util.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.*;

import com.google.common.collect.Multiset;

import util.misc.Pair;

/** simplified wrapper functions for the Jackson JSON library 
 * this is half-baked, still learning the right way to use the library
 */
public class JsonUtil {
	
	public static ObjectMapper om;
	static {
		om = new ObjectMapper();
	}
	
	public static void main(String args[]) {
		List<String> x = toList(args[0], String.class);
		U.p(x);
	}
	
	public static String getTextDefault(JsonNode ob, String keyname, String defaultValue) {
		return ob.has(keyname) ? ob.get(keyname).asText() : defaultValue;
	}
	
	//////////////////////////////////////
	
	// toList() derived from
	// http://stackoverflow.com/questions/9942475/convert-json-to-multiple-objects-using-jackson
	
	public static <T> ArrayList<T> toList(String jsonString, final Class<T> type) {
		try {
			return om.readValue(jsonString, TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, type));
		} catch (IOException e) {
			return null;
		}
	}

	public static <T> ArrayList<T> toList(JsonNode jsonNode, final Class<T> type) {
		try {
			return om.readValue(jsonNode, TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, type));
		} catch (IOException e) {
			return null;
		}
	}

	public static <T> ObjectNode toJson(Multiset<T> counts) {
		ObjectNode jmap = newObject();
		for (Multiset.Entry<T> e : counts.entrySet()) {
			jmap.put(e.getElement().toString(), e.getCount());
		}
		return jmap;
	}
	
    public static <T> JsonNode toJson(final List<T> data) {
    	ArrayNode jlist = new ObjectMapper().createArrayNode();
    	for (T elt : data) {
    		jlist.add( toJson(elt) );
    	}
    	return jlist;
    }

    public static <S, T> JsonNode toJson(final Pair<S,T> pair) {
        try {
        	List<Object> x = new ArrayList<>();
        	x.add( (Object) pair.first);
        	x.add( (Object) pair.second);
            return new ObjectMapper().valueToTree(x);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    ///////// from Play framework below
    
    /**
     * Convert an object to JsonNode.
     *
     * @param data Value to convert in Json.
     */
    public static JsonNode toJson(final Object data) {
        try {
            return om.valueToTree(data);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
   
    /**
     * Convert a JsonNode to a Java value
     *
     * @param json Json value to convert.
     * @param clazz Expected Java value type.
     */
    public static <A> A fromJson(JsonNode json, Class<A> clazz) {
        try {
            return om.treeToValue(json, clazz);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Creates a new empty ObjectNode.
     */ 
    public static ObjectNode newObject() {
        return om.createObjectNode();
    }
    
    /**
     * Convert a JsonNode to its string representation.
     */
    public static String stringify(JsonNode json) {
        return json.toString();
    }
    
    /**
     * Parse a String representing a json, and return it as a JsonNode.
     */
    public static JsonNode parse(String src) {
        try {
            return om.readValue(src, JsonNode.class);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

	public static JsonNode readJson(String jsonStr) throws JsonProcessingException, IOException {
		return om.readTree(jsonStr);
	}

	public static JsonNode readJsonNX(String jsonStr) {
		try {
			return om.readTree(jsonStr);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
