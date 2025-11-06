/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.java;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DefaultJavaPrettyPrinterStrategy {

	public boolean formatFile(File file) {
		try {
			Formatter formatter = new Formatter();
			String toFormat = new String(Files.readAllBytes(file.toPath()));
			String toWrite = formatter.formatSource(toFormat);
			Files.write(file.toPath(), toWrite.getBytes());
			return true;
		}
		catch (IOException | FormatterException e) {
			throw new RuntimeException(e);
		}
	}

}
