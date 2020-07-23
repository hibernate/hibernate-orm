/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class Builders {
	public static ScalarResultBuilder scalar(
			String columnAlias,
			BasicType<?> type) {
		return new ScalarResultBuilder( columnAlias, type );
	}

	public static ScalarResultBuilder scalar(
			String columnAlias,
			Class<?> javaType,
			SessionFactoryImplementor factory) {
		return new ScalarResultBuilder(
				columnAlias,
				factory.getTypeConfiguration().getBasicTypeForJavaType( javaType )
		);
	}

	public static DomainResult<?> implicitScalarDomainResult(
			int colIndex,
			String columnName,
			JdbcValuesMetadata jdbcResultsMetadata,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SessionFactoryImplementor sessionFactory) {
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
		final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( colIndex );
		final BasicJavaDescriptor<?> javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( typeConfiguration );
		final BasicType<?> jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( javaTypeDescriptor, sqlTypeDescriptor );
		sqlSelectionConsumer.accept( new SqlSelectionImpl( colIndex, jdbcMapping ) );
		return new BasicResult<>( colIndex, columnName, javaTypeDescriptor );
	}

	public static EntityResultBuilder entity(String tableAlias, String entityName) {
		throw new NotYetImplementedFor6Exception( );
	}

	public static LegacyFetchBuilder fetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		throw new NotYetImplementedFor6Exception( );
	}
}
