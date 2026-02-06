/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.api.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class XMLPrettyPrinterTest {
	
	private static final String XML_BEFORE = "<foo><bar>foobar</bar></foo>";
	
	private static final String XML_AFTER = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" 	+ System.lineSeparator() +
			"<foo>" 														+ System.lineSeparator() +
	        "    <bar>foobar</bar>" 										+ System.lineSeparator() +
			"</foo>"														+ System.lineSeparator();
	
	private static final String XML_COMMENT = "<!-- Just a comment! -->";
	
	private static final String fileName = "foobarfile.xml";

	@TempDir 
	private File tempDir;
	
	private File xmlFile = null;
	
	@BeforeEach
	public void beforeEach() throws Exception {
		xmlFile = new File(tempDir, fileName);
		PrintWriter writer = new PrintWriter(xmlFile);
		writer.print(XML_BEFORE);
		writer.flush();
		writer.close();
	}
	
	@Test
	public void testXmlPrettyPrintDefault() throws Exception {
		XMLPrettyPrinter.prettyPrintFile(xmlFile);
		String result = Files.readString(xmlFile.toPath());
		assertEquals(XML_AFTER, result);
	}
	
	@Test
	public void testXmlPrettyPrintWithStrategy() throws Exception {
		XMLPrettyPrinter.prettyPrintFile(xmlFile, new FooBarStrategy());
		String result = Files.readString(xmlFile.toPath());
		assertEquals(XML_AFTER + XML_COMMENT, result);
	}
	
	public static class FooBarStrategy implements XMLPrettyPrinterStrategy {
		@Override
		public String prettyPrint(String xml) throws Exception {
			return XML_AFTER + XML_COMMENT;
		}		
	}
	
}
