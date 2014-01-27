/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal;

import org.hibernate.EntityMode;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;

/**
 * @author Gail Badner
 */
public class EntityHierarchyHelper {

	public interface LocalBindingContextExecutor {
		void execute(LocalBindingContextExecutionContext bindingContextContext);
	}

	public interface LocalBindingContextExecutionContext {
		RootEntitySource getRootEntitySource();
		EntitySource getEntitySource();
		EntityBinding getEntityBinding();
		EntityBinding getSuperEntityBinding();
		InheritanceType getInheritanceType();
		EntityMode getEntityMode();
	}

	public static interface LocalBindingContextManager extends LocalBindingContextExecutionContext  {
		LocalBindingContext localBindingContext();
		void cleanupLocalBindingContexts();
		void setupLocalBindingContexts(final EntityHierarchy entityHierarchy);
		void pushSubEntitySource(EntitySource entitySource);
		void popSubEntitySource();
	}

	private LocalBindingContextManager localBindingContextManager;

	EntityHierarchyHelper(final LocalBindingContextManager localBindingContextManager) {
		this.localBindingContextManager = localBindingContextManager;
	}

	/**
	 * Apply executors to all entity hierarchies.
	 */
	public void applyToAllEntityHierarchies(
			final Iterable<EntityHierarchy> entityHierarchies,
			final LocalBindingContextExecutor rootEntityExecutor,
			final LocalBindingContextExecutor subEntityExecutor) {
		for ( final EntityHierarchy entityHierarchy : entityHierarchies ) {
			applyToEntityHierarchy( entityHierarchy, rootEntityExecutor, subEntityExecutor );
		}
	}

	/**
	 * Apply executors to a single entity hierarchy.
	 *
	 * @param entityHierarchy The entity hierarchy to be binded.
	 * @param rootEntityExecutor The executor to be applied to the root {@link EntitySource}
	 *                           in the entity hierarchy.
	 * @param subEntityExecutor The executer to be applied to each {@link SubclassEntitySource}
	 *                           in the entity hierarchy.
	 */
	public void applyToEntityHierarchy(
			final EntityHierarchy entityHierarchy,
			final LocalBindingContextExecutor rootEntityExecutor,
			final LocalBindingContextExecutor subEntityExecutor) {
		localBindingContextManager.cleanupLocalBindingContexts();
		localBindingContextManager.setupLocalBindingContexts( entityHierarchy );
		try {
			rootEntityExecutor.execute( localBindingContextManager );
			if ( entityHierarchy.getHierarchyInheritanceType() != InheritanceType.NO_INHERITANCE ) {
				applyToSubEntities( subEntityExecutor );
			}
		}
		finally {
			localBindingContextManager.cleanupLocalBindingContexts();
		}
	}

	private void applyToSubEntities(final LocalBindingContextExecutor subEntityExecutor) {
		for ( final SubclassEntitySource subEntitySource : localBindingContextManager.getEntitySource().subclassEntitySources() ) {
			applyToSubEntity( subEntitySource, subEntityExecutor );
		}
	}

	private void applyToSubEntity(final EntitySource entitySource, final LocalBindingContextExecutor subEntityExecutor) {
		localBindingContextManager.pushSubEntitySource( entitySource );
		try {
			subEntityExecutor.execute( localBindingContextManager );
			applyToSubEntities( subEntityExecutor );
		}
		finally {
			localBindingContextManager.popSubEntitySource();
		}
	}
}
