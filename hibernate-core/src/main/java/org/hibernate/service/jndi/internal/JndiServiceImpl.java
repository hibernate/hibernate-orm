/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.jndi.internal;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.jndi.JndiException;
import org.hibernate.internal.util.jndi.JndiHelper;
import org.hibernate.service.jndi.spi.JndiService;

import org.jboss.logging.Logger;

/**
 * Standard implementation of JNDI services.
 *
 * @author Steve Ebersole
 */
public class JndiServiceImpl implements JndiService {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, JndiServiceImpl.class.getName());

	private final Hashtable initialContextSettings;

	public JndiServiceImpl(Map configurationValues) {
		this.initialContextSettings = JndiHelper.extractJndiProperties( configurationValues );
	}

	@Override
	public Object locate(String jndiName) {
		InitialContext initialContext = buildInitialContext();
		try {
			return JndiHelper.locate( jndiName, initialContext );
		}
		finally {
			try {
				initialContext.close();
			}
			catch ( NamingException e ) {
                LOG.unableToCloseInitialContext(e.toString());
			}
		}
	}

	private InitialContext buildInitialContext() {
		try {
			return initialContextSettings.size() == 0 ? new InitialContext() : new InitialContext( initialContextSettings );
		}
		catch ( NamingException e ) {
			throw new JndiException( "Unable to open InitialContext", e );
		}
	}

	@Override
	public void bind(String jndiName, Object value) {
		InitialContext initialContext = buildInitialContext();
		try {
			JndiHelper.bind( jndiName, value, initialContext );
		}
		finally {
			try {
				initialContext.close();
			}
			catch ( NamingException e ) {
                LOG.unableToCloseInitialContext(e.toString());
			}
		}
	}
}
