/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.test.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

public class ResourceUtil {

	public static void createResources(Object test, String[] resources, File resourcesDir) {
		try {
			for (String resource : resources) {
				InputStream inputStream = resolveResourceLocation(test.getClass(), resource);
				File resourceFile = new File(resourcesDir, resource);
				File parent = resourceFile.getParentFile();
				if (!parent.exists()) {
					if (!parent.mkdirs()) throw new AssertionError();
				}
				Files.copy(inputStream, resourceFile.toPath());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static InputStream resolveResourceLocation(Class<?> testClass, String resourceName) {
		InputStream result;
		if (resourceName.startsWith("/")) {
			result = testClass.getResourceAsStream(resourceName);
		} else {
			result = resolveRelativeResourceLocation(testClass, resourceName);
		}
		return result;
	}

	public static File resolveResourceFile(Class<?> testClass, String resourceName) {
		String path = testClass.getPackage().getName().replace('.', File.separatorChar);
		URL resourceUrl = testClass.getClassLoader().getResource(path + File.separatorChar
																+ resourceName);
		assert resourceUrl != null;
		return new File(resourceUrl.getFile());
	}

	private static String getRelativeResourcesRoot(Class<?> testClass) {
		return '/' + testClass.getPackage().getName().replace('.', '/') + '/';
	}

	private static InputStream resolveRelativeResourceLocation(Class<?> testClass, String resourceName) {
		InputStream result = testClass.getResourceAsStream(getRelativeResourcesRoot(testClass) + resourceName);
		if (result == null && testClass.getSuperclass() != Object.class) {
			result = resolveRelativeResourceLocation(testClass.getSuperclass(), resourceName);
		}
		return result;
	}

}
