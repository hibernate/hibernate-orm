/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @see javax.persistence.ColumnResult
 *
 * @author Steve Ebersole
 */
public class ScalarResultBuilder implements ResultBuilder {
	private final String explicitName;

	private final BasicType<?> explicitType;
	private final JavaTypeDescriptor<?> explicitJavaTypeDescriptor;

	ScalarResultBuilder(String explicitName, BasicType<?> explicitType) {
		assert explicitName != null;
		this.explicitName = explicitName;

		assert explicitType != null;
		this.explicitType = explicitType;

		this.explicitJavaTypeDescriptor = null;
	}

	ScalarResultBuilder(String explicitName, JavaTypeDescriptor<?> explicitJavaTypeDescriptor) {
		assert explicitName != null;
		this.explicitName = explicitName;

		assert explicitJavaTypeDescriptor != null;
		this.explicitJavaTypeDescriptor = explicitJavaTypeDescriptor;

		this.explicitType = null;
	}

	public ScalarResultBuilder(String explicitName) {
		assert explicitName != null;
		this.explicitName = explicitName;

		this.explicitType = null;
		this.explicitJavaTypeDescriptor = null;
	}

	public String getExplicitName() {
		return explicitName;
	}

	@Override
	public DomainResult<?> buildReturn(
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, LegacyFetchBuilder> legacyFetchResolver,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SessionFactoryImplementor sessionFactory) {
		final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( explicitName );
		final int valuesArrayPosition = jdbcPosition - 1;

		final BasicType<?> jdbcMapping;

		if ( explicitType != null ) {
			jdbcMapping = explicitType;
		}
		else if ( explicitJavaTypeDescriptor != null ) {
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

			final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( jdbcPosition );

			jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( explicitJavaTypeDescriptor, sqlTypeDescriptor );
		}
		else {
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

			final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( jdbcPosition );
			final JavaTypeDescriptor<?> javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( typeConfiguration );

			jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( javaTypeDescriptor, sqlTypeDescriptor );
		}

		final SqlSelectionImpl sqlSelection = new SqlSelectionImpl( valuesArrayPosition, jdbcMapping );
		sqlSelectionConsumer.accept( sqlSelection );

		return new BasicResult( valuesArrayPosition, explicitName, jdbcMapping.getJavaTypeDescriptor() );
	}

}
