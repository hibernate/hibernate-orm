package org.hibernate.tool.api.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

public class DefaultJavaPrettyPrinterStrategy {
	
	public DefaultJavaPrettyPrinterStrategy(Map<Object, Object> settings) {}

	public boolean formatFile(File file) {
		try {
			Formatter formatter = new Formatter();
			String toFormat = new String(Files.readAllBytes(file.toPath()));
			String toWrite = formatter.formatSource(toFormat);
			Files.write(file.toPath(), toWrite.getBytes());
			return true;
		} catch (IOException | FormatterException e) {
			throw new RuntimeException(e);
		}
	}
	
}	
