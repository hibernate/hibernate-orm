/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.engine.jdbc.internal;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.ContextualLobCreator;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Builds {@link LobCreator} instances based on the capabilities of the environment.
 *
 * @author Steve Ebersole
 */
public class LobCreatorBuilder {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, LobCreatorBuilder.class.getName());

    private boolean useContextualLobCreation;

	/**
	 * The public factory method for obtaining the appropriate (according to given JDBC {@link java.sql.Connection}.
	 *
	 *
	 * @param jdbcConnection A JDBC {@link java.sql.Connection} which can be used to gauge the drivers level of support,
	 * specifically for creating LOB references.
	 */
	public LobCreatorBuilder(Map configValues, Connection jdbcConnection) {
		this.useContextualLobCreation = useContextualLobCreation( configValues, jdbcConnection );
	}

	private static final Class[] NO_ARG_SIG = new Class[0];
	private static final Object[] NO_ARGS = new Object[0];

	/**
	 * Basically here we are simply checking whether we can call the {@link Connection} methods for
	 * LOB creation added in JDBC 4.  We not only check whether the {@link Connection} declares these methods,
	 * but also whether the actual {@link Connection} instance implements them (i.e. can be called without simply
	 * throwing an exception).
	 *
	 * @param jdbcConnection The connection which can be used in level-of-support testing.
	 *
	 * @return True if the connection can be used to create LOBs; false otherwise.
	 */
	private static boolean useContextualLobCreation(Map configValues, Connection jdbcConnection) {
		boolean isNonContextualLobCreationRequired =
				ConfigurationHelper.getBoolean( Environment.NON_CONTEXTUAL_LOB_CREATION, configValues );
		if ( isNonContextualLobCreationRequired ) {
			LOG.disablingContextualLOBCreation( Environment.NON_CONTEXTUAL_LOB_CREATION );
			return false;
		}
		if ( jdbcConnection == null ) {
			LOG.disablingContextualLOBCreationSinceConnectionNull();
			return false;
		}

		try {
			try {
				DatabaseMetaData meta = jdbcConnection.getMetaData();
				// if the jdbc driver version is less than 4, it shouldn't have createClob
				if ( meta.getJDBCMajorVersion() < 4 ) {
					LOG.disablingContextualLOBCreationSinceOldJdbcVersion( meta.getJDBCMajorVersion() );
					return false;
				}
			}
			catch ( SQLException ignore ) {
				// ignore exception and continue
			}

			Class connectionClass = Connection.class;
			Method createClobMethod = connectionClass.getMethod( "createClob", NO_ARG_SIG );
			if ( createClobMethod.getDeclaringClass().equals( Connection.class ) ) {
				// If we get here we are running in a jdk 1.6 (jdbc 4) environment...
				// Further check to make sure the driver actually implements the LOB creation methods.  We
				// check against createClob() as indicative of all; should we check against all 3 explicitly?
				try {
					Object clob = createClobMethod.invoke( jdbcConnection, NO_ARGS );
					try {
						Method freeMethod = clob.getClass().getMethod( "free", NO_ARG_SIG );
						freeMethod.invoke( clob, NO_ARGS );
					}
					catch ( Throwable ignore ) {
						LOG.tracef( "Unable to free CLOB created to test createClob() implementation : %s", ignore );
					}
					return true;
				}
				catch ( Throwable t ) {
					LOG.disablingContextualLOBCreationSinceCreateClobFailed( t );
				}
			}
		}
		catch ( NoSuchMethodException ignore ) {
		}

		return false;
	}

	public LobCreator buildLobCreator(LobCreationContext lobCreationContext) {
		return useContextualLobCreation
				? new ContextualLobCreator( lobCreationContext )
				: NonContextualLobCreator.INSTANCE;
	}
}
