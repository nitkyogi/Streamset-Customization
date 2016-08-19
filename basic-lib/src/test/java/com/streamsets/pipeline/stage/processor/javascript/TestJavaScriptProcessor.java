/**
 * Copyright 2015 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.processor.javascript;

import com.google.common.collect.ImmutableList;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Processor;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.sdk.ProcessorRunner;
import com.streamsets.pipeline.sdk.RecordCreator;
import com.streamsets.pipeline.sdk.StageRunner;
import com.streamsets.pipeline.stage.processor.scripting.ProcessingMode;
import com.streamsets.pipeline.stage.processor.scripting.ScriptingProcessorTestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestJavaScriptProcessor {


  @Test
  public void testOutErr() throws Exception {
    Processor processor = new JavaScriptProcessor(
        ProcessingMode.RECORD,
        "for (var i = 0; i < records.length; i++){\n" +
        "  var record = records[i];" +
        "  output.write(record);\n" +
        "  record.value = 'Bye';\n" +
        "  output.write(record);\n" +
        "  record.value = 'Error';\n" +
        "  error.write(record, 'error');\n" +
        "}"
    );
    ScriptingProcessorTestUtil.verifyWriteErrorRecord(JavaScriptDProcessor.class, processor);
  }

  @Test
  public void testJavascriptAllTypes() throws Exception {
    Processor processor = new JavaScriptProcessor(ProcessingMode.RECORD,
        "var record = records[0];\n" +
        "record.value['newField'] = {\n" +
            "a: {\n" +
              "b: record.value['beginner'], \n" +
              "c: record.value['skilled']\n" +
            "}, \n" +
            "d: ['str1', 'str2'], \n" +
            "e: record.value['expert'] \n" +
          "};\n" +
        "output.write(record);");
    ProcessorRunner runner = new ProcessorRunner.Builder(JavaScriptDProcessor.class, processor)
      .addOutputLane("lane")
      .build();
    runner.runInit();
    try {
      Map<String, Field> map = new LinkedHashMap<>();
      map.put("beginner", Field.create("false"));
      map.put("intermediate", Field.create("yes"));
      map.put("advanced", Field.create("no"));
      map.put("expert", Field.create(true));
      map.put("skilled", Field.create(122345566));
      map.put("null", Field.create(Field.Type.STRING, null));

      List<Field> list = ImmutableList.of(Field.create("listString1"), Field.create("listString2"));
      map.put("list", Field.create(list));

      Record record = RecordCreator.create("s", "s:1");
      record.set(Field.create(map));

      StageRunner.Output output = runner.runProcess(ImmutableList.of(record));

      Assert.assertEquals(1, output.getRecords().get("lane").size());
      Field field = output.getRecords().get("lane").get(0).get();
      Assert.assertTrue(field.getValue() instanceof Map);
      Map<String, Field> result = field.getValueAsMap();
      Assert.assertTrue(result.size() == 8);
      Assert.assertTrue(result.containsKey("beginner"));
      Assert.assertEquals("false", result.get("beginner").getValue());
      Assert.assertTrue(result.containsKey("intermediate"));
      Assert.assertEquals("yes", result.get("intermediate").getValue());
      Assert.assertTrue(result.containsKey("advanced"));
      Assert.assertEquals("no", result.get("advanced").getValue());
      Assert.assertTrue(result.containsKey("expert"));
      Assert.assertEquals(true, result.get("expert").getValue());
      Assert.assertTrue(result.containsKey("skilled"));
      Assert.assertEquals(122345566, result.get("skilled").getValue());
      Assert.assertTrue(result.containsKey("null"));
      Assert.assertEquals(null, result.get("null").getValue());
      Assert.assertTrue(result.containsKey("list"));
      List<Field> listField = result.get("list").getValueAsList();
      Assert.assertTrue(listField.size() == 2);
      Assert.assertEquals("listString1", listField.get(0).getValueAsString());
      Assert.assertEquals("listString2", listField.get(1).getValueAsString());

      //Field added by Javascript Evaluator
      Assert.assertTrue(result.containsKey("newField"));
      Map<String, Field> newField = result.get("newField").getValueAsMap();
      Assert.assertTrue(newField.containsKey("a"));
      Assert.assertTrue(newField.get("a").getValueAsMap().containsKey("b"));
      Assert.assertEquals("false", newField.get("a").getValueAsMap().get("b").getValueAsString());
      Assert.assertTrue(newField.get("a").getValueAsMap().containsKey("c"));
      Assert.assertEquals(122345566, newField.get("a").getValueAsMap().get("c").getValue());
      Assert.assertTrue(newField.containsKey("d"));
      Assert.assertEquals("str1", newField.get("d").getValueAsList().get(0).getValueAsString());
      Assert.assertEquals("str2", newField.get("d").getValueAsList().get(1).getValueAsString());
      Assert.assertTrue(newField.containsKey("e"));
      Assert.assertEquals(true, newField.get("e").getValueAsBoolean());
    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testJavascriptMapArray() throws Exception {
    Processor processor = new JavaScriptProcessor(
        ProcessingMode.RECORD,
        "output.write(records[0]);\n" +
        "records[0].value = {};\n" +
        "records[0].value = 'Hello';\n" +
        "output.write(records[0]);\n" +
        "records[0].value = { 'foo' : 'FOO' };\n" +
        "output.write(records[0]);\n" +
        "records[0].value = [ 5 ];\n" +
        "output.write(records[0]);\n" +
        ""
    );
    ScriptingProcessorTestUtil.verifyMapAndArray(JavaScriptDProcessor.class, processor);
  }


  private void testMode(ProcessingMode mode) throws Exception {
    Processor processor = new JavaScriptProcessor(
        mode,
        "for (var i = 0; i < records.length; i++){\n" +
        "  output.write(records[i]);\n" +
        "}"
    );
    ScriptingProcessorTestUtil.verifyMode(JavaScriptDProcessor.class, processor);
  }

  @Test
  public void testRecordMode() throws Exception {
    testMode(ProcessingMode.RECORD);
  }

  @Test
  public void testBatchMode() throws Exception {
    testMode(ProcessingMode.BATCH);
  }

  private void testRecordModeOnErrorHandling(OnRecordError onRecordError) throws Exception {
    Processor processor = new JavaScriptProcessor(
        ProcessingMode.RECORD,
        "for (var i = 0; i < records.length; i++){\n" +
        "  var record = records[i];" +
        "  if (record.value == 'Hello') {\n" +
        "    throw 'Exception';\n" +
        "  }" +
        "  output.write(record);" +
        "}"
    );
    ScriptingProcessorTestUtil.verifyRecordModeOnErrorHandling(JavaScriptDProcessor.class, processor, onRecordError);
  }


  @Test
  public void testRecordOnErrorDiscard() throws Exception {
    testRecordModeOnErrorHandling(OnRecordError.DISCARD);
  }

  @Test
  public void testRecordOnErrorToError() throws Exception {
    testRecordModeOnErrorHandling(OnRecordError.TO_ERROR);
  }

  @Test(expected = StageException.class)
  public void testRecordOnErrorStopPipeline() throws Exception {
    testRecordModeOnErrorHandling(OnRecordError.STOP_PIPELINE);
  }

  private void testBatchModeOnErrorHandling(OnRecordError onRecordError) throws Exception {
    Processor processor = new JavaScriptProcessor(
        ProcessingMode.BATCH,
        "for (var i = 0; i < records.length; i++){\n" +
        "  var record = records[i];" +
        "  if (record.value == 'Hello') {\n" +
        "    throw 'Exception';\n" +
        "  }" +
        "  output.write(record);" +
        "}"
    );
    ScriptingProcessorTestUtil.verifyBatchModeOnErrorHandling(JavaScriptDProcessor.class, processor, onRecordError);
  }


  @Test(expected = StageException.class)
  public void testBatchOnErrorDiscard() throws Exception {
    testBatchModeOnErrorHandling(OnRecordError.DISCARD);
  }

  @Test(expected = StageException.class)
  public void testBatchOnErrorToError() throws Exception {
    testBatchModeOnErrorHandling(OnRecordError.TO_ERROR);
  }

  @Test(expected = StageException.class)
  public void testBatchOnErrorStopPipeline() throws Exception {
    testBatchModeOnErrorHandling(OnRecordError.STOP_PIPELINE);
  }

  @Test
  public void testPrimitiveTypesPassthrough() throws Exception {
    Processor processor = new JavaScriptProcessor(
        ProcessingMode.BATCH,
        "for (var i = 0; i < records.length; i++){\n" +
        "  output.write(records[i]);\n" +
        "}"
    );
    ScriptingProcessorTestUtil.verifyPrimitiveTypesPassthrough(JavaScriptDProcessor.class, processor);
  }

  @Test
  public void testPrimitiveTypesFromScripting() throws Exception {
    Processor processor = new JavaScriptProcessor(
        ProcessingMode.BATCH,
        "for (var i = 0; i < records.length; i++){\n" +
        "  records[i].value = [ 1, 0.5, true, 'hello' ];\n" +
        "  output.write(records[i]);\n" +
        "  records[i].value = null;\n" +
        "  output.write(records[i]);\n" +
        "}"
    );
    ScriptingProcessorTestUtil.verifyPrimitiveTypesFromScriptingJavaScript(JavaScriptDProcessor.class, processor);
  }

  @Test
  public void testStateObject() throws Exception {
    Processor processor = new JavaScriptProcessor(ProcessingMode.RECORD,
        "if (!state['total_count']) {\n" +
        "  state['total_count'] = 0;\n" +
        "}\n" +
        "state['total_count'] = state['total_count'] + records.length;\n" +
        "for (var i = 0; i < records.length; i++) {\n" +
        "  records[i].value['count'] = state['total_count'];\n" +
        "  output.write(records[i]);\n" +
        "}");
    ScriptingProcessorTestUtil.verifyStateObjectJavaScript(JavaScriptDProcessor.class, processor);
  }

  @Test
  public void testListMap() throws Exception {
    Processor processor = new JavaScriptProcessor(
        ProcessingMode.RECORD,
        "output.write(records[0]);\n" +
        "records[0].value['Hello'] = 2\n" +
        "output.write(records[0])\n" +
        ""
    );
    ScriptingProcessorTestUtil.verifyListMap(JavaScriptDProcessor.class, processor);
  }
  @Test
  public void testListMapOrder() throws Exception {
    Processor processor = new JavaScriptProcessor(
        ProcessingMode.RECORD,
        "records[0].value['A0'] = 0\n" +
        "records[0].value['A1'] = 1\n" +
        "records[0].value['A2'] = 2\n" +
        "records[0].value['A3'] = 3\n" +
        "records[0].value['A4'] = 4\n" +
        "records[0].value['A5'] = 5\n" +
        "records[0].value['A6'] = 6\n" +
        "records[0].value['A7'] = 7\n" +
        "records[0].value['A8'] = 8\n" +
        "records[0].value['A9'] = 9\n" +
        "records[0].value['A10'] = 10\n" +
        "records[0].value['A11'] = 11\n" +
        "records[0].value['A12'] = 12\n" +
        "records[0].value['A13'] = 13\n" +
        "records[0].value['A14'] = 14\n" +
        "records[0].value['A15'] = 15\n" +
        "records[0].value['A16'] = 16\n" +
        "records[0].value['A17'] = 17\n" +
        "records[0].value['A18'] = 18\n" +
        "records[0].value['A19'] = 19\n" +
        "output.write(records[0])\n" +
            ""
    );
    ScriptingProcessorTestUtil.verifyListMapOrder(JavaScriptDProcessor.class, processor);
  }
}
