/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.id.enhanced;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.HibernateException;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;

/**
 * Describes a sequence.
 *
 * @author Steve Ebersole
 */
public class SequenceStructure implements DatabaseStructure {
	private static final Logger log = LoggerFactory.getLogger( SequenceStructure.class );

	private final String sequenceName;
	private final int initialValue;
	private final int incrementSize;
	private final Class numberType;
	private final String sql;
	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;

	public SequenceStructure(
			Dialect dialect,
			String sequenceName,
			int initialValue,
			int incrementSize,
			Class numberType) {
		this.sequenceName = sequenceName;
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.numberType = numberType;
		sql = dialect.getSequenceNextValString( sequenceName );
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return sequenceName;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getIncrementSize() {
		return incrementSize;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTimesAccessed() {
		return accessCounter;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getInitialValue() {
		return initialValue;
	}

	/**
	 * {@inheritDoc}
	 */
	public AccessCallback buildCallback(final SessionImplementor session) {
		return new AccessCallback() {
			public IntegralDataTypeHolder getNextValue() {
				accessCounter++;
				try {
					PreparedStatement st = session.getBatcher().prepareSelectStatement( sql );
					try {
						ResultSet rs = st.executeQuery();
						try {
							rs.next();
							IntegralDataTypeHolder value = IdentifierGeneratorHelper.getIntegralDataTypeHolder( numberType );
							value.initialize( rs, 1 );
							if ( log.isDebugEnabled() ) {
								log.debug( "Sequence value obtained: " + value.makeValue() );
							}
							return value;
						}
						finally {
							try {
								rs.close();
							}
							catch( Throwable ignore ) {
								// intentionally empty
							}
						}
					}
					finally {
						session.getBatcher().closeStatement( st );
					}

				}
				catch ( SQLException sqle) {
					throw JDBCExceptionHelper.convert(
							session.getFactory().getSQLExceptionConverter(),
							sqle,
							"could not get next sequence value",
							sql
					);
				}
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public void prepare(Optimizer optimizer) {
		applyIncrementSizeToSourceValues = optimizer.applyIncrementSizeToSourceValues();
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		int sourceIncrementSize = applyIncrementSizeToSourceValues ? incrementSize : 1;
		return dialect.getCreateSequenceStrings( sequenceName, initialValue, sourceIncrementSize );
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return dialect.getDropSequenceStrings( sequenceName );
	}
}
