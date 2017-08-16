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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.doctribute.xml.Cloaker;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlFileRootIdHarvester extends SimpleFileVisitor<Path> {

    private static final DocumentBuilderFactory DOM_FACTORY = DocumentBuilderFactory.newInstance();
    private final DocumentBuilder builder;

    private final Map<String, String> idFileNameMap = new HashMap<>();

    public XmlFileRootIdHarvester() throws IOException {

        try {
            DOM_FACTORY.setNamespaceAware(true);
            builder = DOM_FACTORY.newDocumentBuilder();

        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

        if (path.toString().endsWith(".xml")) {

            try (InputStream inputStream = Cloaker.getCloakedInputStream(path)) {

                try {
                    String id = getId(builder.parse(inputStream));

                    if (!id.isEmpty()) {
                        if (!idFileNameMap.containsKey(id)) {
                            idFileNameMap.put(id, path.getFileName().toString());
                        }
                    }

                } catch (SAXException e) {
                    throw new IOException("File " + path + " couldn't be parsed.", e);
                }
            }
        }

        return FileVisitResult.CONTINUE;
    }

    private String getId(Document document) {

        String id = document.getDocumentElement().getAttribute("id");

        if (id.isEmpty()) {
            id = document.getDocumentElement().getAttribute("xml:id");
        }

        return id;
    }

    public Map<String, String> getIdFileNameMap() {
        return idFileNameMap;
    }
}
