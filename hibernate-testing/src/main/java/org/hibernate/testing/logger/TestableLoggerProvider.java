/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2015 Red Hat Inc.
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
