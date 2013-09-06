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
package org.hibernate.cfg.annotations;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.Mappings;

/**
 * Manages the resolution of the AttributeConverter, if one, for a property (path).
 *
 * @author Steve Ebersole
 */
public class AttributeConverterResolverContext {
	private final Mappings mappings;

	public AttributeConverterResolverContext(Mappings mappings) {
		this.mappings = mappings;
	}

	private Map<String,EntityPropertyPathSource> entityPropertyPathSourceMap;

	public EntityPropertyPathSource resolveEntityPropertyPathSource(XClass entityClass) {
		EntityPropertyPathSource found = null;
		if ( entityPropertyPathSourceMap == null ) {
			entityPropertyPathSourceMap = new HashMap<String, EntityPropertyPathSource>();
		}
		else {
			found = entityPropertyPathSourceMap.get( entityClass.getName() );
		}

		if ( found == null ) {
			found = new EntityPropertyPathSource( entityClass );
			entityPropertyPathSourceMap.put( entityClass.getName(), found );
		}

		return found;
	}

	private Map<String,CollectionPropertyPathSource> collectionPropertyPathSourceMap;

	public CollectionPropertyPathSource resolveRootCollectionPropertyPathSource(XClass owner, XProperty property) {
		CollectionPropertyPathSource found = null;
		if ( collectionPropertyPathSourceMap == null ) {
			collectionPropertyPathSourceMap = new HashMap<String, CollectionPropertyPathSource>();
		}
		else {
			found = collectionPropertyPathSourceMap.get( owner.getName() );
		}

		if ( found == null ) {
			final EntityPropertyPathSource ownerSource = resolveEntityPropertyPathSource( owner );
			found = new CollectionPropertyPathSource( ownerSource, property.getName() );
			collectionPropertyPathSourceMap.put( owner.getName(), found );
		}

		return found;
	}

	public static interface PropertyPathSource {
		public CompositePropertyPathSource makeComposite(String propertyName);
		public CollectionPropertyPathSource makeCollection(String propertyName);
	}

	public static abstract class AbstractPropertyPathSource implements PropertyPathSource {
		protected abstract String normalize(String path);

		private Map<String,CompositePropertyPathSource> compositePropertyPathSourceMap;

		@Override
		public CompositePropertyPathSource makeComposite(String propertyName) {
			CompositePropertyPathSource found = null;
			if ( compositePropertyPathSourceMap == null ) {
				compositePropertyPathSourceMap = new HashMap<String, CompositePropertyPathSource>();
			}
			else {
				found = compositePropertyPathSourceMap.get( propertyName );
			}

			if ( found == null ) {
				found = new CompositePropertyPathSource( this, propertyName );
				compositePropertyPathSourceMap.put( propertyName, found );
			}

			return found;
		}

		private Map<String,CollectionPropertyPathSource> collectionPropertyPathSourceMap;

		@Override
		public CollectionPropertyPathSource makeCollection(String propertyName) {
			CollectionPropertyPathSource found = null;
			if ( collectionPropertyPathSourceMap == null ) {
				collectionPropertyPathSourceMap = new HashMap<String, CollectionPropertyPathSource>();
			}
			else {
				found = collectionPropertyPathSourceMap.get( propertyName );
			}

			if ( found == null ) {
				found = new CollectionPropertyPathSource( this, propertyName );
				collectionPropertyPathSourceMap.put( propertyName, found );
			}

			return found;
		}
	}

	public static class EntityPropertyPathSource extends AbstractPropertyPathSource implements PropertyPathSource {
		private final XClass entityClass;

		private EntityPropertyPathSource(XClass entityClass) {
			this.entityClass = entityClass;
		}

		@Override
		protected String normalize(String path) {
			return path;
		}
	}

	public static class CompositePropertyPathSource extends AbstractPropertyPathSource implements PropertyPathSource {
		private final AbstractPropertyPathSource sourceOfComposite;
		private final String compositeName;

		public CompositePropertyPathSource(AbstractPropertyPathSource sourceOfComposite, String compositeName) {
			this.sourceOfComposite = sourceOfComposite;
			this.compositeName = compositeName;
		}

		@Override
		protected String normalize(String path) {
			return getSourceOfComposite().normalize( compositeName ) + "." + path;
		}

		public AbstractPropertyPathSource getSourceOfComposite() {
			return sourceOfComposite;
		}

		public String getCompositeName() {
			return compositeName;
		}
	}

	public static class CollectionPropertyPathSource extends AbstractPropertyPathSource implements PropertyPathSource {
		private final AbstractPropertyPathSource collectionSource;
		private final String collectionName;

		private CollectionPropertyPathSource(AbstractPropertyPathSource collectionSource, String collectionName) {
			this.collectionSource = collectionSource;
			this.collectionName = collectionName;
		}

		@Override
		protected String normalize(String path) {
			return path;
		}
	}

}
