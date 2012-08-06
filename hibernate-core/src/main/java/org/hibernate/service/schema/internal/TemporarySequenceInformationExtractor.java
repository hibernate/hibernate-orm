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
package org.hibernate.service.schema.internal;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.service.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.schema.spi.SequenceInformation;
import org.hibernate.service.schema.spi.SequenceInformationExtractor;

/**
 * Temporary implementation that works for H2.
 *
 * @author Steve Ebersole
 */
public class TemporarySequenceInformationExtractor implements SequenceInformationExtractor {
	private final JdbcEnvironment jdbcEnvironment;

	public TemporarySequenceInformationExtractor(JdbcEnvironment jdbcEnvironment) {
		this.jdbcEnvironment = jdbcEnvironment;
	}

	@Override
	public Iterable<SequenceInformation> extractMetadata(DatabaseMetaData databaseMetaData) throws SQLException {
		Statement statement = databaseMetaData.getConnection().createStatement();
		try {
			ResultSet resultSet = statement.executeQuery(
					"select SEQUENCE_CATALOG, SEQUENCE_SCHEMA, SEQUENCE_NAME, INCREMENT " +
							"from information_schema.sequences"
			);
			try {
				final List<SequenceInformation> sequenceInformationList = new ArrayList<SequenceInformation>();
				while ( resultSet.next() ) {
					sequenceInformationList.add(
							new SequenceInformationImpl(
									new ObjectName(
											jdbcEnvironment.getIdentifierHelper().fromMetaDataCatalogName(
													resultSet.getString(
															"SEQUENCE_CATALOG"
													)
											),
											jdbcEnvironment.getIdentifierHelper().fromMetaDataSchemaName(
													resultSet.getString(
															"SEQUENCE_SCHEMA"
													)
											),
											jdbcEnvironment.getIdentifierHelper().fromMetaDataCatalogName(
													resultSet.getString(
															"SEQUENCE_NAME"
													)
											)
									),
									resultSet.getInt( "INCREMENT" )
							)
					);
				}
				return sequenceInformationList;
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
