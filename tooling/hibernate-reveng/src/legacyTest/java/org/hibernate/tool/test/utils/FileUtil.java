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
package org.hibernate.tool.test.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.*;

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
