/*
 * Created on 17-Dec-2004
 *
 */
package org.hibernate.tool.internal.xml;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author max
 * 
 */
public final class XMLPrettyPrinter {

	public static void prettyPrintFile(File file) throws IOException {
		String input = readFile(file.getAbsolutePath(), Charset.defaultCharset());
		String output = prettyFormat(input);
		PrintWriter writer = new PrintWriter(file);
		writer.print(output);
		writer.flush();
		writer.close();
	}

	private static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	private static String prettyFormat(String input) {
	    try {
			return XMLPrettyPrinterStrategyFactory.newXMLPrettyPrinterStrategy().prettyPrint(input);
	    } catch (Exception e) {
	        throw new RuntimeException(e); // simple exception handling, please review it
	    }
	}
	
}
