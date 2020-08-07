/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.internal.ScalarResultMappingMemento;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * ResultBuilder for scalar results defined via:<ul>
 *     <li>JPA {@link javax.persistence.ColumnResult}</li>
 *     <li>`<return-scalar/>` as part of a `<resultset/>` stanza in `hbm.xml`</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class CompleteResultBuilderBasicValuedStandard implements CompleteResultBuilderBasicValued {

	private final String explicitColumnName;

	private final BasicType<?> explicitType;
	private final JavaTypeDescriptor<?> explicitJavaTypeDescriptor;

	public CompleteResultBuilderBasicValuedStandard(
			ScalarResultMappingMemento memento,
			ResultSetMappingResolutionContext context) {
		this.explicitColumnName = memento.getExplicitColumnName();
		this.explicitType = memento.getExplicitType();
		this.explicitJavaTypeDescriptor = memento.getExplicitJavaTypeDescriptor();
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			Consumer<SqlSelection> sqlSelectionConsumer,
			DomainResultCreationState domainResultCreationState) {
		final SessionFactoryImplementor sessionFactory = domainResultCreationState.getSqlAstCreationState()
				.getCreationContext()
				.getSessionFactory();

		final int jdbcPosition;
		final String columnName;

		if ( explicitColumnName != null ) {
			jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( explicitColumnName );
			columnName = explicitColumnName;
		}
		else {
			jdbcPosition = resultPosition + 1;
			columnName = jdbcResultsMetadata.resolveColumnName( jdbcPosition );
		}

		final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );

		final BasicType<?> basicType;

		if ( explicitType != null ) {
			basicType = explicitType;
		}
		else if ( explicitJavaTypeDescriptor != null ) {
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

			final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( jdbcPosition );

			basicType = typeConfiguration.getBasicTypeRegistry().resolve( explicitJavaTypeDescriptor, sqlTypeDescriptor );
		}
		else {
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

			final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( jdbcPosition );
			final JavaTypeDescriptor<?> javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( typeConfiguration );

			basicType = typeConfiguration.getBasicTypeRegistry().resolve( javaTypeDescriptor, sqlTypeDescriptor );
		}

		final SqlSelectionImpl sqlSelection = new SqlSelectionImpl( valuesArrayPosition, (BasicValuedMapping) basicType );
		sqlSelectionConsumer.accept( sqlSelection );

		return new BasicResult<>( valuesArrayPosition, columnName, basicType.getJavaTypeDescriptor() );
	}

}
