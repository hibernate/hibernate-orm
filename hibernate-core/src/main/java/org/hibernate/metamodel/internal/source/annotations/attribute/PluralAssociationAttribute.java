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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FetchType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SortType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;

/**
 * Represents an collection (collection, list, set, map) association attribute.
 *
 * @author Hardy Ferentschik
 * @author Strong Liu
 */
public class PluralAssociationAttribute extends AssociationAttribute {
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
	private final boolean isIndexed;
	// Used for the non-owning side of a ManyToMany relationship
	private final String inverseForeignKeyName;
	private final String explicitForeignKeyName;

	private final PluralAttributeSource.Nature pluralAttributeNature;

	private LazyCollectionOption lazyOption;

	public static PluralAssociationAttribute createPluralAssociationAttribute(ClassInfo entityClassInfo,
																			  String name,
																			  Class<?> attributeType,
																			  Class<?> referencedAttributeType,
																			  Nature attributeNature,
																			  String accessType,
																			  Map<DotName, List<AnnotationInstance>> annotations,
																			  EntityBindingContext context) {
		return new PluralAssociationAttribute(
				entityClassInfo,
				name,
				attributeType,
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

	public boolean isIndexed() {
		return isIndexed;
	}

	private PluralAssociationAttribute(ClassInfo entityClassInfo,
									   String name,
									   Class<?> attributeType,
									   Class<?> referencedAttributeType,
									   Nature associationType,
									   String accessType,
									   Map<DotName, List<AnnotationInstance>> annotations,
									   EntityBindingContext context) {
		super( name, attributeType, referencedAttributeType, associationType, accessType, annotations, context );
		this.entityClassInfo = entityClassInfo;
		this.whereClause = determineWereClause();
		this.orderBy = determineOrderBy();

		AnnotationInstance foreignKey = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.FOREIGN_KEY
		);
		if ( foreignKey != null ) {
			explicitForeignKeyName = JandexHelper.getValue( foreignKey, "name", String.class );
			String temp = JandexHelper.getValue( foreignKey, "inverseName", String.class );
			inverseForeignKeyName = StringHelper.isNotEmpty( temp ) ? temp : null;
		}
		else {
			explicitForeignKeyName = null;
			inverseForeignKeyName = null;
		}

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

		final AnnotationInstance sortAnnotation =  JandexHelper.getSingleAnnotation( annotations, HibernateDotNames.SORT );
		if ( sortAnnotation == null ) {
			this.sorted = false;
			this.comparatorName = null;
		}
		else {
			final SortType sortType = JandexHelper.getEnumValue( sortAnnotation, "type", SortType.class );
			this.sorted = sortType != SortType.UNSORTED;
			if ( this.sorted && sortType == SortType.COMPARATOR ) {
				String comparatorName = JandexHelper.getValue( sortAnnotation, "comparator", String.class );
				if ( StringHelper.isEmpty( comparatorName ) ) {
					throw new MappingException(
							"Comparator class must be provided when using SortType.COMPARATOR on property: "+ getRole(),
							getContext().getOrigin()
					);
				}
				this.comparatorName = comparatorName;
			}
			else {
				this.comparatorName = null;
			}
		}

		AnnotationInstance orderColumnAnnotation =  JandexHelper.getSingleAnnotation( annotations, JPADotNames.ORDER_COLUMN );
		AnnotationInstance indexColumnAnnotation = JandexHelper.getSingleAnnotation( annotations, HibernateDotNames.INDEX_COLUMN );
		if ( orderColumnAnnotation != null && indexColumnAnnotation != null ) {
			throw new MappingException(
					"@OrderColumn and @IndexColumn can't be used together on property: " + getRole(),
					getContext().getOrigin()
			);
		}
		this.isIndexed = orderColumnAnnotation != null || indexColumnAnnotation != null;
		this.pluralAttributeNature = resolvePluralAttributeNature();
	}

	private PluralAttributeSource.Nature resolvePluralAttributeNature() {

		if ( Map.class.isAssignableFrom( getAttributeType() ) ) {
			return PluralAttributeSource.Nature.MAP;
		}
		else if ( List.class.isAssignableFrom( getAttributeType() ) ) {
			return isIndexed() ? PluralAttributeSource.Nature.LIST : PluralAttributeSource.Nature.BAG;
		}
		else if ( Set.class.isAssignableFrom( getAttributeType() ) ) {
			return PluralAttributeSource.Nature.SET;
		}
		else if ( getAttributeType().isArray() ) {
			return PluralAttributeSource.Nature.ARRAY;
		}
		else {
			return PluralAttributeSource.Nature.BAG;
		}
	}

	private OnDeleteAction determineOnDeleteAction() {

		OnDeleteAction action = null;
		final AnnotationInstance onDeleteAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.ON_DELETE
		);
		if ( onDeleteAnnotation != null ) {
			action = JandexHelper.getValue( onDeleteAnnotation, "action", OnDeleteAction.class );
		}
		return action;
	}

	@Override
	public boolean isOptimisticLockable() {
		return hasOptimisticLockAnnotation() ? super.isOptimisticLockable() : StringHelper.isEmpty( getMappedBy() );
	}

	private String determineCustomLoaderName() {
		String loader = null;
		final AnnotationInstance customLoaderAnnotation = JandexHelper.getSingleAnnotation(
				annotations(), HibernateDotNames.LOADER
		);
		if ( customLoaderAnnotation != null ) {
			loader = JandexHelper.getValue( customLoaderAnnotation, "namedQuery", String.class );
		}
		return loader;
	}

	private String determineCustomPersister() {
		String entityPersisterClass = null;
		final AnnotationInstance persisterAnnotation = JandexHelper.getSingleAnnotation(
				annotations(), HibernateDotNames.PERSISTER
		);
		if ( persisterAnnotation != null ) {
			entityPersisterClass = JandexHelper.getValue( persisterAnnotation, "impl", String.class );
		}
		return entityPersisterClass;
	}

	@Override
	protected boolean determineIsLazy(AnnotationInstance associationAnnotation) {
		FetchType fetchType = JandexHelper.getEnumValue( associationAnnotation, "fetch", FetchType.class );
		boolean lazy = fetchType == FetchType.LAZY;
		final AnnotationInstance lazyCollectionAnnotationInstance = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.LAZY_COLLECTION
		);
		if ( lazyCollectionAnnotationInstance != null ) {
			lazyOption = JandexHelper.getEnumValue(
					lazyCollectionAnnotationInstance,
					"value",
					LazyCollectionOption.class
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
			where = JandexHelper.getValue( whereAnnotation, "clause", String.class );
		}

		return where;
	}

	private String determineOrderBy() {
		String orderBy = null;

		AnnotationInstance hibernateWhereAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.ORDER_BY
		);

		AnnotationInstance jpaWhereAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				JPADotNames.ORDER_BY
		);

		if ( jpaWhereAnnotation != null && hibernateWhereAnnotation != null ) {
			throw new AnnotationException(
					"Cannot use sql order by clause (@org.hibernate.annotations.OrderBy) " +
							"in conjunction with JPA order by clause (@java.persistence.OrderBy) on  " + getRole()
			);
		}

		if ( hibernateWhereAnnotation != null ) {
			orderBy = JandexHelper.getValue( hibernateWhereAnnotation, "clause", String.class );
		}

		if ( jpaWhereAnnotation != null ) {
			// todo
			// this could be an empty string according to JPA spec 11.1.38 -
			// If the ordering element is not specified for an entity association, ordering by the primary key of the
			// associated entity is assumed
			// The binder will need to take this into account and generate the right property names
			orderBy = JandexHelper.getValue( jpaWhereAnnotation, "value", String.class );
		}

		return orderBy;
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
}


