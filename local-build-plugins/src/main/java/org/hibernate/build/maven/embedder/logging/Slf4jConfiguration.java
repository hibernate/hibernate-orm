package org.hibernate.build.maven.embedder.logging;

import org.apache.maven.cli.logging.impl.Slf4jSimpleConfiguration;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.MavenSlf4jSimpleFriend;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Slf4jConfiguration extends Slf4jSimpleConfiguration {
	@Override
	public void activate() {
		System.out.println("activating Slf4jConfiguration");
		resetLoggerFactory();
		initMavenSlf4jSimpleFriend();
	}

	private void resetLoggerFactory() {
		System.out.println("Resetting Logger factory");
		try {
			Method m = LoggerFactory.class.getDeclaredMethod("reset", new Class[]{});
			m.setAccessible(true);
			m.invoke(null);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initMavenSlf4jSimpleFriend() {
		System.out.println("Initializing Maven Slf4j Simple Friend");
		MavenSlf4jSimpleFriend.init();
	}
}
