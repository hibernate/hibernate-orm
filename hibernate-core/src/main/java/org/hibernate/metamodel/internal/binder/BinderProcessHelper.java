/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.binder;

import java.util.Collection;

import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.IdentifiableTypeSource;
import org.hibernate.metamodel.spi.binding.InheritanceType;

/**
 * The processes performed by Binder are done as a series of steps, where each
 * step performs a iteration on an entity hierarchy and visits the hierarchy and
 * its entities.
 * <p/>
 * This class is the visitation process.  In many ways its a simple iterator over the
 * EntitySource structure skipping MapperSuperclassSource types.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class BinderProcessHelper {
	private final BinderRootContext context;

	public BinderProcessHelper(BinderRootContext context) {
		this.context = context;
	}

	/**
	 * Public method to apply the "hierarchy strategy" to each hierarchy source
	 * object.
	 *
	 * @param hierarchySources The hierarchy sources
	 * @param hierarchyStrategy The strategy to call back into
	 */
	public void apply(
			Collection<EntityHierarchySource> hierarchySources,
			BinderStepHierarchyStrategy hierarchyStrategy) {
		for ( EntityHierarchySource hierarchySource : hierarchySources ) {
			apply( hierarchySource, hierarchyStrategy, NOOP_ENTITY_STRATEGY );
		}
	}

	/**
	 * Public method to apply the "entity strategy" to each hierarchy source
	 * object.
	 *
	 * @param hierarchySources The hierarchy sources
	 * @param entityStrategy The strategy to call back into
	 */
	public void apply(
			Collection<EntityHierarchySource> hierarchySources,
			BinderStepEntityStrategy entityStrategy) {
		for ( EntityHierarchySource hierarchySource: hierarchySources ) {
			apply( hierarchySource, NOOP_HIERARCHY_STRATEGY, entityStrategy );
		}
	}

	/**
	 * Public method to apply the "combined strategy" to each hierarchy source
	 * object.
	 *
	 * @param hierarchySources The hierarchy sources
	 * @param strategy The strategy to call back into
	 */
	public void apply(
			Collection<EntityHierarchySource> hierarchySources,
			BinderStepCombinedStrategy strategy) {
		for ( EntityHierarchySource hierarchySource : hierarchySources ) {
			apply( hierarchySource, strategy );
		}
	}


	/**
	 * Public method to apply the "combined strategy" to a single hierarchy source
	 * object.
	 *
	 * @param hierarchySource The hierarchy source
	 * @param strategy The strategy to call back into
	 */
	public void apply(EntityHierarchySource hierarchySource, BinderStepCombinedStrategy strategy) {
		apply( hierarchySource, strategy, strategy );
	}

	/**
	 * Public method to apply the "combined strategy" to each hierarchy source
	 * object.
	 *
	 * @param hierarchySources The hierarchy sources
	 * @param hierarchyStrategy The hierarchy strategy to call back into
	 * @param entityStrategy The entity strategy to call back into
	 */
	public void apply(
			Collection<EntityHierarchySource> hierarchySources,
			BinderStepHierarchyStrategy hierarchyStrategy,
			BinderStepEntityStrategy entityStrategy) {
		for ( EntityHierarchySource hierarchySource: hierarchySources ) {
			apply( hierarchySource, hierarchyStrategy, entityStrategy );
		}
	}

	public void apply(
			EntityHierarchySource hierarchySource,
			BinderStepHierarchyStrategy hierarchyStrategy,
			BinderStepEntityStrategy entityStrategy) {
		final BinderLocalBindingContextSelectorImpl selector
				= (BinderLocalBindingContextSelectorImpl) context.getLocalBindingContextSelector();

		BinderLocalBindingContext localContext = selector.setCurrent( hierarchySource.getRoot() );
		hierarchyStrategy.visit( hierarchySource, localContext );

		final EntitySource rootEntitySource = hierarchySource.getRoot();
		if ( entityStrategy.applyToRootEntity() ) {
			entityStrategy.visit( rootEntitySource, localContext );
		}

		if ( hierarchySource.getHierarchyInheritanceType() != InheritanceType.NO_INHERITANCE ) {
			visitSubclasses( rootEntitySource, entityStrategy, localContext );
		}

		entityStrategy.afterAllEntitiesInHierarchy();

		selector.unsetCurrent();
	}

	private void visitSubclasses(
			IdentifiableTypeSource source,
			BinderStepEntityStrategy entityStrategy,
			BinderLocalBindingContext localContext) {
		for ( IdentifiableTypeSource subType : source.getSubTypes() ) {
			if ( EntitySource.class.isInstance( subType ) ) {
				entityStrategy.visit( (EntitySource) subType, localContext );
			}
			visitSubclasses( subType, entityStrategy, localContext );
		}
	}


	/**
	 * A no-op version of the BinderStepHierarchyStrategy contract
	 */
	public static final BinderStepHierarchyStrategy NOOP_HIERARCHY_STRATEGY = new BinderStepHierarchyStrategy() {
		@Override
		public void visit(EntityHierarchySource source, BinderLocalBindingContext context) {
		}
	};

	/**
	 * A no-op version of the BinderStepEntityStrategy contract
	 */
	public static final BinderStepEntityStrategy NOOP_ENTITY_STRATEGY = new BinderStepEntityStrategy() {
		@Override
		public boolean applyToRootEntity() {
			return false;
		}

		@Override
		public void visit(EntitySource source, BinderLocalBindingContext context) {
		}

		@Override
		public void afterAllEntitiesInHierarchy() {
		}
	};
}
