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

/**
 * @author Gail Badner
 */
public class LocalBindingContextManagerImpl implements EntityHierarchyHelper.LocalBindingContextManager {
	private final LocalBindingContextsImpl localBindingContexts = new LocalBindingContextsImpl();

	@Override
	public void cleanupLocalBindingContexts() {
		localBindingContexts.cleanup();
	}

	@Override
	public void setupLocalBindingContexts(final EntityHierarchy entityHierarchy) {
		localBindingContexts.setup( entityHierarchy );
	}

	@Override
	public void pushSubEntitySource(EntitySource entitySource) {
		localBindingContexts.pushEntitySource( entitySource );
	}

	@Override
	public void popSubEntitySource() {
		localBindingContexts.popEntitySource();
	}

	@Override
	public EntityMode getEntityMode() {
		return entityHierarchy().getRootEntitySource().getEntityMode();
	}

	@Override
	public InheritanceType getInheritanceType() {
		return entityHierarchy().getHierarchyInheritanceType();
	}

	@Override
	public RootEntitySource getRootEntitySource() {
		return entityHierarchy().getRootEntitySource();
	}

	@Override
	public EntitySource getEntitySource() {
		if ( localBindingContexts.isEmpty() ) {
			throw new IllegalStateException( "No LocalBindingContext defined." );
		}
		return localBindingContexts.entitySource();
	}

	@Override
	public EntityBinding getEntityBinding() {
		return getMetadataImplementor().getEntityBinding( getEntitySource().getEntityName() );
	}

	@Override
	public EntityBinding getSuperEntityBinding() {
		final EntitySource superEntitySource = getSuperEntitySource();
		return superEntitySource == null ?
				null :
				getMetadataImplementor().getEntityBinding( superEntitySource.getEntityName() );
	}

	@Override
	public LocalBindingContext localBindingContext() {
		return getEntitySource().getLocalBindingContext();
	}

	private MetadataImplementor getMetadataImplementor() {
		return localBindingContext().getMetadataImplementor();
	}

	private EntitySource getSuperEntitySource() {
		if ( localBindingContexts.isEmpty() ) {
			throw new IllegalStateException( "No LocalBindingContext defined." );
		}
		return localBindingContexts.superEntitySource();
	}

	private EntityHierarchy entityHierarchy() {
		if ( localBindingContexts.isEmpty() ) {
			throw new IllegalStateException( "No LocalBindingContext defined." );
		}
		return localBindingContexts.entityHierarchy();
	}

	// Each EntitySource contains its LocalBindingContext.
	private class LocalBindingContextsImpl {
		private EntityHierarchy entityHierarchy;
		private final LinkedList<EntitySource> entitySources = new LinkedList<EntitySource>(  );

		private boolean isEmpty() {
			return entityHierarchy == null;
		}

		private void setup(final EntityHierarchy entityHierarchy) {
			// Inheritance type and entity mode applies to entire hierarchy
			if ( entityHierarchy == null || entityHierarchy.getRootEntitySource() == null ) {
				throw new IllegalArgumentException(
						"entityHierarchy and entityHierarchy.getRootEntitySource() must be non-null."
				);
			}
			if ( this.entityHierarchy != null ) {
				throw new IllegalStateException( "Attempt to initialize entityHierarchy when it is already initialized." );
			}
			this.entityHierarchy = entityHierarchy;
			this.entitySources.push( entityHierarchy.getRootEntitySource() );
		}

		private void cleanup() {
			entityHierarchy = null;
			entitySources.clear();
		}

		private void pushEntitySource(EntitySource entitySource) {
			entitySources.push( entitySource );
		}

		private void popEntitySource() {
			entitySources.pop();
		}

		private EntityHierarchy entityHierarchy() {
			return entityHierarchy;
		}

		private EntitySource entitySource() {
			return entitySources.peek();
		}

		private EntitySource superEntitySource() {
			if ( entitySources.size() == 1 ) {
				return null;
			}
			final EntitySource currentEntitySource = entitySources.pop();
			final EntitySource superEntitySource = entitySources.peek();
			entitySources.push( currentEntitySource );
			return superEntitySource;
		}
	}
}