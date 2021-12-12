/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.function.BiFunction;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.model.convert.internal.JpaAttributeConverterImpl;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.query.results.ResultsHelper.impl;

/**
 * ResultBuilder for scalar results defined via:<ul>
 *     <li>JPA {@link jakarta.persistence.ColumnResult}</li>
 *     <li>`<return-scalar/>` as part of a `<resultset/>` stanza in `hbm.xml`</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class CompleteResultBuilderBasicValuedConverted<O,R> implements CompleteResultBuilderBasicValued {
	private final String explicitColumnName;
	private final ManagedBean<? extends AttributeConverter<O, R>> converterBean;
	private final JavaType<? extends AttributeConverter<O, R>> converterJtd;
	private final BasicJavaType<O> domainJavaType;
	private final BasicValuedMapping underlyingMapping;

	public CompleteResultBuilderBasicValuedConverted(
			String explicitColumnName,
			ManagedBean<? extends AttributeConverter<O, R>> converterBean,
			JavaType<? extends AttributeConverter<O, R>> converterJtd,
			BasicJavaType<O> domainJavaType,
			BasicValuedMapping underlyingMapping) {
		this.explicitColumnName = explicitColumnName;
		this.converterBean = converterBean;
		this.converterJtd = converterJtd;
		this.domainJavaType = domainJavaType;
		this.underlyingMapping = underlyingMapping;
	}

	@Override
	public Class<?> getJavaType() {
		return domainJavaType.getJavaTypeClass();
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );
		final SessionFactoryImplementor sessionFactory = creationStateImpl.getSessionFactory();

		final String columnName;
		if ( explicitColumnName != null ) {
			columnName = explicitColumnName;
		}
		else {
			columnName = jdbcResultsMetadata.resolveColumnName( resultPosition + 1 );
		}


//		final int jdbcPosition;
//		if ( explicitColumnName != null ) {
//			jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( explicitColumnName );
//		}
//		else {
//			jdbcPosition = resultPosition + 1;
//		}
//
//		final BasicValuedMapping basicType;
//		if ( explicitType != null ) {
//			basicType = explicitType;
//		}
//		else {
//			basicType = jdbcResultsMetadata.resolveType( jdbcPosition, explicitJavaTypeDescriptor );
//		}
//
//		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
//				creationStateImpl.resolveSqlExpression(
//						columnName,
//						processingState -> {
//							final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
//							return new SqlSelectionImpl( valuesArrayPosition, basicType );
//						}
//				),
//				basicType.getExpressableJavaTypeDescriptor(),
//				sessionFactory.getTypeConfiguration()
//		);


		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
				creationStateImpl.resolveSqlExpression(
						columnName,
						processingState -> {
							final int jdbcPosition;
							if ( explicitColumnName != null ) {
								jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( explicitColumnName );
							}
							else {
								jdbcPosition = resultPosition + 1;
							}

							final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
							return new SqlSelectionImpl( valuesArrayPosition, underlyingMapping );
						}
				),
				domainJavaType,
				sessionFactory.getTypeConfiguration()
		);

		final JpaAttributeConverterImpl<O,R> valueConverter = new JpaAttributeConverterImpl<>(
				converterBean,
				converterJtd,
				domainJavaType,
				underlyingMapping.getJdbcMapping().getJavaTypeDescriptor()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnName,
				domainJavaType,
				valueConverter
		);
	}
}
