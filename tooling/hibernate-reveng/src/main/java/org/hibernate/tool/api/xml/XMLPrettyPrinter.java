/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hibernate.tool.internal.xml.XMLPrettyPrinterStrategyFactory;

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
	    } catch (Exception e) {
	        throw new RuntimeException(e); // simple exception handling, please review it
	    }
	}
	
}
