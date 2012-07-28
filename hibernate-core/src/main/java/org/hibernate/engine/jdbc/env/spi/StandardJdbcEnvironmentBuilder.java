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
package org.hibernate.engine.jdbc.env.spi;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.spi.SchemaNameResolver;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.service.schema.internal.ExistingSequenceMetadataImpl;
import org.hibernate.service.schema.spi.ExistingSequenceMetadata;
import org.hibernate.service.schema.spi.ExistingSequenceMetadataExtractor;
import org.hibernate.service.schema.spi.IdentifierHelper;

/**
 * @author Steve Ebersole
 */
public class StandardJdbcEnvironmentBuilder {
	public static final StandardJdbcEnvironmentBuilder INSTANCE = new StandardJdbcEnvironmentBuilder();

	private static final SchemaNameResolver FOR_NOW_SCHEMA_NAME_RESOLVER = new SchemaNameResolver() {
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
	};

	private static final ExistingSequenceMetadataExtractor FOR_NOW_SEQ_META_EXTRACTOR = new ExistingSequenceMetadataExtractor() {
		@Override
		public Iterable<ExistingSequenceMetadata> extractMetadata(
				DatabaseMetaData databaseMetaData,
				IdentifierHelper identifierHelper) throws SQLException {
			// again, the H2 variant...
			Statement statement = databaseMetaData.getConnection().createStatement();
			try {
				ResultSet resultSet = statement.executeQuery(
						"select SEQUENCE_CATALOG, SEQUENCE_SCHEMA, SEQUENCE_NAME " +
								"from information_schema.sequences"
				);
				try {
					final List<ExistingSequenceMetadata> sequenceMetadataList = new ArrayList<ExistingSequenceMetadata>();
					while ( resultSet.next() ) {
						sequenceMetadataList.add(
								new ExistingSequenceMetadataImpl(
										new ObjectName(
												identifierHelper.fromMetaDataCatalogName(
														resultSet.getString(
																"SEQUENCE_CATALOG"
														)
												),
												identifierHelper.fromMetaDataSchemaName( resultSet.getString( "SEQUENCE_SCHEMA" ) ),
												identifierHelper.fromMetaDataCatalogName( resultSet.getString( "SEQUENCE_NAME" ) )
										)
								)
						);
					}
					return sequenceMetadataList;
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
	};

	public JdbcEnvironment buildJdbcEnvironment(DatabaseMetaData dbmd, Dialect dialect) throws SQLException {
		SchemaCatalogSupport schemaCatalogSupport = new StandardSchemaCatalogSupportImpl(
				dbmd.getCatalogSeparator(),
				dbmd.isCatalogAtStart(),
				dialect.openQuote(),
				dialect.closeQuote()
		);

		Set<String> reservedWords = new HashSet<String>();
		reservedWords.addAll( dialect.getKeywords() );
		// todo : do we need to explicitly handle SQL:2003 keywords?
		reservedWords.addAll( Arrays.asList( dbmd.getSQLKeywords().split( "," ) ) );

		return new JdbcEnvironmentImpl(
				dialect,
				schemaCatalogSupport,
				FOR_NOW_SCHEMA_NAME_RESOLVER,
				FOR_NOW_SEQ_META_EXTRACTOR,
				reservedWords
		);
	}

	public JdbcEnvironment buildJdbcEnvironment(Dialect dialect) throws SQLException {
		SchemaCatalogSupport schemaCatalogSupport = new StandardSchemaCatalogSupportImpl( dialect );

		Set<String> reservedWords = new HashSet<String>();
		reservedWords.addAll( dialect.getKeywords() );

		return new JdbcEnvironmentImpl(
				dialect,
				schemaCatalogSupport,
				FOR_NOW_SCHEMA_NAME_RESOLVER,
				FOR_NOW_SEQ_META_EXTRACTOR,
				reservedWords
		);
	}
}
