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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.RootEntitySourceImpl;
import org.hibernate.metamodel.source.spi.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.AttributeSourceResolutionContext;
import org.hibernate.metamodel.source.spi.ColumnSource;
import org.hibernate.metamodel.source.spi.EmbeddedAttributeSource;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.IdentifiableTypeSource;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.IndexedPluralAttributeSource;
import org.hibernate.metamodel.source.spi.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.PluralAttributeIndexSourceResolver;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.RelationalValueSourceContainer;
import org.hibernate.metamodel.source.spi.SimpleIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.metamodel.spi.SingularAttributeNature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.jboss.logging.Logger;

/**
 * Builds indexes (cross references) of the various parts of the source model
 * for Binder access.
 * <p/>
 * The main contracts of SourceIndex are:<ul>
 *     <li>
 *         Indexing the entities in each hierarchy : {@link #indexHierarchy}
 *     </li>
 *     <li>
 *         Accessing all hierarchies: {@link #getAllHierarchySources()}
 *     </li>
 *     <li>
 *         Accessing the attributes in various ways:<ul>
 *             <li>{@link #attributeSource(AttributeRole)}</li>
 *             <li>{@link #attributeSource(EntityBinding, AttributeBinding)}</li>
 *             <li>{@link #attributeSource(String, String)}</li>
 *             <li>{@link #getSingularAttributeSources}</li>
 *             <li>{@link #getPluralAttributeSources}</li>
 *         </ul>
 *     </li>
 *     <li>
 *         This notion of "resolving associations" at the source level.  See
 *         {@link #resolveAssociationSources}.  I really want to look at this
 *         and the point it is trying to solve.
 *     </li>
 * </ul>

 * @author Gail Badner
 * @author Steve Ebersole
 */
public class SourceIndex {
	private static final Logger log = CoreLogging.logger( SourceIndex.class );

	private final BindingContext context;

	private final Map<String, EntityHierarchySource> entityHierarchiesByRootEntityName;
	private final Map<String, EntitySourceIndex> entitySourceIndexByEntityName;

	private final Map<AttributeRole, AttributeSource> attributeSourcesByKey;
	private final Map<AttributeRole, AttributeRole> mappedByAttributeKeysByOwnerAttributeKeys;

	public SourceIndex(BindingContext context) {
		this.context = context;
		this.entityHierarchiesByRootEntityName = new LinkedHashMap<String, EntityHierarchySource>();
		this.entitySourceIndexByEntityName = new HashMap<String, EntitySourceIndex>();
		this.attributeSourcesByKey = new HashMap<AttributeRole, AttributeSource>();
		this.mappedByAttributeKeysByOwnerAttributeKeys = new HashMap<AttributeRole, AttributeRole>();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Accessing hierarchies

	/**
	 * Access to source information about an entity hierarchy by its root entity
	 * name
	 *
	 * @param rootEntityName The root entity name
	 *
	 * @return The hierarchy source information.
	 */
	public EntityHierarchySource getHierarchySourceByRootEntityName(String rootEntityName) {
		return entityHierarchiesByRootEntityName.get( rootEntityName );
	}

	/**
	 * Obtains source information about all entity hierarchies.
	 * <p/>
	 * Note that a Collection is returned because no order is defined.
	 *
	 * @return Source information about all entity hierarchies.
	 */
	public Collection<EntityHierarchySource> getAllHierarchySources() {
		return entityHierarchiesByRootEntityName.values();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Accessing attributes

	public Map<AttributeRole, SingularAttributeSource> getSingularAttributeSources(
			String entityName,
			boolean isMappedBy,
			SingularAttributeNature singularAttributeNature) {
		final EntitySourceIndex entitySourceIndex = entitySourceIndexByEntityName.get( entityName );
		return entitySourceIndex.getSingularAttributeSources( isMappedBy, singularAttributeNature );
	}

	public Map<AttributeRole, PluralAttributeSource> getPluralAttributeSources(
			String entityName,
			boolean isInverse) {
		final EntitySourceIndex entitySourceIndex = entitySourceIndexByEntityName.get( entityName );
		return entitySourceIndex.getPluralAttributeSources( isInverse );
	}

	public AttributeSource attributeSource(final AttributeRole attributeRole) {
		return attributeSourcesByKey.get( attributeRole );
	}

	public AttributeSource attributeSource(String entityName, String attributePath) {
		final AttributeRole base = new AttributeRole( entityName );

		AttributeRole role;
		if ( attributePath.contains( "." ) ) {
			role = base;
			for ( String part : attributePath.split( "\\." ) ) {
				role = role.append( part );
			}
		}
		else {
			role = base.append( attributePath );
		}
		return attributeSourcesByKey.get( role );
	}

	public AttributeSource attributeSource(EntityBinding entityBinding, AttributeBinding attributeBinding) {
		return attributeSourcesByKey.get( attributeBinding.getAttributeRole() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Resolving associations

	public void resolveAssociationSources(EntitySource source, BinderLocalBindingContext context) {
		final EntityBinding binding = context.locateBinding( source );
		entitySourceIndexByEntityName.get( binding.getEntityName() ).resolveAttributeSources( context );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Indexing

	public void indexHierarchy(EntityHierarchySource hierarchy) {
		final String hierarchyKey = hierarchy.getRoot().getEntityName();

		entityHierarchiesByRootEntityName.put( hierarchyKey, hierarchy );

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

		// todo : consider resolving JavaTypeDescriptor or even o.h.metamodel.spi.domain model as as we index the sources.\
		//
		// This is especially useful for associations and components (mainly in HBM
		// uses, as in Annotation uses the JavaTypeDescriptor is already known).
		//
		// See org.hibernate.metamodel.source.spi.JavaTypeDescriptorResolvable

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
				indexAttributeSources( hierarchyInfo, identifierAttributeSource, true );
				break;
			}
			case NON_AGGREGATED_COMPOSITE: {
				final List<SingularAttributeSource> nonAggregatedAttributeSources =
						( (NonAggregatedCompositeIdentifierSource) identifierSource ).getAttributeSourcesMakingUpIdentifier();
				for ( SingularAttributeSource nonAggregatedAttributeSource : nonAggregatedAttributeSources ) {
					indexAttributeSources( hierarchyInfo, nonAggregatedAttributeSource, true );
				}
				break;
			}
			case AGGREGATED_COMPOSITE: {
				final EmbeddedAttributeSource aggregatedAttributeSource =
						( (AggregatedCompositeIdentifierSource) identifierSource ).getIdentifierAttributeSource();
				indexAttributeSources( hierarchyInfo, aggregatedAttributeSource, true );
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
			AttributeSource attributeSource,
			boolean isInIdentifier) {
		log.debugf( "Indexing attribute source [%s]", attributeSource.getAttributeRole() );
		attributeSourcesByKey.put( attributeSource.getAttributeRole(), attributeSource );

		if ( attributeSource.isSingular() ) {
			attributeIndexingTarget.indexSingularAttributeSource( (SingularAttributeSource) attributeSource );
		}
		else {
			attributeIndexingTarget.indexPluralAttributeSource( (PluralAttributeSource) attributeSource );
		}

		if ( attributeSource instanceof EmbeddedAttributeSource ) {
			for ( AttributeSource subAttributeSource : ( (EmbeddedAttributeSource) attributeSource ).getEmbeddableSource().attributeSources() ) {
				indexAttributeSources(
						attributeIndexingTarget,
						subAttributeSource,
						isInIdentifier
				);
			}
		}
	}

	private void indexAttributes(EntitySourceIndex entitySourceIndex) {
		for ( final AttributeSource attributeSource : entitySourceIndex.entitySource.attributeSources() ) {
			indexAttributeSources( entitySourceIndex, attributeSource, false );
		}
	}


	/**
	 * Interface to target where attributes should be indexed.  Mainly this is
	 * used to collect id attributes into one pool, and non-id attributes into another.
	 */
	private static interface AttributeIndexingTarget {
		public void indexSingularAttributeSource(SingularAttributeSource attributeSource);
		public void indexPluralAttributeSource(PluralAttributeSource attributeSource);
	}

	private static class HierarchyInfo implements AttributeIndexingTarget {
		private final String hierarchyKey;
		private final EntityHierarchySource hierarchySource;

		private final Map<SingularAttributeNature, Map<AttributeRole, SingularAttributeSource>> identifierAttributeSourcesByNature
				= new EnumMap<SingularAttributeNature, Map<AttributeRole, SingularAttributeSource>>( SingularAttributeNature.class );

		private HierarchyInfo(String hierarchyKey, EntityHierarchySource hierarchySource) {
			this.hierarchyKey = hierarchyKey;
			this.hierarchySource = hierarchySource;
		}


		public Map<AttributeRole, SingularAttributeSource> getSingularAttributeSources(
				SingularAttributeNature singularAttributeNature) {
			return identifierAttributeSourcesByNature.get( singularAttributeNature );
		}

		@Override
		public void indexSingularAttributeSource(SingularAttributeSource attributeSource) {
			if ( attributeSource.getSingularAttributeNature() == null ) {
				( (ToOneAttributeSource) attributeSource ).resolveToOneAttributeSourceNatureAsPartOfIdentifier();
				if ( attributeSource.getSingularAttributeNature() == null ) {
					throw new IllegalStateException(
							"Could not determine to-one association nature for [" +
									attributeSource.getAttributeRole().getFullPath() + "]"
					);
				}
			}

			if ( ToOneAttributeSource.class.isInstance( attributeSource )
					&& ToOneAttributeSource.class.cast( attributeSource ).isMappedBy() ) {
				throw new IllegalStateException(
						"Association attribute (to-one) that is part of an identifier " +
								"cannot be mapped as mappedBy : " +
								attributeSource.getAttributeRole().getFullPath()
				);
			}

			Map<AttributeRole, SingularAttributeSource> natureMap =
					identifierAttributeSourcesByNature.get( attributeSource.getSingularAttributeNature() );
			if ( natureMap == null ) {
				natureMap = new HashMap<AttributeRole, SingularAttributeSource>();
				identifierAttributeSourcesByNature.put( attributeSource.getSingularAttributeNature(), natureMap );
			}
			natureMap.put( attributeSource.getAttributeRole(), attributeSource );
		}

		@Override
		public void indexPluralAttributeSource(PluralAttributeSource attributeSource) {
			throw new AssertionFailure(
					String.format(
							"Identifiers should not contain plural attributes: [%s]",
							attributeSource.getAttributeRole().getFullPath()
					)
			);
		}
	}
	
	public EntitySource entitySource(String entityName) {
		return entitySourceIndexByEntityName.get( entityName ).entitySource;
	}

	private static class EntitySourceIndex implements AttributeIndexingTarget {
		private final SourceIndex sourceIndex;
		private final HierarchyInfo hierarchyInfo;
		private final EntitySource entitySource;

		private final Map<AttributeRole, SingularAttributeSource> unresolvedSingularAttributeSourcesByKey
				= new HashMap<AttributeRole, SingularAttributeSource>();

		private final EnumMap<SingularAttributeNature, Map<AttributeRole, SingularAttributeSource>> nonMappedBySingularAttributeSourcesByNature
				= new EnumMap<SingularAttributeNature, Map<AttributeRole, SingularAttributeSource>>( SingularAttributeNature.class );
		private final EnumMap<SingularAttributeNature, Map<AttributeRole, SingularAttributeSource>> mappedBySingularAttributeSourcesByNature
				= new EnumMap<SingularAttributeNature, Map<AttributeRole, SingularAttributeSource>>( SingularAttributeNature.class );

		// TODO: the following should not need to be LinkedHashMap, but it appears that some unit tests
		//       depend on the ordering
		// TODO: rework nonInversePluralAttributeSourcesByKey and inversePluralAttributeSourcesByKey
		private final Map<AttributeRole, PluralAttributeSource> nonInversePluralAttributeSourcesByKey =
				new LinkedHashMap<AttributeRole, PluralAttributeSource>();
		private final Map<AttributeRole, PluralAttributeSource> inversePluralAttributeSourcesByKey =
				new LinkedHashMap<AttributeRole, PluralAttributeSource>();

		private EntitySourceIndex(
				final SourceIndex sourceIndex,
				final HierarchyInfo hierarchyInfo,
				final EntitySource entitySource) {
			this.sourceIndex = sourceIndex;
			this.hierarchyInfo = hierarchyInfo;
			this.entitySource = entitySource;
		}

		@Override
		public void indexSingularAttributeSource(SingularAttributeSource attributeSource) {
			if ( attributeSource.getSingularAttributeNature() == null ) {
				unresolvedSingularAttributeSourcesByKey.put( attributeSource.getAttributeRole(), attributeSource );
				return;
			}

			final EnumMap<SingularAttributeNature, Map<AttributeRole, SingularAttributeSource>> map;
			if ( ToOneAttributeSource.class.isInstance( attributeSource ) &&
					ToOneAttributeSource.class.cast( attributeSource ).isMappedBy() ) {
				map = mappedBySingularAttributeSourcesByNature;
			}
			else {
				map = nonMappedBySingularAttributeSourcesByNature;
			}

			indexSingularAttributeSource( attributeSource, map );
		}

		protected static void indexSingularAttributeSource(
				SingularAttributeSource attributeSource,
				EnumMap<SingularAttributeNature, Map<AttributeRole, SingularAttributeSource>> map) {
			final Map<AttributeRole, SingularAttributeSource> singularAttributeSources;
			if ( map.containsKey( attributeSource.getSingularAttributeNature() ) ) {
				singularAttributeSources = map.get( attributeSource.getSingularAttributeNature() );
			}
			else {
				singularAttributeSources = new LinkedHashMap<AttributeRole,SingularAttributeSource>();
				map.put( attributeSource.getSingularAttributeNature(), singularAttributeSources );
			}
			indexSingularAttributeSource( attributeSource, singularAttributeSources );
		}

		protected static void indexSingularAttributeSource(
				SingularAttributeSource attributeSource,
				Map<AttributeRole, SingularAttributeSource> singularAttributeSourceMap) {
			if ( singularAttributeSourceMap.put( attributeSource.getAttributeRole(), attributeSource ) != null ) {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								"Attempt to reindex attribute source for: [%s]",
								attributeSource.getAttributeRole()
						)
				);
			}
		}


		@Override
		public void indexPluralAttributeSource(PluralAttributeSource attributeSource) {
			final Map<AttributeRole,PluralAttributeSource> map = attributeSource.isInverse()
					? inversePluralAttributeSourcesByKey
					: nonInversePluralAttributeSourcesByKey;
			if ( map.put( attributeSource.getAttributeRole(), attributeSource ) != null ) {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								"Attempt to reindex attribute source for: [%s]",
								attributeSource.getAttributeRole() )
				);
			}
		}


		public Map<AttributeRole, SingularAttributeSource> getSingularAttributeSources(
				boolean isMappedBy,
				SingularAttributeNature singularAttributeNature) {
			final Map<AttributeRole, SingularAttributeSource> entries;
			if ( isMappedBy && mappedBySingularAttributeSourcesByNature.containsKey( singularAttributeNature ) ) {
				entries = Collections.unmodifiableMap(
						mappedBySingularAttributeSourcesByNature.get(
								singularAttributeNature
						)
				);
			}
			else if ( !isMappedBy && nonMappedBySingularAttributeSourcesByNature.containsKey( singularAttributeNature ) ) {
				entries = Collections.unmodifiableMap(
						nonMappedBySingularAttributeSourcesByNature.get(
								singularAttributeNature
						)
				);
			}
			else {
				entries = Collections.emptyMap();
			}
			return entries;
		}

		public Map<AttributeRole, PluralAttributeSource> getPluralAttributeSources(boolean isInverse) {
			final Map<AttributeRole,PluralAttributeSource> map = isInverse
					? inversePluralAttributeSourcesByKey
					: nonInversePluralAttributeSourcesByKey;
			return Collections.unmodifiableMap( map );
		}


		public void resolveAttributeSources(BinderLocalBindingContext context) {
			final AttributeSourceResolutionContext sourceResolutionContext = makeAttributeSourceResolutionContext( context );

			// Resolve plural attributes.
			for ( PluralAttributeSource pluralAttributeSource : inversePluralAttributeSourcesByKey.values() ) {
				if ( pluralAttributeSource.isInverse() ) {
					// This plural attribute is mappedBy the opposite side of the association,
					// so it needs to be resolved.
					pluralAttributeSource.resolvePluralAttributeElementSource( sourceResolutionContext );
				}

				if ( IndexedPluralAttributeSource.class.isInstance( pluralAttributeSource ) ) {
					final IndexedPluralAttributeSource indexedPluralAttributeSource = (IndexedPluralAttributeSource) pluralAttributeSource;
					if ( PluralAttributeIndexSourceResolver.class.isInstance( indexedPluralAttributeSource.getIndexSource() ) ) {
						( (PluralAttributeIndexSourceResolver) indexedPluralAttributeSource.getIndexSource() ).resolvePluralAttributeIndexSource(
								sourceResolutionContext
						);
					}
				}
			}

			for ( PluralAttributeSource pluralAttributeSource : nonInversePluralAttributeSourcesByKey.values() ) {
				if ( IndexedPluralAttributeSource.class.isInstance( pluralAttributeSource ) ) {
					final IndexedPluralAttributeSource indexedPluralAttributeSource = (IndexedPluralAttributeSource) pluralAttributeSource;
					if ( PluralAttributeIndexSourceResolver.class.isInstance( indexedPluralAttributeSource.getIndexSource() ) ) {
						( (PluralAttributeIndexSourceResolver) indexedPluralAttributeSource.getIndexSource() ).resolvePluralAttributeIndexSource(
								sourceResolutionContext
						);
					}
				}
			}

			// cycle through unresolved SingularAttributeSource.
			// TODO: rework so approach is similar to one-to-many/many-to-many resolution.
			for ( final SingularAttributeSource attributeSource : unresolvedSingularAttributeSourcesByKey.values() ) {
				if ( !ToOneAttributeSource.class.isInstance( attributeSource ) ) {
					throw new AssertionFailure(
							String.format(
									Locale.ENGLISH,
									"Only a ToOneAttributeSource is expected to have a null nature; attribute: %s ",
									attributeSource.getAttributeRole()
							)
					);
				}
				ToOneAttributeSource toOneAttributeSource = (ToOneAttributeSource) attributeSource;
				toOneAttributeSource.resolveToOneAttributeSourceNature( sourceResolutionContext );
				if ( toOneAttributeSource.getSingularAttributeNature() == null ) {
					throw new AssertionFailure(
							String.format(
									Locale.ENGLISH,
									"Null nature should have been resolved: %s ",
									attributeSource.getAttributeRole()
							)
					);
				}
				indexSingularAttributeSource(
						attributeSource,
						toOneAttributeSource.isMappedBy()
								? mappedBySingularAttributeSourcesByNature
								: nonMappedBySingularAttributeSourcesByNature
				);
			}
		}

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
				
				@Override
				public String resolveAttributeName(String entityName, List<String> columnNames) {
					final EntitySource entitySource = sourceIndex.entitySource( entityName );
					for (AttributeSource attributeSource : entitySource.attributeSources()) {
						final String owningAttributeName = resolveAttributeName( attributeSource, entityName, columnNames );
						if (! StringHelper.isEmpty( owningAttributeName )) {
							return owningAttributeName;
						}
					}
					
					// TODO: yuck
					if (RootEntitySourceImpl.class.isInstance( entitySource )) {
						final RootEntitySourceImpl rootSource = (RootEntitySourceImpl) entitySource;
						for (AttributeSource attributeSource : rootSource.getIdentifierAttributes()) {
							final String owningAttributeName = resolveAttributeName( attributeSource, entityName, columnNames );
							if (! StringHelper.isEmpty( owningAttributeName )) {
								return owningAttributeName;
							}
						}
					}
					return null;
				}
				
				private String resolveAttributeName(AttributeSource attributeSource, String entityName, List<String> columnNames) {
					if (EmbeddedAttributeSource.class.isInstance( attributeSource )) {
						final EmbeddedAttributeSource embeddedSource = (EmbeddedAttributeSource) attributeSource;
						for (AttributeSource embeddedAttributeSource : embeddedSource.getEmbeddableSource().attributeSources()) {
							final String owningAttributeName = resolveAttributeName( embeddedAttributeSource, entityName, columnNames );
							if (! StringHelper.isEmpty( owningAttributeName )) {
								return embeddedSource.getName() + "." + owningAttributeName;
							}
						}
					}
					else if (SingularAttributeSource.class.isInstance( attributeSource )) {
						final SingularAttributeSource singularSource = (SingularAttributeSource) attributeSource;
						int count = 0;
						for (RelationalValueSource relationalSource : singularSource.relationalValueSources()) {
							if (ColumnSource.class.isInstance( relationalSource )) {
								final ColumnSource columnSource = (ColumnSource) relationalSource;
								if (StringHelper.containsIgnoreCase( columnNames, columnSource.getName() )) {
									count++;
								}
							}
						}
						if (count == columnNames.size()
								&& singularSource.relationalValueSources().size() == columnNames.size()) {
							return attributeSource.getName();
						}
					}
					else if (PluralAttributeSource.class.isInstance( attributeSource )) {
						final PluralAttributeSource pluralSource = (PluralAttributeSource) attributeSource;
						if (pluralSource.getElementSource() instanceof RelationalValueSourceContainer) {
							final RelationalValueSourceContainer relationalSourceContainer
									= (RelationalValueSourceContainer) pluralSource.getElementSource();
							int count = 0;
							for (RelationalValueSource relationalSource : relationalSourceContainer.relationalValueSources()) {
								if (ColumnSource.class.isInstance( relationalSource )) {
									final ColumnSource columnSource = (ColumnSource) relationalSource;
									if (StringHelper.containsIgnoreCase( columnNames, columnSource.getName() )) {
										count++;
									}
								}
							}
							if (count == columnNames.size()
									&& pluralSource.getPluralAttribute().getJoinColumnValues().size() == columnNames.size()) {
								return attributeSource.getName();
							}
						}
					}
					return null;
				}
			};
		}
	}

}
