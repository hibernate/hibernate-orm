/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.query.results.implicit;

import java.util.function.BiFunction;

import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.NamedNativeQuery;

/**
 * ResultBuilder for handling {@link NamedNativeQuery#resultClass()} when the
 * class does not refer to an entity
 *
 * @author Steve Ebersole
 */
public class ImplicitResultClassBuilder implements ResultBuilder {
	private final Class<?> suppliedResultClass;

	public ImplicitResultClassBuilder(Class<?> suppliedResultClass) {
		this.suppliedResultClass = suppliedResultClass;
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final MappingMetamodelImplementor mappingMetamodel = domainResultCreationState.getSqlAstCreationState()
				.getCreationContext()
				.getMappingMetamodel();
		final TypeConfiguration typeConfiguration = mappingMetamodel.getTypeConfiguration();
		final int jdbcResultPosition = resultPosition + 1;

		final BasicType<Object> basicType = jdbcResultsMetadata.resolveType(
				jdbcResultPosition,
				typeConfiguration.getJavaTypeRegistry().resolveDescriptor( suppliedResultClass ),
				typeConfiguration
		);

		return new BasicResult<>(
				resultPosition,
				jdbcResultsMetadata.resolveColumnName( jdbcResultPosition ),
				basicType
		);
	}

	@Override
	public Class<?> getJavaType() {
		return suppliedResultClass;
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}
}
