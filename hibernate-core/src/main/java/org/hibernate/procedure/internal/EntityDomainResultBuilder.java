/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.complete.EntityResultImpl;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.implicit.ImplicitModelPartResultBuilderBasic;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Christian Beikov
 */
public class EntityDomainResultBuilder<T> implements ResultBuilder {

	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final ResultBuilderBasicValued discriminatorResultBuilder;

	public EntityDomainResultBuilder(EntityMappingType entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
		this.navigablePath = new NavigablePath( entityDescriptor.getEntityName() );
		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		if ( discriminatorMapping == null ) {
			this.discriminatorResultBuilder = null;
		}
		else {
			this.discriminatorResultBuilder = new ImplicitModelPartResultBuilderBasic(
					navigablePath,
					discriminatorMapping
			);
		}
	}

	@Override
	public EntityResult buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final BasicResult<?> discriminatorResult;
		if ( discriminatorResultBuilder == null ) {
			discriminatorResult = null;
		}
		else {
			discriminatorResult = discriminatorResultBuilder.buildResult(
					jdbcResultsMetadata,
					resultPosition,
					legacyFetchResolver,
					domainResultCreationState
			);
		}

		return new EntityResultImpl(
				navigablePath,
				entityDescriptor,
				null,
				LockMode.NONE,
				discriminatorResult,
				domainResultCreationState
		);
	}
}
