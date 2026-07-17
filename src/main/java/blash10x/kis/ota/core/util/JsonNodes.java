package blash10x.kis.ota.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author myungsik.sung@gmail.com
 */
public class JsonNodes {
  public static final ObjectMapper objectMapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .registerModule(new JavaTimeModule());

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

  public static ObjectNode createEmptyObjectNode() {
    return objectMapper.createObjectNode();
  }

  public static ArrayNode createEmptyArrayNode() {
    return objectMapper.createArrayNode();
  }

  public static ArrayNode createArrayNode(List<String> values) {
    ArrayNode arrayNode = createEmptyArrayNode();
    values.forEach(arrayNode::add);
    return arrayNode;
  }

  public static String toString(TreeNode value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T toValue(String content, Class<T> valueType) {
    if (content == null) {
      return null;
    }
    try {
      return objectMapper.readValue(content, valueType);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T toValue(String content, TypeReference<T> valueTypeRef) {
    if (content == null) {
      return null;
    }
    try {
      return objectMapper.readValue(content, valueTypeRef);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T toValue(TreeNode source, Class<T> valueType) {
    if (source == null) {
      return null;
    }
    try {
      return objectMapper.treeToValue(source, valueType);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T toValue(TreeNode source, TypeReference<T> valueTypeRef) {
    if (source == null) {
      return null;
    }
    try {
      return objectMapper.treeToValue(source, valueTypeRef);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T toValue(Map<?, ?> source, Class<T> valueType) {
    return source != null ? objectMapper.convertValue(source, valueType) : null;
  }

  public static JsonNode toJsonNode(String content) {
    if (content == null) {
      return null;
    }
    try {
      return objectMapper.readTree(content);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static JsonNode toJsonNode(Object source) {
    return source != null ? objectMapper.valueToTree(source) : null;
  }

  public static Map<String, Object> toMap(Object source) {
    return source != null ? objectMapper.convertValue(source, MAP_TYPE_REFERENCE) : Collections.emptyMap();
  }

  public static <T> T toValue(File src, Class<T> valueType) {
    try {
      return objectMapper.readValue(src, valueType);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void toFile(File resultFile, Object value) {
    try {
      objectMapper.writeValue(resultFile, value);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
