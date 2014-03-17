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
package org.hibernate.metamodel.internal.binder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.spi.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.AttributeSourceResolutionContext;
import org.hibernate.metamodel.source.spi.ComponentAttributeSource;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.IdentifiableTypeSource;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.IndexedPluralAttributeSource;
import org.hibernate.metamodel.source.spi.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.SimpleIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.Column;

import org.jboss.logging.Logger;

/**
 * Used to build indexes (x-refs) of various parts of an entity hierarchy and
 * its attributes.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class SourceIndex {
	private static final Logger log = CoreLogging.logger( SourceIndex.class );

	private static final String EMPTY_STRING = "";

	private final Map<String, EntitySourceIndex> entitySourceIndexByEntityName = new HashMap<String, EntitySourceIndex>();
	private final Map<AttributeSourceKey, AttributeSource> attributeSourcesByKey = new HashMap<AttributeSourceKey, AttributeSource>();
	private final Map<AttributeSourceKey, AttributeSourceKey> mappedByAttributeKeysByOwnerAttributeKeys =
			new HashMap<AttributeSourceKey, AttributeSourceKey>();


	public void indexHierarchy(EntityHierarchySource hierarchy) {
		final String hierarchyKey = hierarchy.getRoot().getEntityName();
		final HierarchyInfo hierarchyInfo = new HierarchyInfo( hierarchyKey, hierarchy );
		indexIdentifierAttributeSources( hierarchy, hierarchyInfo );

		indexIdentifiableTypeSource( hierarchy.getRoot(), hierarchyInfo );
	}

	private void indexIdentifiableTypeSource(IdentifiableTypeSource identifiableTypeSource, HierarchyInfo hierarchyInfo) {
		if ( EntitySource.class.isInstance( identifiableTypeSource ) ) {
			indexEntitySource( (EntitySource) identifiableTypeSource, hierarchyInfo );
		}

		for ( IdentifiableTypeSource subTypes : identifiableTypeSource.getSubTypes() ) {
			indexIdentifiableTypeSource( subTypes, hierarchyInfo );
		}
	}

	private void indexEntitySource(EntitySource entitySource, HierarchyInfo hierarchyInfo) {
		final String entityName = entitySource.getEntityName();
		EntitySourceIndex entitySourceIndex = new EntitySourceIndex( this, hierarchyInfo, entitySource );
		entitySourceIndexByEntityName.put( entityName, entitySourceIndex );
		log.debugf( "Indexing entity source [%s]", entityName );
		indexAttributes( entitySourceIndex );
	}

	private void indexIdentifierAttributeSources(EntityHierarchySource entityHierarchySource, HierarchyInfo hierarchyInfo) {
		final IdentifierSource identifierSource = entityHierarchySource.getIdentifierSource();

		switch ( identifierSource.getNature() ) {
			case SIMPLE: {
				final AttributeSource identifierAttributeSource =
						( (SimpleIdentifierSource) identifierSource ).getIdentifierAttributeSource();
				indexAttributeSources( hierarchyInfo, EMPTY_STRING, identifierAttributeSource, true );
				break;
			}
			case NON_AGGREGATED_COMPOSITE: {
				final List<SingularAttributeSource> nonAggregatedAttributeSources =
						( (NonAggregatedCompositeIdentifierSource) identifierSource ).getAttributeSourcesMakingUpIdentifier();
				for ( SingularAttributeSource nonAggregatedAttributeSource : nonAggregatedAttributeSources ) {
					indexAttributeSources( hierarchyInfo, EMPTY_STRING, nonAggregatedAttributeSource, true );
				}
				break;
			}
			case AGGREGATED_COMPOSITE: {
				final ComponentAttributeSource aggregatedAttributeSource =
						( (AggregatedCompositeIdentifierSource) identifierSource ).getIdentifierAttributeSource();
				indexAttributeSources( hierarchyInfo, EMPTY_STRING, aggregatedAttributeSource, true );
				break;
			}
			default: {
				throw new AssertionFailure(
						String.format( "Unknown type of identifier: [%s]", identifierSource.getNature() )
				);
			}
		}
	}

	private void indexAttributeSources(
			AttributeIndexingTarget attributeIndexingTarget,
			String pathBase,
			AttributeSource attributeSource,
			boolean isInIdentifier) {
		final AttributeSourceKey key = new AttributeSourceKey(
				attributeIndexingTarget.getAttributeSourceKeyBase(),
				pathBase,
				attributeSource.getName()
		);
		attributeSourcesByKey.put( key, attributeSource );
		log.debugf( "Indexing attribute source [%s]", key );

		if ( attributeSource.isSingular() ) {
			attributeIndexingTarget.indexSingularAttributeSource( pathBase, (SingularAttributeSource) attributeSource, isInIdentifier );
		}
		else {
			attributeIndexingTarget.indexPluralAttributeSource( pathBase, (PluralAttributeSource) attributeSource );
		}

		if ( attributeSource instanceof ComponentAttributeSource ) {
			for ( AttributeSource subAttributeSource : ( (ComponentAttributeSource) attributeSource ).attributeSources() ) {
				indexAttributeSources(
						attributeIndexingTarget,
						key.attributePath(),
						subAttributeSource,
						isInIdentifier
				);
			}
		}
	}

	private void indexAttributes(EntitySourceIndex entitySourceIndex) {
		final String emptyString = "";
		for ( final AttributeSource attributeSource : entitySourceIndex.entitySource.attributeSources() ) {
			indexAttributeSources(entitySourceIndex, emptyString, attributeSource, false );
		}
	}










	public void resolveAssociationSources(EntitySource source, BinderLocalBindingContext context) {
		final EntityBinding binding = context.locateBinding( source );
		entitySourceIndexByEntityName.get( binding.getEntityName() ).resolveAttributeSources( context );
	}

	public Map<AttributeSourceKey, SingularAttributeSource> getSingularAttributeSources(
			String entityName,
			boolean isMappedBy,
			SingularAttributeSource.Nature nature) {
		final EntitySourceIndex entitySourceIndex = entitySourceIndexByEntityName.get( entityName );
		return entitySourceIndex.getSingularAttributeSources( isMappedBy, nature );
	}

	public Map<AttributeSourceKey, PluralAttributeSource> getPluralAttributeSources(
			String entityName,
			boolean isInverse) {
		final EntitySourceIndex entitySourceIndex = entitySourceIndexByEntityName.get( entityName );
		return entitySourceIndex.getPluralAttributeSources( isInverse );
	}

	public AttributeSource attributeSource(final String entityName, final String attributePath) {
		return attributeSourcesByKey.get( new AttributeSourceKey( entityName, attributePath ) );
	}

	public AttributeSource attributeSource(EntityBinding entityBinding, AttributeBinding attributeBinding) {
		return attributeSourcesByKey.get(
				new AttributeSourceKey(
						entityBinding.getEntityName(),
						attributeBinding.getAttributePath()
				)
		);
	}

	public AttributeSource locateAttributeSourceOwnedBy(final String entityName, final String attributePath) {
		AttributeSourceKey ownerKey = new AttributeSourceKey( entityName, attributePath );
		AttributeSourceKey mappedByKey = mappedByAttributeKeysByOwnerAttributeKeys.get( ownerKey );
		return mappedByKey == null ? null : attributeSourcesByKey.get( mappedByKey );
	}

	public EntitySource entitySource(final String entityName) {
		return entitySourceIndexByEntityName.get( entityName ).entitySource;
	}

	private EntitySourceIndex entitySourceIndex(String entityName) {
		return entitySourceIndexByEntityName.get( entityName );
	}

	void addMappedByAssociationByOwnerAssociation(AttributeSourceKey ownerKey, AttributeSourceKey ownedKey) {
		mappedByAttributeKeysByOwnerAttributeKeys.put(
				ownerKey,
				ownedKey
		);

	}


	/**
	 * Helper class for indexing attribute look ups.
	 */
	public static class AttributeSourceKey {
		private final String entityName;
		private final String containerPath;
		private final String attributeName;

		private AttributeSourceKey(final String entityName, final String containerPath, final String attributeName) {
			this.entityName = entityName;
			this.containerPath = containerPath;
			this.attributeName = attributeName;
		}

		private AttributeSourceKey(final String entityName, final String attributePath) {
			this.entityName = entityName;
			int indexLastDot = attributePath.lastIndexOf( '.' );
			if ( indexLastDot == -1 ) {
				this.containerPath = EMPTY_STRING;
				this.attributeName = attributePath;
			}
			else {
				this.containerPath = attributePath.substring( 0, indexLastDot );
				this.attributeName = attributePath.substring( indexLastDot + 1 );
			}
		}

		public String entityName() {
			return entityName;
		}

		public String containerPath() {
			return containerPath;
		}

		public String attributeName() {
			return attributeName;
		}

		public String attributePath() {
			return StringHelper.isEmpty( containerPath )
					? attributeName
					: containerPath + '.' + attributeName;
		}

		public String getAttributePathQualifiedByEntityName() {
			return entityName + '.' + attributePath();
		}

		@Override
		public String toString() {
			return getAttributePathQualifiedByEntityName();
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final AttributeSourceKey that = (AttributeSourceKey) o;
			return attributeName.equals( that.attributeName )
					&& containerPath.equals( that.containerPath )
					&& entityName.equals( that.entityName );
		}

		@Override
		public int hashCode() {
			int result = entityName.hashCode();
			result = 31 * result + containerPath.hashCode();
			result = 31 * result + attributeName.hashCode();
			return result;
		}
	}

	private static interface AttributeIndexingTarget {
		public String getAttributeSourceKeyBase();

		public void indexSingularAttributeSource(
				String pathBase,
				SingularAttributeSource attributeSource,
				boolean isInIdentifier);

		public void indexPluralAttributeSource(String pathBase, PluralAttributeSource attributeSource);
	}

	private static abstract class AbstractAttributeIndexingTarget implements AttributeIndexingTarget {
		private final Map<AttributeSourceKey, SingularAttributeSource> unresolvedSingularAttributeSourcesByKey
				= new HashMap<AttributeSourceKey, SingularAttributeSource>();

		private final Map<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>> identifierAttributeSourcesByNature
				= new EnumMap<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>>( SingularAttributeSource.Nature.class );
		private final Map<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>> nonMappedBySingularAttributeSourcesByNature
				= new EnumMap<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>>( SingularAttributeSource.Nature.class );
		private final Map<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>> mappedBySingularAttributeSourcesByNature
				= new EnumMap<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>>( SingularAttributeSource.Nature.class );

		// TODO: the following should not need to be LinkedHashMap, but it appears that some unit tests
		//       depend on the ordering
		// TODO: rework nonInversePluralAttributeSourcesByKey and inversePluralAttributeSourcesByKey
		private final Map<AttributeSourceKey, PluralAttributeSource> nonInversePluralAttributeSourcesByKey =
				new LinkedHashMap<AttributeSourceKey, PluralAttributeSource>();
		private final Map<AttributeSourceKey, PluralAttributeSource> inversePluralAttributeSourcesByKey =
				new LinkedHashMap<AttributeSourceKey, PluralAttributeSource>();

		protected AttributeSourceKey makeKey(String pathBase, AttributeSource attributeSource) {
			return new AttributeSourceKey( getAttributeSourceKeyBase(), pathBase, attributeSource.getName() );
		}

		@Override
		public void indexSingularAttributeSource(
				String pathBase,
				SingularAttributeSource attributeSource,
				boolean isInIdentifier) {
			final AttributeSourceKey attributeSourceKey = makeKey( pathBase, attributeSource );
			if ( attributeSource.getNature() == null ) {
				unresolvedSingularAttributeSourcesByKey.put( attributeSourceKey, attributeSource );
				return;
			}

			final Map<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>> map;
			if ( isInIdentifier ) {
				map = identifierAttributeSourcesByNature;
			}
			else if ( ToOneAttributeSource.class.isInstance( attributeSource ) &&
					ToOneAttributeSource.class.cast( attributeSource ).isMappedBy() ) {
				map = mappedBySingularAttributeSourcesByNature;
			}
			else {
				map = nonMappedBySingularAttributeSourcesByNature;
			}

			indexSingularAttributeSource( attributeSourceKey, attributeSource, map );
		}

		private static void indexSingularAttributeSource(
				AttributeSourceKey attributeSourceKey,
				SingularAttributeSource attributeSource,
				Map<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>> map) {
			final Map<AttributeSourceKey, SingularAttributeSource> singularAttributeSources;
			if ( map.containsKey( attributeSource.getNature() ) ) {
				singularAttributeSources = map.get( attributeSource.getNature() );
			}
			else {
				singularAttributeSources = new LinkedHashMap<AttributeSourceKey,SingularAttributeSource>();
				map.put( attributeSource.getNature(), singularAttributeSources );
			}
			if ( singularAttributeSources.put( attributeSourceKey, attributeSource ) != null ) {
				throw new AssertionFailure(
						String.format( "Attempt to reindex attribute source for: [%s]",  attributeSourceKey )
				);
			}
		}


		@Override
		public void indexPluralAttributeSource(
				String pathBase,
				PluralAttributeSource attributeSource) {
			final AttributeSourceKey key = makeKey( pathBase, attributeSource );
			final Map<AttributeSourceKey,PluralAttributeSource> map = attributeSource.isInverse()
					? inversePluralAttributeSourcesByKey
					: nonInversePluralAttributeSourcesByKey;
			if ( map.put( key, attributeSource ) != null ) {
				throw new AssertionFailure(
						String.format( "Attempt to reindex attribute source for: [%s]", key )
				);
			}
		}


		public Map<AttributeSourceKey, SingularAttributeSource> getSingularAttributeSources(
				boolean isMappedBy,
				SingularAttributeSource.Nature nature) {
			final Map<AttributeSourceKey, SingularAttributeSource> entries;
			if ( isMappedBy && mappedBySingularAttributeSourcesByNature.containsKey( nature ) ) {
				entries = Collections.unmodifiableMap( mappedBySingularAttributeSourcesByNature.get( nature ) );
			}
			else if ( !isMappedBy && nonMappedBySingularAttributeSourcesByNature.containsKey( nature ) ) {
				entries = Collections.unmodifiableMap( nonMappedBySingularAttributeSourcesByNature.get( nature ) );
			}
			else {
				entries = Collections.emptyMap();
			}
			return entries;
		}

		public Map<AttributeSourceKey, PluralAttributeSource> getPluralAttributeSources(boolean isInverse) {
			final Map<AttributeSourceKey,PluralAttributeSource> map = isInverse
					? inversePluralAttributeSourcesByKey
					: nonInversePluralAttributeSourcesByKey;
			return Collections.unmodifiableMap( map );
		}


		public void resolveAttributeSources(BinderLocalBindingContext context) {
			final AttributeSourceResolutionContext sourceResolutionContext = makeAttributeSourceResolutionContext( context );

			// Resolve plural attributes.
			for ( PluralAttributeSource pluralAttributeSource : inversePluralAttributeSourcesByKey.values() ) {
				if ( pluralAttributeSource.getMappedBy() != null ) {
					// This plural attribute is mappedBy the opposite side of the association,
					// so it needs to be resolved.
					pluralAttributeSource.resolvePluralAttributeElementSource( sourceResolutionContext );
				}
				if ( IndexedPluralAttributeSource.class.isInstance( pluralAttributeSource ) ) {
					IndexedPluralAttributeSource indexedPluralAttributeSource =
							(IndexedPluralAttributeSource) pluralAttributeSource;
					indexedPluralAttributeSource.resolvePluralAttributeIndexSource(  sourceResolutionContext );
				}
			}

			for ( PluralAttributeSource pluralAttributeSource : nonInversePluralAttributeSourcesByKey.values() ) {
				if ( IndexedPluralAttributeSource.class.isInstance( pluralAttributeSource ) ) {
					IndexedPluralAttributeSource indexedPluralAttributeSource =
							(IndexedPluralAttributeSource) pluralAttributeSource;
					indexedPluralAttributeSource.resolvePluralAttributeIndexSource( sourceResolutionContext );
				}
			}

			// cycle through unresolved SingularAttributeSource.
			// TODO: rework so approach is similar to one-to-many/many-to-many resolution.
			for ( Iterator<Map.Entry<AttributeSourceKey,SingularAttributeSource>> it = unresolvedSingularAttributeSourcesByKey.entrySet().iterator(); it.hasNext(); ) {
				final Map.Entry<AttributeSourceKey,SingularAttributeSource> entry = it.next();
				final AttributeSourceKey attributeSourceKey = entry.getKey();
				final SingularAttributeSource attributeSource = entry.getValue();
				if ( !ToOneAttributeSource.class.isInstance( attributeSource ) ) {
					throw new AssertionFailure(
							String.format( "Only a ToOneAttributeSource is expected to have a null nature; attribute: %s ", attributeSourceKey )
					);
				}
				ToOneAttributeSource toOneAttributeSource = (ToOneAttributeSource) attributeSource;
				toOneAttributeSource.resolveToOneAttributeSource( sourceResolutionContext );
				if ( toOneAttributeSource.getNature() == null ) {
					throw new AssertionFailure(
							String.format( "Null nature should have been resolved: %s ", attributeSourceKey )
					);
				}
				indexSingularAttributeSource(
						attributeSourceKey,
						attributeSource,
						toOneAttributeSource.isMappedBy()
								? mappedBySingularAttributeSourcesByNature
								: nonMappedBySingularAttributeSourcesByNature
				);
			}
		}

		protected abstract AttributeSourceResolutionContext makeAttributeSourceResolutionContext(BinderLocalBindingContext context);
	}

	private static class HierarchyInfo extends AbstractAttributeIndexingTarget {
		private final String hierarchyKey;
		private final EntityHierarchySource hierarchySource;

		private HierarchyInfo(String hierarchyKey, EntityHierarchySource hierarchySource) {
			this.hierarchyKey = hierarchyKey;
			this.hierarchySource = hierarchySource;
		}

		@Override
		public String getAttributeSourceKeyBase() {
			return hierarchyKey;
		}

		@Override
		public void indexPluralAttributeSource(String pathBase, PluralAttributeSource attributeSource) {
			throw new AssertionFailure(
					String.format(
							"Identifiers should not contain plural attributes: [%s]",
							makeKey( pathBase, attributeSource )
					)
			);
		}

		@Override
		public Map<AttributeSourceKey, PluralAttributeSource> getPluralAttributeSources(boolean isInverse) {
			return Collections.emptyMap();
		}

		@Override
		protected AttributeSourceResolutionContext makeAttributeSourceResolutionContext(final BinderLocalBindingContext context) {
			return new AttributeSourceResolutionContext() {
				@Override
				public IdentifierSource resolveIdentifierSource(String entityName) {
					return hierarchySource.getIdentifierSource();
				}

				@Override
				public AttributeSource resolveAttributeSource(String entityName, String attributeName) {
					throw new UnsupportedOperationException( "Whaaa!?!" );
				}

				@Override
				public List<Column> resolveIdentifierColumns() {
					return context.locateBinding( hierarchySource ).getRootEntityBinding().getPrimaryTable().getPrimaryKey().getColumns();
				}
			};
		}
	}

	private static class EntitySourceIndex extends AbstractAttributeIndexingTarget {
		private final SourceIndex sourceIndex;
		private final HierarchyInfo hierarchyInfo;
		private final EntitySource entitySource;

		private EntitySourceIndex(
				final SourceIndex sourceIndex,
				final HierarchyInfo hierarchyInfo,
				final EntitySource entitySource) {
			this.sourceIndex = sourceIndex;
			this.hierarchyInfo = hierarchyInfo;
			this.entitySource = entitySource;
		}

		@Override
		public String getAttributeSourceKeyBase() {
			return entitySource.getEntityName();
		}

		@Override
		public Map<AttributeSourceKey, SingularAttributeSource> getSingularAttributeSources(
				boolean isMappedBy,
				SingularAttributeSource.Nature nature) {
			Map<AttributeSourceKey, SingularAttributeSource> values = hierarchyInfo.getSingularAttributeSources( isMappedBy, nature );
			if ( values == null || values.isEmpty() ) {
				values = super.getSingularAttributeSources( isMappedBy, nature );
			}

			return values;
		}

		@Override
		protected AttributeSourceResolutionContext makeAttributeSourceResolutionContext(
				final BinderLocalBindingContext context) {
			return new AttributeSourceResolutionContext() {
				@Override
				public IdentifierSource resolveIdentifierSource(String entityName) {
					return entitySource.getHierarchy().getIdentifierSource();
				}

				@Override
				public AttributeSource resolveAttributeSource(String entityName, String attributeName) {
					return sourceIndex.attributeSource( entityName, attributeName );
				}

				@Override
				public List<Column> resolveIdentifierColumns() {
					return context.locateBinding( entitySource ).getPrimaryTable().getPrimaryKey().getColumns();
				}
			};
		}
	}


	private static class DependencyTreeNode {
		private String dependedOnHierarchyKey;
		private List<String> dependantHierarchyKeys;
	}
}
