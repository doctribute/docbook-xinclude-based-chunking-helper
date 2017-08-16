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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;

public class MainFileLocalTocAppender {

    private static final DocumentBuilderFactory DOM_FACTORY = DocumentBuilderFactory.newInstance();
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
    private static final XPath XPATH = XPATH_FACTORY.newXPath();
    private static final String XPATH_EXPRESSION = "//*[local-name()='chapter' or local-name()='appendix' or local-name()='section'][child::*[local-name()='xxXINCLUDExx']]";
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    public static void run(InputStream inputStream, OutputStream outputStream) throws IOException {

        try {
            DOM_FACTORY.setNamespaceAware(true);
            DocumentBuilder builder = DOM_FACTORY.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            XPathExpression xPathExpression = XPATH.compile(XPATH_EXPRESSION);

            NodeList nodeList = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                ProcessingInstruction pi = document.createProcessingInstruction("toc", "");
                node.insertBefore(pi, node.getFirstChild());
            }

            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(outputStream);

            transformer.transform(source, result);


        } catch (ParserConfigurationException| TransformerException | SAXException | XPathExpressionException e) {
            throw new IOException(e);
        }
    }

}
