/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.Consumer;

import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.filter.internal.FilterHelper;
import org.hibernate.sql.RestrictionRendering;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.predicate.SqlFragmentPredicate;

/** Restrictions belonging to an association, independently of its target entity's restrictions. */
final class ToOneRestrictions {
	static final ToOneRestrictions NONE = new ToOneRestrictions( null, null );
	private static final String[] NO_FILTERS = new String[0];

	private final @Nullable String sqlTemplate;
	private final @Nullable FilterHelper filters;
	private volatile @Nullable RestrictionRendering sqlRendering;

	private ToOneRestrictions(@Nullable String sqlTemplate, @Nullable FilterHelper filters) {
		this.sqlTemplate = sqlTemplate;
		this.filters = filters;
	}

	static ToOneRestrictions create(ToOne bootMapping, EntityMappingType target) {
		final String restriction = bootMapping.getSqlRestriction();
		final var configurations = bootMapping.getFilters();
		if ( (restriction == null || restriction.isBlank()) && configurations.isEmpty() ) {
			return NONE;
		}
		final var factory = target.getEntityPersister().getFactory();
		return new ToOneRestrictions(
				restriction == null || restriction.isBlank() ? null : Template.renderWhereStringTemplate(
						"(" + restriction + ")", factory.getJdbcServices().getDialect(), factory.getTypeConfiguration() ),
				configurations.isEmpty() ? null : new FilterHelper( configurations,
						AbstractEntityPersister.getEntityNameByTableNameMap(
								bootMapping.getMetadata().getEntityBinding( bootMapping.getReferencedEntityName() ),
								factory.getSqlStringGenerationContext() ), factory ) );
	}

	boolean hasSqlRestriction() {
		return sqlTemplate != null;
	}

	boolean hasFilters() {
		return filters != null;
	}

	String[] getFilterNames() {
		return filters == null ? NO_FILTERS : filters.getFilterNames();
	}

	boolean isAffectedByFilters(LoadQueryInfluencers influencers) {
		return filters != null && filters.isAffectedBy( influencers.getEnabledFilters() );
	}

	void apply(Consumer<Predicate> consumer, EntityMappingType target, TableGroup tableGroup,
			SqlAstCreationState creationState) {
		if ( sqlTemplate != null ) {
			var rendering = sqlRendering;
			if ( rendering == null ) {
				// Runtime attributes might not exist yet when the to-one mapping is constructed.
				sqlRendering = rendering = RestrictionRendering.compile( sqlTemplate, target );
			}
			final var reference = tableGroup.resolveTableReference( target.getEntityPersister().getTableName() );
			final String alias = reference.getIdentificationVariable() != null
					? reference.getIdentificationVariable() : reference.getTableId();
			consumer.accept( new SqlFragmentPredicate( rendering.render( alias, true, tableGroup, creationState ) ) );
		}
		if ( filters != null ) {
			filters.applyEnabledFilters( consumer, target.getEntityPersister().getFilterAliasGenerator( tableGroup ),
					creationState.getLoadQueryInfluencers().getEnabledFilters(), false, tableGroup, creationState );
		}
	}
}
