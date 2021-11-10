/*
 * Copyright (c) 2021 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.json;

import static com.fasterxml.jackson.databind.node.JsonNodeType.OBJECT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.airbyte.commons.jackson.MoreMappers;
import io.airbyte.commons.stream.MoreStreams;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jsons {

  private static final Logger LOGGER = LoggerFactory.getLogger(Jsons.class);
  public static final String SECRET_MASK = "******";

  // Object Mapper is thread-safe
  private static final ObjectMapper OBJECT_MAPPER = MoreMappers.initMapper();
  private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer(new JsonPrettyPrinter());

  public static <T> String serialize(final T object) {
    try {
      return OBJECT_MAPPER.writeValueAsString(object);
    } catch (final JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T deserialize(final String jsonString, final Class<T> klass) {
    try {
      return OBJECT_MAPPER.readValue(jsonString, klass);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static JsonNode deserialize(final String jsonString) {
    try {
      return OBJECT_MAPPER.readTree(jsonString);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> Optional<T> tryDeserialize(final String jsonString, final Class<T> klass) {
    try {
      return Optional.of(OBJECT_MAPPER.readValue(jsonString, klass));
    } catch (final IOException e) {
      return Optional.empty();
    }
  }

  public static Optional<JsonNode> tryDeserialize(final String jsonString) {
    try {
      return Optional.of(OBJECT_MAPPER.readTree(jsonString));
    } catch (final IOException e) {
      return Optional.empty();
    }
  }

  public static <T> JsonNode jsonNode(final T object) {
    return OBJECT_MAPPER.valueToTree(object);
  }

  public static JsonNode emptyObject() {
    return jsonNode(Collections.emptyMap());
  }

  public static <T> T object(final JsonNode jsonNode, final Class<T> klass) {
    return OBJECT_MAPPER.convertValue(jsonNode, klass);
  }

  public static <T> T object(final JsonNode jsonNode, final TypeReference<T> typeReference) {
    return OBJECT_MAPPER.convertValue(jsonNode, typeReference);
  }

  public static <T> Optional<T> tryObject(final JsonNode jsonNode, final Class<T> klass) {
    try {
      return Optional.of(OBJECT_MAPPER.convertValue(jsonNode, klass));
    } catch (final Exception e) {
      return Optional.empty();
    }
  }

  public static <T> Optional<T> tryObject(final JsonNode jsonNode, final TypeReference<T> typeReference) {
    try {
      return Optional.of(OBJECT_MAPPER.convertValue(jsonNode, typeReference));
    } catch (final Exception e) {
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T clone(final T object) {
    return (T) deserialize(serialize(object), object.getClass());
  }

  public static byte[] toBytes(final JsonNode jsonNode) {
    return serialize(jsonNode).getBytes(Charsets.UTF_8);
  }

  public static Set<String> keys(final JsonNode jsonNode) {
    if (jsonNode.isObject()) {
      return Jsons.object(jsonNode, new TypeReference<Map<String, Object>>() {}).keySet();
    } else {
      return new HashSet<>();
    }
  }

  public static List<JsonNode> children(final JsonNode jsonNode) {
    return MoreStreams.toStream(jsonNode.elements()).collect(Collectors.toList());
  }

  public static String toPrettyString(final JsonNode jsonNode) {
    try {
      return OBJECT_WRITER.writeValueAsString(jsonNode) + "\n";
    } catch (final JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static JsonNode navigateTo(JsonNode node, final List<String> keys) {
    for (final String key : keys) {
      node = node.get(key);
    }
    return node;
  }

  public static void replaceNestedString(final JsonNode json, final List<String> keys, final String replacement) {
    replaceNested(json, keys, (node, finalKey) -> node.put(finalKey, replacement));
  }

  public static void replaceNestedInt(final JsonNode json, final List<String> keys, final int replacement) {
    replaceNested(json, keys, (node, finalKey) -> node.put(finalKey, replacement));
  }

  private static void replaceNested(final JsonNode json, final List<String> keys, final BiConsumer<ObjectNode, String> typedReplacement) {
    Preconditions.checkArgument(keys.size() > 0, "Must pass at least one key");
    final JsonNode nodeContainingFinalKey = navigateTo(json, keys.subList(0, keys.size() - 1));
    typedReplacement.accept((ObjectNode) nodeContainingFinalKey, keys.get(keys.size() - 1));
  }

  public static Optional<JsonNode> getOptional(final JsonNode json, final String... keys) {
    return getOptional(json, Arrays.asList(keys));
  }

  public static Optional<JsonNode> getOptional(JsonNode json, final List<String> keys) {
    for (final String key : keys) {
      if (json == null) {
        return Optional.empty();
      }

      json = json.get(key);
    }

    return Optional.ofNullable(json);
  }

  public static String getStringOrNull(final JsonNode json, final String... keys) {
    return getStringOrNull(json, Arrays.asList(keys));
  }

  public static String getStringOrNull(final JsonNode json, final List<String> keys) {
    final Optional<JsonNode> optional = getOptional(json, keys);
    return optional.map(JsonNode::asText).orElse(null);
  }

  public static int getIntOrZero(final JsonNode json, final String... keys) {
    return getIntOrZero(json, Arrays.asList(keys));
  }

  public static int getIntOrZero(final JsonNode json, final List<String> keys) {
    final Optional<JsonNode> optional = getOptional(json, keys);
    return optional.map(JsonNode::asInt).orElse(0);
  }

  public static JsonNode flattenConfig(final JsonNode config) {
    return flattenConfig((ObjectNode) Jsons.emptyObject(), (ObjectNode) config);
  }

  private static ObjectNode flattenConfig(final ObjectNode flatConfig, final ObjectNode configToFlatten) {
    for (final String key : Jsons.keys(configToFlatten)) {
      if (configToFlatten.get(key).getNodeType() == OBJECT) {
        flattenConfig(flatConfig, (ObjectNode) configToFlatten.get(key));
      } else {
        if (flatConfig.has(key)) {
          LOGGER.warn(String.format("Config's key '%s' already exists", key));
        }
        flatConfig.set(key, configToFlatten.get(key));
      }
    }
    return flatConfig;
  }

  /**
   * By the Jackson DefaultPrettyPrinter prints objects with an extra space as follows: {"name" :
   * "airbyte"}. We prefer {"name": "airbyte"}.
   */
  private static class JsonPrettyPrinter extends DefaultPrettyPrinter {

    // this method has to be overridden because in the superclass it checks that it is an instance of
    // DefaultPrettyPrinter (which is no longer the case in this inherited class).
    @Override
    public DefaultPrettyPrinter createInstance() {
      return new DefaultPrettyPrinter(this);
    }

    // override the method that inserts the extra space.
    @Override
    public DefaultPrettyPrinter withSeparators(final Separators separators) {
      _separators = separators;
      _objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
      return this;
    }

  }

  public static JsonNode mergeJsons(final ObjectNode mainConfig, final ObjectNode fromConfig) {
    return mergeJsons(mainConfig, fromConfig, null);
  }

  public static JsonNode mergeJsons(final ObjectNode mainConfig, final ObjectNode fromConfig, final JsonNode maskedValue) {
    for (final String key : Jsons.keys(fromConfig)) {
      if (fromConfig.get(key).getNodeType() == OBJECT) {
        // nested objects are merged rather than overwrite the contents of the equivalent object in config
        if (mainConfig.get(key) == null) {
          mergeJsons(mainConfig.putObject(key), (ObjectNode) fromConfig.get(key), maskedValue);
        } else if (mainConfig.get(key).getNodeType() == OBJECT) {
          mergeJsons((ObjectNode) mainConfig.get(key), (ObjectNode) fromConfig.get(key), maskedValue);
        } else {
          throw new IllegalStateException("Can't merge an object node into a non-object node!");
        }
      } else {
        if (maskedValue != null && !maskedValue.isNull()) {
          LOGGER.debug(String.format("Masking instance wide parameter %s in config", key));
          mainConfig.set(key, maskedValue);
        } else {
          if (!mainConfig.has(key) || isSecretMask(mainConfig.get(key).asText())) {
            LOGGER.debug(String.format("injecting instance wide parameter %s into config", key));
            mainConfig.set(key, fromConfig.get(key));
          }
        }
      }
    }
    return mainConfig;
  }

  public static JsonNode getSecretMask() {
    // TODO secrets should be masked with the correct type
    // https://github.com/airbytehq/airbyte/issues/5990
    // In the short-term this is not world-ending as all secret fields are currently strings
    return Jsons.jsonNode(SECRET_MASK);
  }

  private static boolean isSecretMask(final String input) {
    return Strings.isNullOrEmpty(input.replaceAll("\\*", ""));
  }

}
