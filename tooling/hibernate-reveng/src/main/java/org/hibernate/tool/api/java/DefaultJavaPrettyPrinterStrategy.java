/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2021-2025 Red Hat, Inc.
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
package org.hibernate.tool.api.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

public class DefaultJavaPrettyPrinterStrategy {

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