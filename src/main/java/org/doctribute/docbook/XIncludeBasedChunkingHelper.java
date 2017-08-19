/*
 * Copyright 2017-present doctribute (http://doctribute.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 * Jan Tosovsky
 */
package org.doctribute.docbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.doctribute.xml.Cloaker;

public class XIncludeBasedChunkingHelper {

    private static final String PARAM_XML = "-xml";
    private static final String PARAM_TOC = "-toc";
    private static final String PARAM_KEY = "-key";
    private static final String PARAM_IGNORE_KEY = "-ignorekey";

    private static final String PARAM_KEY_DEFAULT_VALUE = "xinclude-based-chunking";
    private static final String PARAM_IGNORE_KEY_DEFAULT_VALUE = "do-not-chunk-this-xinclude";

    public static void main(String[] args) throws IOException {

        Map<String, String> passedValuesMap = new HashMap<>();

        for (String arg : args) {
            int index = arg.indexOf(":");
            if (index > 0 && index < arg.length() - 1) {
                passedValuesMap.put(arg.substring(0, index), arg.substring(index + 1));
            }
        }

        if (!passedValuesMap.isEmpty() && passedValuesMap.containsKey(PARAM_XML)) {

            Path sourcePath = Paths.get(passedValuesMap.get(PARAM_XML));

            if (!Files.exists(sourcePath)) {
                throw new IOException("The specified path was not found: " + sourcePath.toString());
            }

            sourcePath = sourcePath.toAbsolutePath();

            Map<String, String> defaultValuesMap = new HashMap<>();
            defaultValuesMap.put(PARAM_TOC, sourcePath.getParent().resolve("toc.xml").toString());
            defaultValuesMap.put(PARAM_KEY, PARAM_KEY_DEFAULT_VALUE);
            defaultValuesMap.put(PARAM_IGNORE_KEY, PARAM_IGNORE_KEY_DEFAULT_VALUE);

            Path manualTocPath = Paths.get(getValue(PARAM_TOC, passedValuesMap, defaultValuesMap));
            String key = getValue(PARAM_KEY, passedValuesMap, defaultValuesMap);
            String ignoreKey = getValue(PARAM_IGNORE_KEY, passedValuesMap, defaultValuesMap);

            String xml = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);

            if (xml.contains(key)) {

                XmlFileRootIdHarvester xmlFileRootIdHarvester = new XmlFileRootIdHarvester();
                Files.walkFileTree(sourcePath.getParent(), xmlFileRootIdHarvester);
                Map<String, String> idFileNameMap = xmlFileRootIdHarvester.getIdFileNameMap();

                try (
                        InputStream inputStream = Cloaker.getCloakedInputStream(sourcePath);
                        OutputStream outputStream = Files.newOutputStream(manualTocPath)) {

                    ChunkConfigBuilder.run(inputStream, outputStream, idFileNameMap, ignoreKey);
                }

                try (
                        InputStream cloakedInputStream = Cloaker.getCloakedInputStream(sourcePath);
                        ByteArrayOutputStream cloakedOutputStream = new ByteArrayOutputStream();
                        OutputStream outputStream = Files.newOutputStream(sourcePath)) {

                    MainFileLocalTocAppender.run(cloakedInputStream, cloakedOutputStream);

                    xml = new String(cloakedOutputStream.toByteArray(), StandardCharsets.UTF_8);

                    outputStream.write(Cloaker.getUncloakedContent(xml).getBytes(StandardCharsets.UTF_8));
                }
            }

        } else {
            System.out.println("Usage:");
            System.out.println("java -jar tocprocessor.jar ");
            System.out.println("     -xml:source.xml");
            System.out.println("    [-toc:toc.xml]");
            System.out.println("    [-key:" + PARAM_KEY_DEFAULT_VALUE + "]");
            System.out.println("    [-ignorekey:" + PARAM_IGNORE_KEY_DEFAULT_VALUE + "]");
        }
    }

    private static String getValue(String key, Map<String, String> passedValuesMap, Map<String, String> defaultValuesMap) {

        String passedValue = passedValuesMap.get(key);

        return (passedValue != null) ? passedValue : defaultValuesMap.get(key);
    }
}
