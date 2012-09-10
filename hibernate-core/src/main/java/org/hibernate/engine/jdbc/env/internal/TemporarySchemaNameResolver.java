/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.env.internal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;

/**
 * Temporary implementation that works for H2.
 *
 * @author Steve Ebersole
 */
public class TemporarySchemaNameResolver implements SchemaNameResolver {
	public static final TemporarySchemaNameResolver INSTANCE = new TemporarySchemaNameResolver();

	@Override
	public String resolveSchemaName(Connection connection) throws SQLException {
		// the H2 variant...
		Statement statement = connection.createStatement();
		try {
			ResultSet resultSet = statement.executeQuery( "call schema()" );
			try {
				if ( ! resultSet.next() ) {
					return null;
				}
				return resultSet.getString( 1 );
			}
			finally {
				try {
					resultSet.close();
				}
				catch (SQLException ignore) {
				}
			}
		}
		finally {
			try {
				statement.close();
			}
			catch (SQLException ignore) {
			}
		}
	}
}
