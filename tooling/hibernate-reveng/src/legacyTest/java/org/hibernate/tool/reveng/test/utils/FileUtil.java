/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtil {

	static public String findFirstString(String string, File file) {
		String str;
		try {
			BufferedReader in = new BufferedReader(new FileReader(file) );
			while ( (str = in.readLine() ) != null ) {
				if(str.contains(string)) {
					break;
				}
			}
			in.close();
		}
		catch (IOException e) {
			throw new RuntimeException("trouble with searching in " + file,e);
		}
		return str;
	}

	static public void generateNoopComparator(File sourceFolder) throws IOException {
		File file = new File(sourceFolder.getAbsolutePath() + "/comparator/NoopComparator.java");
		assertTrue(file.getParentFile().mkdirs());
		FileWriter fileWriter = new FileWriter(file);
		PrintWriter pw = new PrintWriter(fileWriter);
		pw.println("package comparator;                                ");
		pw.println("import java.util.Comparator;                       ");
		pw.println("public class NoopComparator implements Comparator {");
		pw.println("	public int compare(Object o1, Object o2) {     ");
		pw.println("		return 0;                                  ");
		pw.println("	}                                              ");
		pw.println("}                                                  ");
		pw.flush();
		pw.close();
	}

}
