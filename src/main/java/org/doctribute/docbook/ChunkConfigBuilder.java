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
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class ChunkConfigBuilder {

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();
    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();
    private static final QName[] QNAMES_ID = {new QName("id"), new QName("http://www.w3.org/XML/1998/namespace", "id")};
    private static final QName[] QNAMES_HREF = {new QName("href"), new QName("http://docbook.org/ns/docbook", "href")};

    public static void run(InputStream inputStream, OutputStream outputStream, Map<String, String> idFileNameMap, String ignoreKey) throws IOException {

        try {

            XMLEventReader eventReader = INPUT_FACTORY.createXMLEventReader(inputStream);
            XMLStreamWriter streamWriter = OUTPUT_FACTORY.createXMLStreamWriter(outputStream);

            boolean isSuppressed = false;
            int level = 0;

            while (eventReader.hasNext()) {

                XMLEvent event = eventReader.nextEvent();

                switch (event.getEventType()) {

                    case XMLStreamConstants.START_ELEMENT:

                        StartElement startElement = event.asStartElement();
                        String startElementName = startElement.getName().getLocalPart();

                        switch (startElementName) {

                            case "book":
                            case "article": {

                                String id = getAttributeValue(startElement, QNAMES_ID);

                                if (id == null) {
                                    throw new IOException("Missing mandatory ID attribute of the root element.");
                                }

                                streamWriter.writeStartDocument();
                                streamWriter.writeStartElement("toc");
                                streamWriter.writeAttribute("role", "chunk-toc");

                                streamWriter.writeStartElement("tocentry");
                                streamWriter.writeAttribute("linkend", id);
                                streamWriter.writeProcessingInstruction("dbhtml", "filename=\"index.html\"");

                                break;
                            }

                            case "xxXINCLUDExx": {

                                String href = getAttributeValue(startElement, QNAMES_HREF);
                                String fileName = getFileName(href);

                                boolean isIgnored = false;

                                while (!(event.isEndElement())) {

                                    event = eventReader.nextEvent();
                                    if (event.isProcessingInstruction()) {
                                        isIgnored = event.toString().contains(ignoreKey);
                                    }
                                }

                                if (!isIgnored) {
                                    if (fileName != null && idFileNameMap.containsValue(fileName)) {

                                        for (Entry<String, String> entry : idFileNameMap.entrySet()) {
                                            if (entry.getValue().equals(fileName)) {
                                                streamWriter.writeStartElement("tocentry");
                                                streamWriter.writeAttribute("linkend", entry.getKey());
                                                streamWriter.writeProcessingInstruction("dbhtml", "filename=\"" + fileName.replace(".xml", ".html") + "\"");
                                                streamWriter.writeEndElement();
                                                break;
                                            }
                                        }

                                    } else {
                                        throw new IOException("Missing mandatory ID attribute of the root element in the file '" + href + "'.");
                                    }
                                }

                                break;
                            }

                            default: {

                                String id = getAttributeValue(startElement, QNAMES_ID);

                                if (id != null && (startElementName.equals("chapter")
                                        || startElementName.equals("appendix")
                                        || startElementName.equals("index")
                                        || startElementName.equals("glossary")
                                        || startElementName.equals("section"))) {

                                    streamWriter.writeStartElement("tocentry");
                                    streamWriter.writeAttribute("linkend", id);
                                    streamWriter.writeProcessingInstruction("dbhtml", "filename=\"" + id + ".html\"");

                                } else {
                                    isSuppressed = true;
                                    level++;
                                }

                                break;
                            }
                        }

                        break;

                    case XMLStreamConstants.END_ELEMENT:

                        EndElement endElement = event.asEndElement();
                        String endElementName = endElement.getName().getLocalPart();

                        if (endElementName.equals("book") || endElementName.equals("article")) {

                            streamWriter.writeEndElement();
                            streamWriter.writeEndDocument();

                        } else if (!isSuppressed) {
                            streamWriter.writeEndElement();

                        } else {
                            level--;
                            if (level == 0) {
                                isSuppressed = false;
                            }
                        }

                        break;
                }
            }

            eventReader.close();
            streamWriter.close();

        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private static String getAttributeValue(StartElement element, QName[] qNames) {

        for (QName qName : qNames) {

            Attribute attribute = element.getAttributeByName(qName);
            if (attribute != null) {
                return attribute.getValue();
            }
        }

        return null;
    }

    private static String getFileName(String href) {

        String fileName = null;

        if (href != null) {
            String[] pathFragments = href.split("/");
            fileName = pathFragments[pathFragments.length - 1];
        }

        return fileName;
    }
}
