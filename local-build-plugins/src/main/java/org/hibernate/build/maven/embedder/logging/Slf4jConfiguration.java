/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.maven.embedder.logging;

import org.apache.maven.cli.logging.impl.Slf4jSimpleConfiguration;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.MavenSlf4jSimpleFriend;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Slf4jConfiguration extends Slf4jSimpleConfiguration {
	@Override
	public void activate() {
		resetLoggerFactory();
		initMavenSlf4jSimpleFriend();
	}

	private void resetLoggerFactory() {
		try {
			Method m = LoggerFactory.class.getDeclaredMethod("reset", new Class[]{});
			m.setAccessible(true);
			m.invoke(null);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void initMavenSlf4jSimpleFriend() {
		MavenSlf4jSimpleFriend.init();
	}
}
