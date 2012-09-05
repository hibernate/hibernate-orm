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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.Orderable;
import org.hibernate.metamodel.spi.source.PluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.Sortable;
import org.hibernate.metamodel.spi.source.TableSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * @author Hardy Ferentschik
 */
public class PluralAttributeSourceImpl implements PluralAttributeSource, Orderable, Sortable {

	private final PluralAssociationAttribute attribute;
	private final Nature nature;
	private final ExplicitHibernateTypeSource typeSource;
	private final PluralAttributeKeySource keySource;
	private final PluralAttributeElementSource elementSource;

	public PluralAttributeSourceImpl(final PluralAssociationAttribute attribute) {
		this.attribute = attribute;
		this.nature = resolveAttributeNature();
		this.keySource = new PluralAttributeKeySourceImpl( attribute );
		this.elementSource = determineElementSource();
		this.typeSource = new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
				return attribute.getHibernateTypeResolver().getExplicitHibernateTypeName();
			}

			@Override
			public Map<String, String> getParameters() {
				return attribute.getHibernateTypeResolver().getExplicitHibernateTypeParameters();
			}
		};
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public PluralAttributeElementSource getElementSource() {
		return elementSource;
	}

	private PluralAttributeElementSource determineElementSource() {
		switch ( attribute.getNature() ) {
			case MANY_TO_MANY:
				return new ManyToManyPluralAttributeElementSourceImpl( attribute );
			case MANY_TO_ANY:
				return new ManyToAnyPluralAttributeElementSourceImpl( attribute );
			case ONE_TO_MANY:
				return new OneToManyPluralAttributeElementSourceImpl( attribute );
			case ELEMENT_COLLECTION_BASIC:
			case ELEMENT_COLLECTION_EMBEDDABLE: {
				return new BasicPluralAttributeElementSourceImpl( attribute );
			}
		}
		throw new AssertionError( "unexpected attribute nature" );
	}

	@Override
	public PluralAttributeKeySource getKeySource() {
		return keySource;
	}

	@Override
	public TableSpecificationSource getCollectionTableSpecificationSource() {
		// todo - see org.hibernate.metamodel.internal.Binder#bindOneToManyCollectionKey
		// todo - needs to cater for @CollectionTable and @JoinTable
		return new TableSource() {
			@Override
			public String getExplicitSchemaName() {
				return null;  //To change body of implemented methods use File | Settings | File Templates.
			}

			@Override
			public String getExplicitCatalogName() {
				return null;  //To change body of implemented methods use File | Settings | File Templates.
			}

			@Override
			public String getExplicitTableName() {
				return null;  //To change body of implemented methods use File | Settings | File Templates.
			}
		};
	}

	@Override
	public String getCollectionTableComment() {
		return null;
	}

	@Override
	public String getCollectionTableCheck() {
		return attribute.getCheckCondition();
	}

	@Override
	public Caching getCaching() {
		return attribute.getCaching();
	}

	@Override
	public String getCustomPersisterClassName() {
		return attribute.getCustomPersister();
	}

	@Override
	public String getWhere() {
		return attribute.getWhereClause();
	}

	@Override
	public boolean isInverse() {
		return attribute.getMappedBy() != null;
	}

	@Override
	public String getCustomLoaderName() {
		return attribute.getCustomLoaderName();
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return attribute.getCustomInsert();
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return attribute.getCustomUpdate();
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return attribute.getCustomDelete();
	}

	@Override
	public CustomSQL getCustomSqlDeleteAll() {
		return attribute.getCustomDeleteAll();
	}

	@Override
	public String getName() {
		return attribute.getName();
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
		return attribute.getAccessType();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return attribute.isOptimisticLockable();
	}

	@Override
	public Iterable<MetaAttributeSource> getMetaAttributeSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}

	@Override
	public FetchMode getFetchMode() {
		return attribute.getFetchMode();
	}

	@Override
	public String getOrder() {
		return attribute.getOrderBy();
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( getOrder() );
	}

	@Override
	public String getComparatorName() {
		return attribute.getComparatorName();
	}

	@Override
	public boolean isSorted() {
		return attribute.isSorted();
	}

	@Override
	public FetchTiming getFetchTiming() {
		if ( attribute.isExtraLazy() ) {
			return FetchTiming.EXTRA_DELAYED;
		}
		if ( attribute.isLazy() ) {
			return FetchTiming.DELAYED;
		}
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public FetchStyle getFetchStyle() {
		return attribute.getFetchStyle();
	}

	private Nature resolveAttributeNature() {
		if ( Map.class.isAssignableFrom( attribute.getAttributeType() ) ) {
			return PluralAttributeSource.Nature.MAP;
		}
		else if ( List.class.isAssignableFrom( attribute.getAttributeType() ) ) {
			return PluralAttributeSource.Nature.LIST;
		}
		else if ( Set.class.isAssignableFrom( attribute.getAttributeType() ) ) {
			return PluralAttributeSource.Nature.SET;
		}
		else {
			return PluralAttributeSource.Nature.BAG;
		}
	}
}


