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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class JavaUtil {
	
	public static void compile(File folder) {
		compile(folder, (List<String>)null);
	}
	
	public static void compile(File folder, List<String> classPath) {
		compile(folder, folder, classPath);
	}
	
	public static void compile(File sourceFolder, File destinationFolder) {
		compile(sourceFolder, destinationFolder, null);
	}
	
	public static void compile(
			File sourceFolder, 
			File destinationFolder, 
			List<String> classPath) {
		JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
		ArrayList<String> arguments = new ArrayList<String>();
		arguments.add("-d");
		arguments.add(destinationFolder.getAbsolutePath());
		arguments.add("-sourcepath");
		arguments.add(sourceFolder.getAbsolutePath());
		if (classPath != null && !classPath.isEmpty()) {
			arguments.add("-cp");
			arguments.add(convertClassPath(classPath));
		}
		ArrayList<String> fileNames = new ArrayList<String>();
		collectJavaFiles(sourceFolder, fileNames);
		arguments.addAll(fileNames);
		javaCompiler.run(
				null, 
				null, 
				null, 
				arguments.toArray(new String[arguments.size()]));
	}
	
	public static String resolvePathToJarFileFor(Class<?> clazz) {
		String result = null;
		CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
		if (codeSource != null) {
			URL url = codeSource.getLocation();
			if (url != null) {
				try {
					result = url.toURI().getPath();
				}
				catch (URISyntaxException e) {
					throw new IllegalArgumentException( "Unexpected path to a Jar file: " + url, e );
				}
			}
		}
		return result;
	}
	
	private static void collectJavaFiles(File file, ArrayList<String> list) {
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				collectJavaFiles(child, list);
			}
		} else { 
			if (file.getName().endsWith(".java")) {
				list.add(file.getAbsolutePath());
			}
		}
	}
	
	private static String convertClassPath(List<String> paths) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < paths.size() - 1; i++) {
			sb.append(paths.get(i)).append(File.pathSeparator);
		}
		sb.append(paths.get(paths.size() - 1));
		return sb.toString();
	}

}
