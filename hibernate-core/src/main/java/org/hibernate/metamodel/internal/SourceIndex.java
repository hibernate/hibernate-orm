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

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.source.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceResolutionContext;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.IdentifierSource;
import org.hibernate.metamodel.spi.source.IndexedPluralAttributeSource;
import org.hibernate.metamodel.spi.source.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.metamodel.spi.source.SimpleIdentifierSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;

/**
 * @author Gail Badner
 */
public class SourceIndex {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SourceIndex.class.getName()
	);
	private static final String EMPTY_STRING = "";

	private final Map<String, EntitySourceIndex> entitySourceIndexByEntityName = new HashMap<String, EntitySourceIndex>();
	private final Map<AttributeSourceKey, AttributeSource> attributeSourcesByKey = new HashMap<AttributeSourceKey, AttributeSource>();
	private final Map<AttributeSourceKey, AttributeSourceKey> mappedByAttributeKeysByOwnerAttributeKeys =
			new HashMap<AttributeSourceKey, AttributeSourceKey>();

	public void indexEntitySource(final RootEntitySource rootEntitySource, final EntitySource entitySource) {
		String entityName = entitySource.getEntityName();
		EntitySourceIndex entitySourceIndex = new EntitySourceIndex( this, rootEntitySource, entitySource );
		entitySourceIndexByEntityName.put( entityName, entitySourceIndex );
		log.debugf( "Mapped entity source \"%s\"", entityName );
		indexAttributes( entitySourceIndex );
	}

	public void resolveAssociationSources(EntityHierarchyHelper.LocalBindingContextExecutionContext bindingContextContext) {
		entitySourceIndexByEntityName
				.get( bindingContextContext.getEntityBinding().getEntityName() )
				.resolveAttributeSources( bindingContextContext );
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

	private void indexAttributes(EntitySourceIndex entitySourceIndex) {
		final String emptyString = "";
		if ( entitySourceIndex.entitySource instanceof RootEntitySource ) {
			indexIdentifierAttributeSources( entitySourceIndex );
		}
		for ( final AttributeSource attributeSource : entitySourceIndex.entitySource.attributeSources() ) {
			indexAttributeSources(entitySourceIndex, emptyString, attributeSource, false );
		}
	}

	private void indexIdentifierAttributeSources(EntitySourceIndex entitySourceIndex)  {
		RootEntitySource rootEntitySource = (RootEntitySource) entitySourceIndex.entitySource;
		IdentifierSource identifierSource = rootEntitySource.getIdentifierSource();
		switch ( identifierSource.getNature() ) {
			case SIMPLE:
				final AttributeSource identifierAttributeSource =
						( (SimpleIdentifierSource) identifierSource ).getIdentifierAttributeSource();
				indexAttributeSources( entitySourceIndex, EMPTY_STRING, identifierAttributeSource, true );
				break;
			case NON_AGGREGATED_COMPOSITE:
				final List<SingularAttributeSource> nonAggregatedAttributeSources =
						( (NonAggregatedCompositeIdentifierSource) identifierSource ).getAttributeSourcesMakingUpIdentifier();
				for ( SingularAttributeSource nonAggregatedAttributeSource : nonAggregatedAttributeSources ) {
					indexAttributeSources( entitySourceIndex, EMPTY_STRING, nonAggregatedAttributeSource, true );
				}
				break;
			case AGGREGATED_COMPOSITE:
				final ComponentAttributeSource aggregatedAttributeSource =
						( (AggregatedCompositeIdentifierSource) identifierSource ).getIdentifierAttributeSource();
				indexAttributeSources( entitySourceIndex, EMPTY_STRING, aggregatedAttributeSource, true );
				break;
			default:
				throw new AssertionFailure(
						String.format( "Unknown type of identifier: [%s]", identifierSource.getNature() )
				);
		}
	}

	private void indexAttributeSources(
			EntitySourceIndex entitySourceIndex,
			String pathBase,
			AttributeSource attributeSource,
			boolean isInIdentifier) {
		AttributeSourceKey key = new AttributeSourceKey( entitySourceIndex.entitySource.getEntityName(), pathBase, attributeSource.getName() );
		attributeSourcesByKey.put( key, attributeSource );
		log.debugf(
				"Mapped attribute source \"%s\"", key
		);
		if ( attributeSource.isSingular() ) {
			entitySourceIndex.indexSingularAttributeSource( pathBase, (SingularAttributeSource) attributeSource, isInIdentifier );
		}
		else {
			entitySourceIndex.indexPluralAttributeSource( pathBase, (PluralAttributeSource) attributeSource );
		}
		if ( attributeSource instanceof ComponentAttributeSource ) {
			for ( AttributeSource subAttributeSource : ( (ComponentAttributeSource) attributeSource ).attributeSources() ) {
				indexAttributeSources(
						entitySourceIndex,
						key.attributePath(),
						subAttributeSource,
						isInIdentifier
				);
			}
		}
	}

	void addMappedByAssociationByOwnerAssociation(AttributeSourceKey ownerKey, AttributeSourceKey ownedKey) {
		mappedByAttributeKeysByOwnerAttributeKeys.put(
				ownerKey,
				ownedKey
		);

	}

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
			return StringHelper.isEmpty( containerPath ) ?
					attributeName :
					containerPath + '.' + attributeName;
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

			AttributeSourceKey that = (AttributeSourceKey) o;

			if ( !attributeName.equals( that.attributeName ) ) {
				return false;
			}
			if ( !containerPath.equals( that.containerPath ) ) {
				return false;
			}
			if ( !entityName.equals( that.entityName ) ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = entityName.hashCode();
			result = 31 * result + containerPath.hashCode();
			result = 31 * result + attributeName.hashCode();
			return result;
		}
	}

	private static class EntitySourceIndex {
		private final SourceIndex sourceIndex;
		private final RootEntitySource rootEntitySource;
		private final EntitySource entitySource;
		private final Map<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>>
				identifierAttributeSourcesByNature = new EnumMap<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>>( SingularAttributeSource.Nature.class );
		private final Map<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>>
				nonMappedBySingularAttributeSourcesByNature = new EnumMap<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>>( SingularAttributeSource.Nature.class );
		private final Map<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>>
				mappedBySingularAttributeSourcesByNature = new EnumMap<SingularAttributeSource.Nature, Map<AttributeSourceKey, SingularAttributeSource>>( SingularAttributeSource.Nature.class );
		private final Map<AttributeSourceKey, SingularAttributeSource> unresolvedSingularAttributeSourcesByKey =
				new HashMap<AttributeSourceKey, SingularAttributeSource>();

		// TODO: the following should not need to be LinkedHashMap, but it appears that some unit tests
		//       depend on the ordering
		// TODO: rework nonInversePluralAttributeSourcesByKey and inversePluralAttributeSourcesByKey
		private final Map<AttributeSourceKey, PluralAttributeSource> nonInversePluralAttributeSourcesByKey =
				new LinkedHashMap<AttributeSourceKey, PluralAttributeSource>();
		private final Map<AttributeSourceKey, PluralAttributeSource> inversePluralAttributeSourcesByKey =
				new LinkedHashMap<AttributeSourceKey, PluralAttributeSource>();

		private EntitySourceIndex(
				final SourceIndex sourceIndex,
				final RootEntitySource rootEntitySource,
				final EntitySource entitySource) {
			this.sourceIndex = sourceIndex;
			this.rootEntitySource = rootEntitySource;
			this.entitySource = entitySource;
		}

		private void indexSingularAttributeSource(
				String pathBase,
				SingularAttributeSource attributeSource,
				boolean isInIdentifier) {
			final AttributeSourceKey attributeSourceKey =
					new AttributeSourceKey( entitySource.getEntityName(), pathBase, attributeSource.getName() );
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
			indexSingularAttributeSource(
					attributeSourceKey,
					attributeSource,
					map
			);
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

		private Map<AttributeSourceKey, SingularAttributeSource> getSingularAttributeSources(
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

		private void indexPluralAttributeSource(
				String pathBase,
				PluralAttributeSource attributeSource) {
			AttributeSourceKey key =
					new AttributeSourceKey( entitySource.getEntityName(), pathBase, attributeSource.getName() );
			final Map<AttributeSourceKey,PluralAttributeSource> map =
					attributeSource.isInverse() ?
							inversePluralAttributeSourcesByKey :
							nonInversePluralAttributeSourcesByKey;
			if ( map.put( key, attributeSource ) != null ) {
				throw new AssertionFailure(
						String.format(
								"Attempt to reindex attribute source for: [%s]",
								new AttributeSourceKey( entitySource.getEntityName(), pathBase, attributeSource.getName() )
						)
				);
			}
		}

		private Map<AttributeSourceKey, PluralAttributeSource> getPluralAttributeSources(
				boolean isInverse) {
			final Map<AttributeSourceKey,PluralAttributeSource> map =
					isInverse ?
							inversePluralAttributeSourcesByKey :
							nonInversePluralAttributeSourcesByKey;
			return Collections.unmodifiableMap( map );
		}

		private void resolveAttributeSources(EntityHierarchyHelper.LocalBindingContextExecutionContext bindingContextContext) {
			final AttributeSourceResolutionContext sourceResolutionContext =
					new AttributeSourceResolutionContextImpl( bindingContextContext );
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
						toOneAttributeSource.isMappedBy() ?
								mappedBySingularAttributeSourcesByNature :
								nonMappedBySingularAttributeSourcesByNature
				);
			}
		}

		class AttributeSourceResolutionContextImpl implements AttributeSourceResolutionContext {
			private final EntityHierarchyHelper.LocalBindingContextExecutionContext bindingContextContext;

			public AttributeSourceResolutionContextImpl(
					EntityHierarchyHelper.LocalBindingContextExecutionContext bindingContextContext) {
				this.bindingContextContext = bindingContextContext;
			}

			@Override
			public IdentifierSource resolveIdentifierSource(String entityName) {
				return bindingContextContext.getRootEntitySource().getIdentifierSource();
			}

			@Override
			public AttributeSource resolveAttributeSource(String entityName, String attributeName) {
				return sourceIndex.attributeSource( entityName, attributeName );
			}

			@Override
			public List<Column> resolveIdentifierColumns() {
				return bindingContextContext.getEntityBinding().getPrimaryTable().getPrimaryKey().getColumns();
			}
		}
	}
}
