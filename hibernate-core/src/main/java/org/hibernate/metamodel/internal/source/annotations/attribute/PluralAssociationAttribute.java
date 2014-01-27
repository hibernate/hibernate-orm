/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.attribute;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.persistence.FetchType;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SortType;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.CompositeAttributeTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.EnumeratedTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.HibernateTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.LobTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.TemporalTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Represents an collection (collection, list, set, map) association attribute.
 *
 * @author Hardy Ferentschik
 * @author Strong Liu
 */
public class PluralAssociationAttribute extends AssociationAttribute {
	private final Class<?> indexType;
	private final String whereClause;
	private final String orderBy;
	private final boolean sorted;
	private final String comparatorName;
	private final Caching caching;
	private final String customPersister;
	private final String customLoaderName;
	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;
	private final CustomSQL customDeleteAll;
	private final ClassInfo entityClassInfo;
	private final boolean isExtraLazy;
	private final OnDeleteAction onDeleteAction;
	private final boolean isSequentiallyIndexed;
	// Used for the non-owning side of a ManyToMany relationship
	private final String inverseForeignKeyName;
	private final String explicitForeignKeyName;

	private final PluralAttributeSource.Nature pluralAttributeNature;

	private static final EnumSet<PluralAttributeSource.Nature> SHOULD_NOT_HAS_COLLECTION_ID = EnumSet.of( PluralAttributeSource.Nature.SET,
			PluralAttributeSource.Nature.MAP, PluralAttributeSource.Nature.LIST, PluralAttributeSource.Nature.ARRAY );

	private LazyCollectionOption lazyOption;
	private final boolean isCollectionIdPresent;
	private final boolean mutable;
	private final int batchSize;

	private AttributeTypeResolver elementTypeResolver;
	private AttributeTypeResolver indexTypeResolver;

	public static PluralAssociationAttribute createPluralAssociationAttribute(
			ClassInfo entityClassInfo,
			String name,
			Class<?> attributeType,
			Class<?> indexType,
			Class<?> referencedAttributeType,
			Nature attributeNature,
			String accessType,
			Map<DotName, List<AnnotationInstance>> annotations,
			EntityBindingContext context) {
		return new PluralAssociationAttribute(
				entityClassInfo,
				name,
				attributeType,
				indexType,
				referencedAttributeType,
				attributeNature,
				accessType,
				annotations,
				context
		);
	}

	public PluralAttributeSource.Nature getPluralAttributeNature() {
		return pluralAttributeNature;
	}

	public Class<?> getIndexType() {
		return indexType;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public String getInverseForeignKeyName() {
		return inverseForeignKeyName;
	}
	public String getExplicitForeignKeyName(){
		return explicitForeignKeyName;
	}

	public Caching getCaching() {
		return caching;
	}

	public String getCustomPersister() {
		return customPersister;
	}

	public String getCustomLoaderName() {
		return customLoaderName;
	}

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public CustomSQL getCustomDeleteAll() {
		return customDeleteAll;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "PluralAssociationAttribute" );
		sb.append( "{name='" ).append( getName() ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
	public OnDeleteAction getOnDeleteAction() {
		return onDeleteAction;
	}

	public String getComparatorName() {
		return comparatorName;
	}

	public boolean isSorted() {
		return sorted;
	}

	public boolean isSequentiallyIndexed() {
		return isSequentiallyIndexed;
	}

	private PluralAssociationAttribute(
			final ClassInfo entityClassInfo,
			final String name,
			final Class<?> attributeType,
			final Class<?> indexType,
			final Class<?> referencedAttributeType,
			final Nature associationType,
			final String accessType,
			final Map<DotName, List<AnnotationInstance>> annotations,
			final EntityBindingContext context) {
		super( entityClassInfo, name, attributeType, referencedAttributeType, associationType, accessType, annotations, context );
		this.entityClassInfo = entityClassInfo;
		this.indexType = indexType;
		this.whereClause = determineWereClause();
		this.orderBy = determineOrderBy();

		AnnotationInstance foreignKey = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.FOREIGN_KEY
		);
		if ( foreignKey != null ) {
			explicitForeignKeyName = JandexHelper.getValue( foreignKey, "name", String.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
			String temp = JandexHelper.getValue( foreignKey, "inverseName", String.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
			inverseForeignKeyName = StringHelper.isNotEmpty( temp ) ? temp : null;
		}
		else {
			explicitForeignKeyName = null;
			inverseForeignKeyName = null;
		}

		this.mutable = JandexHelper.getSingleAnnotation( annotations(), HibernateDotNames.IMMUTABLE ) == null;

		this.caching = determineCachingSettings();
		this.isExtraLazy = lazyOption == LazyCollectionOption.EXTRA;
		this.customPersister = determineCustomPersister();
		this.customLoaderName = determineCustomLoaderName();
		this.customInsert = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_INSERT, annotations()
		);
		this.customUpdate = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_UPDATE, annotations()
		);
		this.customDelete = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_DELETE, annotations()
		);
		this.customDeleteAll = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_DELETE_ALL, annotations()
		);
		this.onDeleteAction = determineOnDeleteAction();
		this.isCollectionIdPresent = JandexHelper.getSingleAnnotation(
				annotations,
				HibernateDotNames.COLLECTION_ID
		) != null;
		final AnnotationInstance sortAnnotation =  JandexHelper.getSingleAnnotation( annotations, HibernateDotNames.SORT );
		final AnnotationInstance sortNaturalAnnotation = JandexHelper.getSingleAnnotation( annotations, HibernateDotNames.SORT_NATURAL );
		final AnnotationInstance sortComparatorAnnotation = JandexHelper.getSingleAnnotation( annotations, HibernateDotNames.SORT_COMPARATOR );

		if ( sortNaturalAnnotation != null ) {
			this.sorted = true;
			this.comparatorName = "natural";
		}
		else if ( sortComparatorAnnotation != null ) {
			this.sorted = true;
			this.comparatorName = JandexHelper.getValue( sortComparatorAnnotation, "value", String.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}
		else if ( sortAnnotation != null ) {
			final SortType sortType = JandexHelper.getEnumValue( sortAnnotation, "type", SortType.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
			switch ( sortType ){

				case NATURAL:
					this.sorted = true;
					this.comparatorName = "natural";
					break;
				case COMPARATOR:
					String comparatorName = JandexHelper.getValue( sortAnnotation, "comparator", String.class,
							getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
					if ( StringHelper.isEmpty( comparatorName ) ) {
						throw new MappingException(
								"Comparator class must be provided when using SortType.COMPARATOR on property: " + getRole(),
								getContext().getOrigin()
						);
					}
					this.sorted = true;
					this.comparatorName = comparatorName;
					break;
				default:
					this.sorted = false;
					this.comparatorName = null;
					break;
			}
		}
		else {
			this.sorted = false;
			this.comparatorName = null;
		}


		AnnotationInstance orderColumnAnnotation =  JandexHelper.getSingleAnnotation( annotations, JPADotNames.ORDER_COLUMN );
		AnnotationInstance indexColumnAnnotation = JandexHelper.getSingleAnnotation( annotations, HibernateDotNames.INDEX_COLUMN );
		if ( orderColumnAnnotation != null && indexColumnAnnotation != null ) {
			throw new MappingException(
					"@OrderColumn and @IndexColumn can't be used together on property: " + getRole(),
					getContext().getOrigin()
			);
		}
		this.isSequentiallyIndexed = orderColumnAnnotation != null || indexColumnAnnotation != null;
		this.pluralAttributeNature = resolvePluralAttributeNature();

		AnnotationInstance batchAnnotation = JandexHelper.getSingleAnnotation( annotations, HibernateDotNames.BATCH_SIZE );
		if ( batchAnnotation != null ) {
			this.batchSize = batchAnnotation.value( "size" ).asInt();
		}
		else {
			this.batchSize = -1;
		}
		validateMapping();
	}

	private void validateMapping() {
		checkSortedTypeIsSortable();
		checkIfCollectionIdIsWronglyPlaced();
	}

	private void checkIfCollectionIdIsWronglyPlaced() {
		if ( isCollectionIdPresent && SHOULD_NOT_HAS_COLLECTION_ID.contains( pluralAttributeNature ) ) {
			throw new MappingException(
					"The Collection type doesn't support @CollectionId annotation: " + getRole(),
					getContext().getOrigin()
			);
		}
	}

	private void checkSortedTypeIsSortable() {
		//shortcut, a little performance improvement of avoiding the class type check
		if ( pluralAttributeNature == PluralAttributeSource.Nature.MAP
				|| pluralAttributeNature == PluralAttributeSource.Nature.SET ) {
			if ( SortedMap.class.isAssignableFrom( getAttributeType() )
					|| SortedSet.class.isAssignableFrom( getAttributeType() ) ) {
				if ( !isSorted() ) {
					throw new MappingException(
							"A sorted collection has to define @Sort: " + getRole(),
							getContext().getOrigin()
					);
				}
			}
		}

	}


	//TODO org.hibernate.cfg.annotations.CollectionBinder#hasToBeSorted
	private PluralAttributeSource.Nature resolvePluralAttributeNature() {

		if ( Map.class.isAssignableFrom( getAttributeType() ) ) {
			return PluralAttributeSource.Nature.MAP;
		}
		else if ( List.class.isAssignableFrom( getAttributeType() ) ) {
			if ( isSequentiallyIndexed() ) {
				return PluralAttributeSource.Nature.LIST;
			}
			else if ( isCollectionIdPresent ) {
				return PluralAttributeSource.Nature.ID_BAG;
			}
			else {
				return PluralAttributeSource.Nature.BAG;
			}
		}
		else if ( Set.class.isAssignableFrom( getAttributeType() ) ) {
			return PluralAttributeSource.Nature.SET;
		}
		else if ( getAttributeType().isArray() ) {
			return PluralAttributeSource.Nature.ARRAY;
		}
		else if ( Collection.class.isAssignableFrom( getAttributeType() ) ) {
			return isCollectionIdPresent ? PluralAttributeSource.Nature.ID_BAG : PluralAttributeSource.Nature.BAG;
		}
		else {
			return PluralAttributeSource.Nature.BAG;
		}
	}

	//todo duplicated with the one in EntityClass
	private OnDeleteAction determineOnDeleteAction() {
		final AnnotationInstance onDeleteAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.ON_DELETE
		);
		if ( onDeleteAnnotation != null ) {
			return JandexHelper.getEnumValue( onDeleteAnnotation, "action", OnDeleteAction.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}
		return null;
	}

	@Override
	public boolean isOptimisticLockable() {
		return hasOptimisticLockAnnotation() ? super.isOptimisticLockable() : StringHelper.isEmpty( getMappedBy() );
	}

	public int getBatchSize() {
		return batchSize;
	}

	private String determineCustomLoaderName() {
		String loader = null;
		final AnnotationInstance customLoaderAnnotation = JandexHelper.getSingleAnnotation(
				annotations(), HibernateDotNames.LOADER
		);
		if ( customLoaderAnnotation != null ) {
			loader = JandexHelper.getValue( customLoaderAnnotation, "namedQuery", String.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}
		return loader;
	}

	private String determineCustomPersister() {
		String entityPersisterClass = null;
		final AnnotationInstance persisterAnnotation = JandexHelper.getSingleAnnotation(
				annotations(), HibernateDotNames.PERSISTER
		);
		if ( persisterAnnotation != null ) {
			entityPersisterClass = JandexHelper.getValue( persisterAnnotation, "impl", String.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}
		return entityPersisterClass;
	}

	@Override
	protected boolean determineIsLazy(AnnotationInstance associationAnnotation) {
		FetchType fetchType = JandexHelper.getEnumValue( associationAnnotation, "fetch", FetchType.class,
				getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		boolean lazy = fetchType == FetchType.LAZY;
		final AnnotationInstance lazyCollectionAnnotationInstance = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.LAZY_COLLECTION
		);
		if ( lazyCollectionAnnotationInstance != null ) {
			lazyOption = JandexHelper.getEnumValue(
					lazyCollectionAnnotationInstance,
					"value",
					LazyCollectionOption.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class )
			);
			lazy = !( lazyOption == LazyCollectionOption.FALSE );

		}
		final AnnotationInstance fetchAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.FETCH
		);
		if ( fetchAnnotation != null && fetchAnnotation.value() != null ) {
			FetchMode fetchMode = FetchMode.valueOf( fetchAnnotation.value( ).asEnum().toUpperCase() );
			if ( fetchMode == FetchMode.JOIN ) {
				lazy = false;
			}
		}
		return lazy;
	}

	public boolean isExtraLazy() {
		return isExtraLazy;
	}

	private String determineWereClause() {
		String where = null;

		AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation( annotations(), HibernateDotNames.WHERE );
		if ( whereAnnotation != null ) {
			where = JandexHelper.getValue( whereAnnotation, "clause", String.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}

		return where;
	}

	private String determineOrderBy() {
		String orderBy = null;

		AnnotationInstance hibernateOrderByAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.ORDER_BY
		);

		AnnotationInstance jpaOrderByAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				JPADotNames.ORDER_BY
		);

		if ( jpaOrderByAnnotation != null && hibernateOrderByAnnotation != null ) {
			throw new MappingException(
					"Cannot use sql order by clause (@org.hibernate.annotations.OrderBy) " +
							"in conjunction with JPA order by clause (@java.persistence.OrderBy) on  " + getRole(),
					getContext().getOrigin()
			);
		}

		if ( hibernateOrderByAnnotation != null ) {
			orderBy = JandexHelper.getValue( hibernateOrderByAnnotation, "clause", String.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		}

		if ( jpaOrderByAnnotation != null ) {
			// this could be an empty string according to JPA spec 11.1.38 -
			// If the ordering element is not specified for an entity association, ordering by the primary key of the
			// associated entity is assumed
			// The binder will need to take this into account and generate the right property names
			orderBy = JandexHelper.getValue( jpaOrderByAnnotation, "value", String.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
			if ( orderBy == null ) {
				orderBy = isBasicCollection() ?  "$element$ asc" :"id asc" ;
			}
			if ( orderBy.equalsIgnoreCase( "desc" ) ) {
				orderBy = isBasicCollection() ? "$element$ desc" :"id desc";
			}
		}

		return orderBy;
	}

	private boolean isBasicCollection(){
		return getNature() == Nature.ELEMENT_COLLECTION_BASIC || getNature() == Nature.ELEMENT_COLLECTION_EMBEDDABLE;
	}

	private Caching determineCachingSettings() {
		Caching caching = null;
		final AnnotationInstance hibernateCacheAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.CACHE
		);
		if ( hibernateCacheAnnotation != null ) {
			org.hibernate.cache.spi.access.AccessType accessType;
			if ( hibernateCacheAnnotation.value( "usage" ) == null ) {
				accessType = getContext().getMappingDefaults().getCacheAccessType();
			}
			else {
				accessType = CacheConcurrencyStrategy.parse( hibernateCacheAnnotation.value( "usage" ).asEnum() )
						.toAccessType();
			}

			return new Caching(
					hibernateCacheAnnotation.value( "region" ) == null
							? StringHelper.qualify( entityClassInfo.name().toString(), getName() )
							: hibernateCacheAnnotation.value( "region" ).asString(),
					accessType,
					hibernateCacheAnnotation.value( "include" ) != null
							&& "all".equals( hibernateCacheAnnotation.value( "include" ).asString() )
			);
		}
		return caching;
	}


	public boolean isMutable() {
		return mutable;
	}

	@Override
	public AttributeTypeResolver getHibernateTypeResolver() {
		if ( elementTypeResolver == null ) {
			elementTypeResolver = getDefaultElementHibernateTypeResolver();
		}
		return elementTypeResolver;
	}

	private AttributeTypeResolver getDefaultElementHibernateTypeResolver() {
		CompositeAttributeTypeResolver resolver = new CompositeAttributeTypeResolver( this );
		resolver.addHibernateTypeResolver( HibernateTypeResolver.createCollectionElementTypeResolver( this ) );
		resolver.addHibernateTypeResolver( TemporalTypeResolver.createCollectionElementTypeResolver( this ) );
		resolver.addHibernateTypeResolver( LobTypeResolver.createCollectionElementTypeResolve( this ) );
		resolver.addHibernateTypeResolver( EnumeratedTypeResolver.createCollectionElementTypeResolver( this ) );
		return resolver;
	}

	public AttributeTypeResolver getIndexTypeResolver() {
		if ( indexType == null ) {
				return getDefaultHibernateTypeResolver();
		}
		else if ( indexTypeResolver == null ) {
			CompositeAttributeTypeResolver resolver = new CompositeAttributeTypeResolver( this );
			final String name = getName() + ".index";
			resolver.addHibernateTypeResolver( HibernateTypeResolver.createCollectionIndexTypeResolver( this ) );
			// TODO: Lob allowed as collection index? I don't see an annotation for that.
			resolver.addHibernateTypeResolver( EnumeratedTypeResolver.createCollectionIndexTypeResolver( this ) );
			resolver.addHibernateTypeResolver( TemporalTypeResolver.createCollectionIndexTypeResolver( this ) );
			indexTypeResolver = resolver;
		}
		return indexTypeResolver;
	}
}


