package org.hibernate.tool.api.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class XMLPrettyPrinterTest {
	
	private static final String XML_BEFORE = "<foo><bar>foobar</bar></foo>";
	
	private static final String XML_AFTER = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
			"<foo>\n" +
	        "    <bar>foobar</bar>\n" +
			"</foo>\n";
	
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
