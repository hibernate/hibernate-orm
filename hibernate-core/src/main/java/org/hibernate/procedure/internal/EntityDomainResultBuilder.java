/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.FetchBuilderBasicValued;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.complete.EntityResultImpl;
import org.hibernate.query.results.internal.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.internal.implicit.ImplicitFetchBuilderBasic;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Christian Beikov
 */
public class EntityDomainResultBuilder implements ResultBuilder {

	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final FetchBuilderBasicValued discriminatorFetchBuilder;

	public EntityDomainResultBuilder(EntityMappingType entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
		this.navigablePath = new NavigablePath( entityDescriptor.getEntityName() );
		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		if ( discriminatorMapping == null ) {
			this.discriminatorFetchBuilder = null;
		}
		else {
			this.discriminatorFetchBuilder = new ImplicitFetchBuilderBasic(
					navigablePath,
					discriminatorMapping
			);
		}
	}

	@Override
	public Class<?> getJavaType() {
		return entityDescriptor.getJavaType().getJavaTypeClass();
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public EntityResult buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {

		return new EntityResultImpl(
				navigablePath,
				entityDescriptor,
				null,
				LockMode.NONE,
				entityResult -> {
					if ( discriminatorFetchBuilder == null ) {
						return null;
					}
					return discriminatorFetchBuilder.buildFetch(
							entityResult,
							navigablePath.append( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME ),
							jdbcResultsMetadata,
							legacyFetchResolver,
							domainResultCreationState
					);
				},
				domainResultCreationState
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final EntityDomainResultBuilder that = (EntityDomainResultBuilder) o;
		return entityDescriptor.equals( that.entityDescriptor );
	}

	@Override
	public int hashCode() {
		return entityDescriptor.hashCode();
	}
}
