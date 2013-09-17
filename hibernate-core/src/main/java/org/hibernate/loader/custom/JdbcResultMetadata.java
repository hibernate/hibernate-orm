/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.custom;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Simplified access to JDBC ResultSetMetaData
 *
 * @author Steve Ebersole
 */
class JdbcResultMetadata {
	private final SessionFactoryImplementor factory;
	private final ResultSet resultSet;
	private final ResultSetMetaData resultSetMetaData;

	public JdbcResultMetadata(SessionFactoryImplementor factory, ResultSet resultSet) throws HibernateException {
		try {
			this.factory = factory;
			this.resultSet = resultSet;
			this.resultSetMetaData = resultSet.getMetaData();
		}
		catch( SQLException e ) {
			throw new HibernateException( "Could not extract result set metadata", e );
		}
	}

	public int getColumnCount() throws HibernateException {
		try {
			return resultSetMetaData.getColumnCount();
		}
		catch( SQLException e ) {
			throw new HibernateException( "Could not determine result set column count", e );
		}
	}

	public int resolveColumnPosition(String columnName) throws HibernateException {
		try {
			return resultSet.findColumn( columnName );
		}
		catch( SQLException e ) {
			throw new HibernateException( "Could not resolve column name in result set [" + columnName + "]", e );
		}
	}

	public String getColumnName(int position) throws HibernateException {
		try {
			return factory.getDialect().getColumnAliasExtractor().extractColumnAlias( resultSetMetaData, position );
		}
		catch( SQLException e ) {
			throw new HibernateException( "Could not resolve column name [" + position + "]", e );
		}
	}

	public Type getHibernateType(int columnPos) throws SQLException {
		int columnType = resultSetMetaData.getColumnType( columnPos );
		int scale = resultSetMetaData.getScale( columnPos );
		int precision = resultSetMetaData.getPrecision( columnPos );

		int length = precision;
		if ( columnType == Types.CHAR && precision == 0 ) {
			length = resultSetMetaData.getColumnDisplaySize( columnPos );
		}

		return factory.getTypeResolver().heuristicType(
				factory.getDialect().getHibernateTypeName(
						columnType,
						length,
						precision,
						scale
				)
		);
	}
}
