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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;

/**
 * Represents an collection (collection, list, set, map) association attribute.
 *
 * @author Hardy Ferentschik
 */
public class PluralAssociationAttribute extends AssociationAttribute {
	private final String whereClause;
	private final String orderBy;
	private final Caching caching;
	private final String customPersister;
	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;
	private final ClassInfo entityClassInfo;
	private final boolean isExtraLazy;
	private LazyCollectionOption lazyOption;


	// Used for the non-owning side of a ManyToMany relationship
	private final String inverseForeignKeyName;

	public static PluralAssociationAttribute createPluralAssociationAttribute(ClassInfo entityClassInfo,
																			  String name,
																			  Class<?> attributeType,
																			  AttributeNature attributeNature,
																			  String accessType,
																			  Map<DotName, List<AnnotationInstance>> annotations,
																			  EntityBindingContext context) {
		return new PluralAssociationAttribute(
				entityClassInfo,
				name,
				attributeType,
				attributeNature,
				accessType,
				annotations,
				context
		);
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

	public Caching getCaching() {
		return caching;
	}

	public String getCustomPersister() {
		return customPersister;
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

	private PluralAssociationAttribute(ClassInfo entityClassInfo,
									   String name,
									   Class<?> javaType,
									   AttributeNature associationType,
									   String accessType,
									   Map<DotName, List<AnnotationInstance>> annotations,
									   EntityBindingContext context) {
		super( name, javaType, associationType, accessType, annotations, context );
		this.entityClassInfo = entityClassInfo;
		this.whereClause = determineWereClause();
		this.orderBy = determineOrderBy();
		this.inverseForeignKeyName = determineInverseForeignKeyName();
		this.caching = determineCachingSettings();
		this.isExtraLazy = lazyOption == LazyCollectionOption.EXTRA;
		this.customPersister = determineCustomPersister();
		this.customInsert = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_INSERT, annotations()
		);
		this.customUpdate = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_UPDATE, annotations()
		);
		this.customDelete = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_DELETE, annotations()
		);
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

	private String determineInverseForeignKeyName() {
		String foreignKeyName = null;

		AnnotationInstance foreignKey = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.FOREIGN_KEY
		);
		if ( foreignKey != null &&
				StringHelper.isNotEmpty( JandexHelper.getValue( foreignKey, "inverseName", String.class ) ) ) {
			foreignKeyName = JandexHelper.getValue( foreignKey, "inverseName", String.class );
		}

		return foreignKeyName;
	}

	@Override
	protected boolean determinIsLazy(AnnotationInstance associationAnnotation) {
		boolean lazy =  super.determinIsLazy( associationAnnotation );
		final AnnotationInstance lazyCollectionAnnotationInstance = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.LAZY_COLLECTION
		);
		if ( lazyCollectionAnnotationInstance != null ) {
			LazyCollectionOption option = JandexHelper.getEnumValue(
					lazyCollectionAnnotationInstance,
					"value",
					LazyCollectionOption.class
			);
			return option == LazyCollectionOption.TRUE;

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
							"in conjunction with JPA order by clause (@java.persistence.OrderBy) on  " + getName()
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


