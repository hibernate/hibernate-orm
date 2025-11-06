/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.xml;

import org.hibernate.tool.reveng.internal.xml.XMLPrettyPrinterStrategyFactory;

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
		prettyPrintFile(file, null);
	}

	public static void prettyPrintFile(File file, XMLPrettyPrinterStrategy strategy) throws IOException {
		String input = readFile(file.getAbsolutePath(), Charset.defaultCharset());
		String output = prettyFormat(input, strategy);
		PrintWriter writer = new PrintWriter(file);
		writer.print(output);
		writer.flush();
		writer.close();
	}

	private static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	private static String prettyFormat(String input, XMLPrettyPrinterStrategy strategy) {
		try {
			if (strategy == null) {
				strategy = XMLPrettyPrinterStrategyFactory.newXMLPrettyPrinterStrategy();
			}
			return strategy.prettyPrint(input);
		}
		catch (Exception e) {
			throw new RuntimeException(e); // simple exception handling, please review it
		}
	}

}
