/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
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
package org.hibernate.tools.test.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JavaUtilTest {
	
	private static final String FOO_STRING = 
			"package org.hibernate.tool.test;"+
			"public class Foo {              "+
			"  public Bar bar;               "+
			"}                               ";
	
	private static final String BAR_STRING = 
			"package org.hibernate.tool.test;"+
			"public class Bar {              "+
			"  public Foo foo;               "+
			"}                               ";
	
	@TempDir
	public File outputFolder = new File("output");
	
	@Test
	public void testCompile() throws Exception {
		File packageFolder = new File(outputFolder, "org/hibernate/tool/test");
		packageFolder.mkdirs();
		File fooFile = new File(packageFolder, "Foo.java");
		FileWriter fileWriter = new FileWriter(fooFile);
		fileWriter.write(FOO_STRING);
		fileWriter.close();
		File barFile = new File(packageFolder, "Bar.java");
		fileWriter = new FileWriter(barFile);
		fileWriter.write(BAR_STRING);
		fileWriter.close();
		assertFalse(new File(packageFolder, "Foo.class").exists());
		assertFalse(new File(packageFolder, "Bar.class").exists());
		JavaUtil.compile(outputFolder);
		assertTrue(new File(packageFolder, "Foo.class").exists());
		assertTrue(new File(packageFolder, "Bar.class").exists());
	}

}
