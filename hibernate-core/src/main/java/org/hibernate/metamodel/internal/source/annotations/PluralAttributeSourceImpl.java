/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.Collections;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.Orderable;
import org.hibernate.metamodel.spi.source.PluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.Sortable;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * @author Hardy Ferentschik
 */
public class PluralAttributeSourceImpl implements PluralAttributeSource, Orderable, Sortable {

	private final PluralAssociationAttribute associationAttribute;
	private final Nature nature;
	private final ExplicitHibernateTypeSource typeSource;
	private final PluralAttributeKeySource keySource;
	private final PluralAttributeElementSource elementSource;

	public PluralAttributeSourceImpl(
			final PluralAssociationAttribute associationAttribute,
			final ConfiguredClass entityClass ) {
		this.associationAttribute = associationAttribute;
		this.keySource = new PluralAttributeKeySourceImpl( associationAttribute );
		this.typeSource = new ExplicitHibernateTypeSourceImpl( associationAttribute );
		this.nature = associationAttribute.getPluralAttributeNature();
		this.elementSource = determineElementSource( associationAttribute, entityClass );
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public PluralAttributeElementSource getElementSource() {
		return elementSource;
	}

	@Override
	public ValueHolder<Class<?>> getElementClassReference() {
		// needed for arrays
		Class<?> attributeType = associationAttribute.getAttributeType();
		if ( attributeType.isArray() ) {
			return new ValueHolder<Class<?>>( attributeType.getComponentType() );
		}
		else {
			return null;
		}
	}

	private static PluralAttributeElementSource determineElementSource(PluralAssociationAttribute associationAttribute, ConfiguredClass entityClass) {
		switch ( associationAttribute.getNature() ) {
			case MANY_TO_MANY:
				return new ManyToManyPluralAttributeElementSourceImpl( associationAttribute );
			case MANY_TO_ANY:
				return new ManyToAnyPluralAttributeElementSourceImpl( associationAttribute );
			case ONE_TO_MANY:
				return new OneToManyPluralAttributeElementSourceImpl( associationAttribute );
			case ELEMENT_COLLECTION_BASIC:
				return new BasicPluralAttributeElementSourceImpl( associationAttribute );
			case ELEMENT_COLLECTION_EMBEDDABLE: {
				// TODO: cascadeStyles?
				return new CompositePluralAttributeElementSourceImpl(
						associationAttribute, entityClass
				);
			}
		}
		throw new AssertionError( "Unexpected attribute nature for a association:" + associationAttribute.getNature() );
	}

	@Override
	public PluralAttributeKeySource getKeySource() {
		return keySource;
	}

	@Override
	public TableSpecificationSource getCollectionTableSpecificationSource() {
		// todo - see org.hibernate.metamodel.internal.Binder#bindOneToManyCollectionKey
		// todo - needs to cater for @CollectionTable and @JoinTable
		final AnnotationInstance joinTableAnnotation = associationAttribute.getJoinTableAnnotation();
		return joinTableAnnotation == null ? null : new TableSourceImpl( joinTableAnnotation );
	}

	@Override
	public String getCollectionTableComment() {
		return null;
	}

	@Override
	public String getCollectionTableCheck() {
		return associationAttribute.getCheckCondition();
	}

	@Override
	public Caching getCaching() {
		return associationAttribute.getCaching();
	}

	@Override
	public String getCustomPersisterClassName() {
		return associationAttribute.getCustomPersister();
	}

	@Override
	public String getWhere() {
		return associationAttribute.getWhereClause();
	}

	@Override
	public String getMappedBy() {
		return associationAttribute.getMappedBy();
	}

	@Override
	public boolean isInverse() {
		return getMappedBy() != null;
	}

	@Override
	public String getCustomLoaderName() {
		return associationAttribute.getCustomLoaderName();
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return associationAttribute.getCustomInsert();
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return associationAttribute.getCustomUpdate();
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return associationAttribute.getCustomDelete();
	}

	@Override
	public CustomSQL getCustomSqlDeleteAll() {
		return associationAttribute.getCustomDeleteAll();
	}

	@Override
	public String getName() {
		return associationAttribute.getName();
	}

	@Override
	public boolean isSingular() {
		return false;
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return associationAttribute.getAccessType();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return associationAttribute.isOptimisticLockable();
	}

	@Override
	public Iterable<MetaAttributeSource> getMetaAttributeSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}

	@Override
	public String getOrder() {
		return associationAttribute.getOrderBy();
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( getOrder() );
	}

	@Override
	public String getComparatorName() {
		return associationAttribute.getComparatorName();
	}

	@Override
	public boolean isSorted() {
		return associationAttribute.isSorted();
	}

	@Override
	public FetchTiming getFetchTiming() {
		if ( associationAttribute.isExtraLazy() ) {
			return FetchTiming.EXTRA_DELAYED;
		}
		else if ( associationAttribute.isLazy() ) {
			return FetchTiming.DELAYED;
		}
		else {
			return FetchTiming.IMMEDIATE;
		}
	}

	@Override
	public FetchStyle getFetchStyle() {
		return associationAttribute.getFetchStyle();
	}


}


