/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.test.utils;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
		ArrayList<String> arguments = new ArrayList<>();
		arguments.add("-d");
		arguments.add(destinationFolder.getAbsolutePath());
		arguments.add("-sourcepath");
		arguments.add(sourceFolder.getAbsolutePath());
		if (classPath != null && !classPath.isEmpty()) {
			arguments.add("-cp");
			arguments.add(convertClassPath(classPath));
		}
		ArrayList<String> fileNames = new ArrayList<>();
		collectJavaFiles(sourceFolder, fileNames);
		arguments.addAll(fileNames);
		javaCompiler.run(
				null,
				null,
				null,
				arguments.toArray(new String[0]));
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
			for (File child : Objects.requireNonNull(file.listFiles())) {
				collectJavaFiles(child, list);
			}
		} else {
			if (file.getName().endsWith(".java")) {
				list.add(file.getAbsolutePath());
			}
		}
	}

	private static String convertClassPath(List<String> paths) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < paths.size() - 1; i++) {
			sb.append(paths.get(i)).append(File.pathSeparator);
		}
		sb.append(paths.get(paths.size() - 1));
		return sb.toString();
	}

}
