/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import org.jboss.logging.Log4j2LoggerProvider;
import org.jboss.logging.Logger;
import org.jboss.logging.LoggerProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@code LoggerProvider} for JBoss Logger.
 * See also META-INF/services/org.jboss.logging.LoggerProvider
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
public class TestableLoggerProvider implements LoggerProvider {

	//We LEAK Logger instances: good only for testing as we know the set of categories is limited in practice
	private static final ConcurrentMap<String, Logger> reuseLoggerInstances = new ConcurrentHashMap<>();
	private final LoggerProvider delegate;

	public TestableLoggerProvider() {
		delegate = new Log4j2LoggerProvider();
	}

	// Maintainer note:
	// Except the next method, which is adjusted to return our own Log4DelegatingLogger
	// this class is a verbatim copy of org.jboss.logging.Log4jLoggerProvider
	// (which is a final class)

	@Override
	public Logger getLogger(final String name) {
		Logger logger = reuseLoggerInstances.get( name );
		if ( logger == null ) {
			logger = new DelegatingLogger( delegate.getLogger( "".equals( name ) ? "ROOT" : name ) );
			Logger previous = reuseLoggerInstances.putIfAbsent( name, logger );
			if ( previous != null ) {
				return previous;
			}
		}
		return logger;
	}

	@Override
	public void clearMdc() {
		delegate.clearMdc();
	}

	@Override
	public Object putMdc(String key, Object value) {
		return delegate.putMdc( key, value );
	}

	@Override
	public Object getMdc(String key) {
		return delegate.getMdc( key );
	}

	@Override
	public void removeMdc(String key) {
		delegate.removeMdc( key );
	}

	@Override
	public Map<String, Object> getMdcMap() {
		return delegate.getMdcMap();
	}

	@Override
	public void clearNdc() {
		delegate.clearNdc();
	}

	@Override
	public String getNdc() {
		return delegate.getNdc();
	}

	@Override
	public int getNdcDepth() {
		return delegate.getNdcDepth();
	}

	@Override
	public String popNdc() {
		return delegate.popNdc();
	}

	@Override
	public String peekNdc() {
		return delegate.peekNdc();
	}

	@Override
	public void pushNdc(String message) {
		delegate.pushNdc( message );
	}

	@Override
	public void setNdcMaxDepth(int maxDepth) {
		delegate.setNdcMaxDepth( maxDepth );
	}
}
