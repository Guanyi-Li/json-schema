/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.jsonvalidator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.everit.jsonvalidator.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

@RunWith(Parameterized.class)
public class IntegrationTest {

  @Parameters(name = "{2}")
  public static List<Object[]> params() {
    List<Object[]> rval = new ArrayList<>();
    Reflections refs = new Reflections("org.everit.jsonvalidator",
        new ResourcesScanner());
    Set<String> paths = refs.getResources(Pattern.compile(".*\\.json"));
    for (String path : paths) {
      if (path.indexOf("/optional/") > -1) {
        continue;
      }
      String fileName = path.substring(path.lastIndexOf('/') + 1);
      JSONArray arr = loadTests(IntegrationTest.class.getResourceAsStream("/" + path));
      for (int i = 0; i < arr.length(); ++i) {
        JSONObject schemaTest = arr.getJSONObject(i);
        JSONArray testcaseInputs = schemaTest.getJSONArray("tests");
        for (int j = 0; j < testcaseInputs.length(); ++j) {
          JSONObject input = testcaseInputs.getJSONObject(j);
          Object[] params = new Object[5];
          params[0] = "[" + fileName + "]/" + schemaTest.getString("description");
          params[1] = schemaTest.get("schema");
          params[2] = "[" + fileName + "]/" + input.getString("description");
          params[3] = input.get("data");
          params[4] = input.getBoolean("valid");
          rval.add(params);
        }
      }
    }
    return rval;
  }

  private static JSONArray loadTests(final InputStream input) {
    return new JSONArray(new JSONTokener(input));
  }

  private final String schemaDescription;

  private final JSONObject schemaJson;

  private final String inputDescription;

  private final Object input;

  private final boolean expectedToBeValid;

  public IntegrationTest(final String schemaDescription, final JSONObject schemaJson,
      final String inputDescription,
      final Object input, final Boolean expectedToBeValid) {
    this.schemaDescription = schemaDescription;
    this.schemaJson = schemaJson;
    this.inputDescription = inputDescription;
    this.input = input;
    this.expectedToBeValid = expectedToBeValid;
  }

  @Test
  public void test() {
    try {
      Schema schema = SchemaLoader.load(schemaJson);
      schema.validate(input);
      if (!expectedToBeValid) {
        throw new AssertionError("false success for " + inputDescription);
      }
    } catch (ValidationException e) {
      if (expectedToBeValid) {
        throw new AssertionError("false failure for " + inputDescription, e);
      }
    } catch (SchemaException e) {
      throw new AssertionError("schema loading failure for " + schemaDescription, e);
    } catch (JSONException e) {
      throw new AssertionError("schema loading error for " + schemaDescription, e);
    }
  }

}
