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

import java.util.LinkedList;

import org.hibernate.EntityMode;
import org.hibernate.metamodel.spi.MetadataImplementor;
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

	private MetadataImplementor metadata;

	// the inheritanceTypes and entityModes correspond with bindingContexts
	private final LinkedList<LocalBindingContext> bindingContexts = new LinkedList<LocalBindingContext>();
	private final LinkedList<InheritanceType> inheritanceTypes = new LinkedList<InheritanceType>();
	private final LinkedList<EntityMode> entityModes = new LinkedList<EntityMode>();

	EntityHierarchyHelper(final MetadataImplementor metadata) {
		this.metadata = metadata;
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
	 */
	public void applyToEntityHierarchy(
			final EntityHierarchy entityHierarchy,
			final LocalBindingContextExecutor rootEntityExecutor,
			final LocalBindingContextExecutor subEntityExecutor) {
		bindingContexts.clear();
		inheritanceTypes.clear();
		entityModes.clear();
		final RootEntitySource rootEntitySource = entityHierarchy.getRootEntitySource();
		setupBindingContext( entityHierarchy, rootEntitySource );
		try {
			LocalBindingContextExecutionContext executionContext =
					new LocalBindingContextExecutionContextImpl( rootEntitySource, null );
			rootEntityExecutor.execute( executionContext );
			if ( inheritanceTypes.peek() != InheritanceType.NO_INHERITANCE ) {
				applyToSubEntities(
						executionContext.getEntityBinding(),
						rootEntitySource,
						subEntityExecutor );
			}
		}
		finally {
			cleanupBindingContext();
		}
	}

	private void cleanupBindingContext() {
		bindingContexts.pop();
		inheritanceTypes.pop();
		entityModes.pop();
	}

	public LocalBindingContext bindingContext() {
		return bindingContexts.peek();
	}

	public InheritanceType inheritanceType() {
		return inheritanceTypes.peek();
	}

	public EntityMode entityMode() {
		return entityModes.peek();
	}

	private void setupBindingContext(
			final EntityHierarchy entityHierarchy,
			final RootEntitySource rootEntitySource) {
		// Save inheritance type and entity mode that will apply to entire hierarchy
		inheritanceTypes.push( entityHierarchy.getHierarchyInheritanceType() );
		entityModes.push( rootEntitySource.getEntityMode() );
		bindingContexts.push( rootEntitySource.getLocalBindingContext() );
	}


	private void applyToSubEntities(
			final EntityBinding entityBinding,
			final EntitySource entitySource,
			final LocalBindingContextExecutor subEntityExecutor) {
		for ( final SubclassEntitySource subEntitySource : entitySource.subclassEntitySources() ) {
			applyToSubEntity( entityBinding, subEntitySource, subEntityExecutor );
		}
	}

	private void applyToSubEntity(
			final EntityBinding superEntityBinding,
			final EntitySource entitySource,
			final LocalBindingContextExecutor subEntityExecutor) {
		final LocalBindingContext bindingContext = entitySource.getLocalBindingContext();
		bindingContexts.push( bindingContext );
		try {
			LocalBindingContextExecutionContext executionContext =
					new LocalBindingContextExecutionContextImpl( entitySource, superEntityBinding );
			subEntityExecutor.execute( executionContext );
			applyToSubEntities( executionContext.getEntityBinding(), entitySource, subEntityExecutor );
		}
		finally {
			bindingContexts.pop();
		}
	}

	public interface LocalBindingContextExecutionContext {
		EntitySource getEntitySource();
		EntityBinding getEntityBinding();
		EntityBinding getSuperEntityBinding();
	}

	private class LocalBindingContextExecutionContextImpl implements LocalBindingContextExecutionContext {
		private final EntitySource entitySource;
		private final EntityBinding superEntityBinding;

		private LocalBindingContextExecutionContextImpl(
				EntitySource entitySource,
				EntityBinding superEntityBinding) {
			this.entitySource = entitySource;
			this.superEntityBinding = superEntityBinding;
		}

		@Override
		public EntitySource getEntitySource() {
			return entitySource;
		}
		@Override
		public EntityBinding getEntityBinding() {
			return metadata.getEntityBinding( entitySource.getEntityName() );
		}
		@Override
		public EntityBinding getSuperEntityBinding() {
			return superEntityBinding;
		}
	}
}
