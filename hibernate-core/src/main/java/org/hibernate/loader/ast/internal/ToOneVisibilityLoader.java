/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.sql.exec.spi.JdbcParametersList;

/** Checks association visibility when the owner SQL could not supply a restricted target key. */
public final class ToOneVisibilityLoader {
	private final ToOneAttributeMapping association;
	private volatile @Nullable SingleIdLoadPlan<Object> identifierPlan;
	private volatile @Nullable SingleIdLoadPlan<Object> uniqueKeyPlan;

	public ToOneVisibilityLoader(ToOneAttributeMapping association) {
		this.association = association;
	}

	public boolean isVisible(Object key, boolean byUniqueKey, SharedSessionContractImplementor session) {
		final var influencers = session.getLoadQueryInfluencers();
		final SingleIdLoadPlan<Object> plan;
		if ( association.isAffectedByEnabledFilters( influencers ) ) {
			// Filter parameter bindings belong to the current load, not the shared plan.
			plan = createPlan( byUniqueKey, influencers );
		}
		else {
			var cached = byUniqueKey ? uniqueKeyPlan : identifierPlan;
			if ( cached == null ) {
				cached = createPlan( byUniqueKey, influencers );
				if ( byUniqueKey ) {
					uniqueKeyPlan = cached;
				}
				else {
					identifierPlan = cached;
				}
			}
			plan = cached;
		}
		// Select only the identifier: an excluded association must not mark an entity as absent.
		return plan.load( key, session ) != null;
	}

	private SingleIdLoadPlan<Object> createPlan(boolean byUniqueKey, LoadQueryInfluencers influencers) {
		final var target = association.getEntityMappingType();
		final ModelPart attribute = byUniqueKey
				? target.findByPath( association.getReferencedPropertyName() ) : target.getIdentifierMapping();
		final ModelPart key = attribute instanceof ToOneAttributeMapping toOne
				? toOne.getForeignKeyDescriptor() : attribute;
		final var parameters = JdbcParametersList.newBuilder();
		final var select = LoaderSelectBuilder.createAssociationKeySelect( association, key, influencers, parameters::add );
		return new SingleIdLoadPlan<>( target, key, select, parameters.build(), LockOptions.NONE,
				influencers.getSessionFactory() );
	}
}
