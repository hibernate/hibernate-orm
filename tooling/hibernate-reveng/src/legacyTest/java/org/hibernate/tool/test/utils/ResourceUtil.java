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
		InputStream result = null;
		if (resourceName.startsWith("/")) {
			result = testClass.getResourceAsStream(resourceName);
		} else {
			result = resolveRelativeResourceLocation(testClass, resourceName);
		}
		return result;
	}

    public static File resolveResourceFile(Class<?> testClass, String resourceName) {
        String path = testClass.getPackage().getName().replace('.', '/');
        URL resourceUrl = testClass.getClassLoader().getResource(path + "/" + resourceName);
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
