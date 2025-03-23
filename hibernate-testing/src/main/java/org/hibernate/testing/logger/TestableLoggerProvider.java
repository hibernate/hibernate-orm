/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.logging.Logger;

import org.apache.logging.log4j.ThreadContext;

/**
 * A {@code LoggerProvider} for JBoss Logger.
 * See also META-INF/services/org.jboss.logging.LoggerProvider
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
public class TestableLoggerProvider implements org.jboss.logging.LoggerProvider {

	//We LEAK Logger instances: good only for testing as we know the set of categories is limited in practice
	private static final ConcurrentMap<String,Logger> reuseLoggerInstances = new ConcurrentHashMap<>();

	// Maintainer note:
	// Except the next method, which is adjusted to return our own Log4DelegatingLogger
	// this class is a verbatim copy of org.jboss.logging.Log4jLoggerProvider
	// (which is a final class)

	@Override
	public Logger getLogger(final String name) {
		Logger logger = reuseLoggerInstances.get( name );
		if ( logger == null ) {
			logger = new Log4J2DelegatingLogger( "".equals( name ) ? "ROOT" : name );
			Logger previous = reuseLoggerInstances.putIfAbsent( name, logger );
			if ( previous != null ) {
				return previous;
			}
		}
		return logger;
	}

	@Override
	public void clearMdc() {
		ThreadContext.clearMap();
	}

	@Override
	public Object putMdc(String key, Object value) {
		try {
			return ThreadContext.get( key );
		}
		finally {
			ThreadContext.put( key, String.valueOf( value ) );
		}
	}

	@Override
	public Object getMdc(String key) {
		return ThreadContext.get( key );
	}

	@Override
	public void removeMdc(String key) {
		ThreadContext.remove( key );
	}

	@Override
	public Map<String, Object> getMdcMap() {
		return new HashMap<>( ThreadContext.getImmutableContext() );
	}

	@Override
	public void clearNdc() {
		ThreadContext.clearStack();
	}

	@Override
	public String getNdc() {
		return ThreadContext.peek();
	}

	@Override
	public int getNdcDepth() {
		return ThreadContext.getDepth();
	}

	@Override
	public String popNdc() {
		return ThreadContext.pop();
	}

	@Override
	public String peekNdc() {
		return ThreadContext.peek();
	}

	@Override
	public void pushNdc(String message) {
		ThreadContext.push( message );
	}

	@Override
	public void setNdcMaxDepth(int maxDepth) {
		ThreadContext.trim( maxDepth );
	}

}
