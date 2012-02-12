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
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;

/**
 * <b>increment</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that returns a <tt>long</tt>, constructed by
 * counting from the maximum primary key value at startup. Not safe for use in a
 * cluster!<br>
 * <br>
 * Mapping parameters supported, but not usually needed: tables, column.
 * (The tables parameter specified a comma-separated list of table names.)
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class IncrementGenerator implements IdentifierGenerator, Configurable {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, IncrementGenerator.class.getName());

	private Class returnClass;
	private String sql;

	private IntegralDataTypeHolder previousValueHolder;

	public synchronized Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		if ( sql != null ) {
			initializePreviousValueHolder( session );
		}
		return previousValueHolder.makeValueThenIncrement();
	}

	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		returnClass = type.getReturnedClass();

		ObjectNameNormalizer normalizer =
				( ObjectNameNormalizer ) params.get( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER );

		String column = params.getProperty( "column" );
		if ( column == null ) {
			column = params.getProperty( PersistentIdentifierGenerator.PK );
		}
		column = dialect.quote( normalizer.normalizeIdentifierQuoting( column ) );

		String tableList = params.getProperty( "tables" );
		if ( tableList == null ) {
			tableList = params.getProperty( PersistentIdentifierGenerator.TABLES );
		}
		String[] tables = StringHelper.split( ", ", tableList );

		final String schema = dialect.quote(
				normalizer.normalizeIdentifierQuoting(
						params.getProperty( PersistentIdentifierGenerator.SCHEMA )
				)
		);
		final String catalog = dialect.quote(
				normalizer.normalizeIdentifierQuoting(
						params.getProperty( PersistentIdentifierGenerator.CATALOG )
				)
		);

		StringBuilder buf = new StringBuilder();
		for ( int i=0; i < tables.length; i++ ) {
			final String tableName = dialect.quote( normalizer.normalizeIdentifierQuoting( tables[i] ) );
			if ( tables.length > 1 ) {
				buf.append( "select " ).append( column ).append( " from " );
			}
			buf.append( Table.qualify( catalog, schema, tableName ) );
			if ( i < tables.length-1 ) {
				buf.append( " union " );
			}
		}
		if ( tables.length > 1 ) {
			buf.insert( 0, "( " ).append( " ) ids_" );
			column = "ids_." + column;
		}

		sql = "select max(" + column + ") from " + buf.toString();
	}

	private void initializePreviousValueHolder(SessionImplementor session) {
		previousValueHolder = IdentifierGeneratorHelper.getIntegralDataTypeHolder( returnClass );

		LOG.debugf( "Fetching initial value: %s", sql );
		try {
			PreparedStatement st = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
			try {
				ResultSet rs = st.executeQuery();
				try {
                    if (rs.next()) previousValueHolder.initialize(rs, 0L).increment();
                    else previousValueHolder.initialize(1L);
					sql = null;
					if ( LOG.isDebugEnabled() ) {
						LOG.debugf( "First free id: %s", previousValueHolder.makeValue() );
					}
				}
				finally {
					rs.close();
				}
			}
			finally {
				st.close();
			}
		}
		catch (SQLException sqle) {
			throw session.getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not fetch initial value for increment generator",
					sql
			);
		}
	}
}
