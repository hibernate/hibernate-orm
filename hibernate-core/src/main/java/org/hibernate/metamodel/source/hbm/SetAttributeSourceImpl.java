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
package org.hibernate.metamodel.source.hbm;

import org.hibernate.FetchMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.binder.AttributeSourceContainer;
import org.hibernate.metamodel.source.binder.MetaAttributeSource;
import org.hibernate.metamodel.source.binder.Orderable;
import org.hibernate.metamodel.source.binder.PluralAttributeElementSource;
import org.hibernate.metamodel.source.binder.PluralAttributeKeySource;
import org.hibernate.metamodel.source.binder.PluralAttributeNature;
import org.hibernate.metamodel.source.binder.PluralAttributeSource;
import org.hibernate.metamodel.source.binder.Sortable;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSetElement;

/**
 * @author Steve Ebersole
 */
public class SetAttributeSourceImpl implements PluralAttributeSource, Sortable, Orderable {
	private XMLSetElement setElement;
	private AttributeSourceContainer container;

	// todo : a lot of this could be consolidated with common JAXB interface for collection mappings and moved to a base class

	private final PluralAttributeKeySource keySource;
	private final PluralAttributeElementSource elementSource;

	public SetAttributeSourceImpl(XMLSetElement setElement, AttributeSourceContainer container) {
		this.setElement = setElement;
		this.container = container;

		this.keySource = new PluralAttributeKeySourceImpl( setElement.getKey(), container );
		this.elementSource = interpretElementType( setElement );
	}

	private PluralAttributeElementSource interpretElementType(XMLSetElement setElement) {
		if ( setElement.getElement() != null ) {
			return new BasicPluralAttributeElementSourceImpl( setElement.getElement(), container.getLocalBindingContext() );
		}
		else if ( setElement.getCompositeElement() != null ) {
			return new CompositePluralAttributeElementSourceImpl( setElement.getCompositeElement(), container.getLocalBindingContext() );
		}
		else if ( setElement.getOneToMany() != null ) {
			return new OneToManyPluralAttributeElementSourceImpl( setElement.getOneToMany(), container.getLocalBindingContext() );
		}
		else if ( setElement.getManyToMany() != null ) {
			return new ManyToManyPluralAttributeElementSourceImpl( setElement.getManyToMany(), container.getLocalBindingContext() );
		}
		else if ( setElement.getManyToAny() != null ) {
			throw new NotYetImplementedException( "Support for many-to-any not yet implemented" );
//			return PluralAttributeElementNature.MANY_TO_ANY;
		}
		else {
			throw new MappingException(
					"Unexpected collection element type : " + setElement.getName(),
					bindingContext().getOrigin()
			);
		}
	}

	private LocalBindingContext bindingContext() {
		return container.getLocalBindingContext();
	}

	@Override
	public PluralAttributeNature getPluralAttributeNature() {
		return PluralAttributeNature.BAG;
	}

	@Override
	public PluralAttributeKeySource getKeySource() {
		return keySource;
	}

	@Override
	public PluralAttributeElementSource getElementSource() {
		return elementSource;
	}

	@Override
	public String getExplicitCollectionTableName() {
		return setElement.getTable();
	}

	@Override
	public boolean isInverse() {
		return setElement.isInverse();
	}

	@Override
	public String getName() {
		return setElement.getName();
	}

	@Override
	public boolean isSingular() {
		return false;
	}

	@Override
	public String getPropertyAccessorName() {
		return setElement.getAccess();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return setElement.isOptimisticLock();
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Helper.buildMetaAttributeSources( setElement.getMeta() );
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( setElement.getCascade(), bindingContext() );
	}

	@Override
	public FetchMode getFetchMode() {
		return setElement.getFetch() == null
				? FetchMode.DEFAULT
				: FetchMode.valueOf( setElement.getFetch().value() );
	}

	@Override
	public boolean isSorted() {
		return StringHelper.isNotEmpty( setElement.getSort() );
	}

	@Override
	public String getComparatorName() {
		return setElement.getSort();
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( setElement.getOrderBy() );
	}

	@Override
	public String getOrder() {
		return setElement.getOrderBy();
	}
}
