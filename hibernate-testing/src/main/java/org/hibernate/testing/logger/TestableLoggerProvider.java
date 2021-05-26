/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.jboss.logging.Logger;

/**
 * A {@code LoggerProvider} for JBoss Logger.
 * See also META-INF/services/org.jboss.logging.LoggerProvider
 *
 * @author <a href="mailto:sanne@hibernate.org">Sanne Grinovero</a> (C) 2015 Red Hat Inc.
 */
public class TestableLoggerProvider implements org.jboss.logging.LoggerProvider {

	//We LEAK Logger instances: good only for testing as we know the set of categories is limited in practice
	private static final ConcurrentMap<String,Logger> reuseLoggerInstances = new ConcurrentHashMap<String,Logger>();

	// Maintainer note:
	// Except the next method, which is adjusted to return our own Log4DelegatingLogger
	// this class is a verbatim copy of org.jboss.logging.Log4jLoggerProvider
	// (which is a final class)

	public Logger getLogger(final String name) {
		Logger logger = reuseLoggerInstances.get( name );
		if ( logger == null ) {
			logger = new Log4DelegatingLogger( "".equals( name ) ? "ROOT" : name );
			Logger previous = reuseLoggerInstances.putIfAbsent( name, logger );
			if ( previous != null ) {
				return previous;
			}
		}
		return logger;
	}

	@Override
	public void clearMdc() {
		MDC.clear();
	}

	public Object getMdc(String key) {
		return MDC.get( key );
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getMdcMap() {
		return MDC.getContext();
	}

	public Object putMdc(String key, Object val) {
		try {
			return MDC.get( key );
		}
		finally {
			MDC.put( key, val );
		}
	}

	public void removeMdc(String key) {
		MDC.remove( key );
	}

	public void clearNdc() {
		NDC.remove();
	}

	public String getNdc() {
		return NDC.get();
	}

	public int getNdcDepth() {
		return NDC.getDepth();
	}

	public String peekNdc() {
		return NDC.peek();
	}

	public String popNdc() {
		return NDC.pop();
	}

	public void pushNdc(String message) {
		NDC.push( message );
	}

	public void setNdcMaxDepth(int maxDepth) {
		NDC.setMaxDepth( maxDepth );
	}

}
