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
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.binder.AttributeSourceContainer;
import org.hibernate.metamodel.source.binder.MetaAttributeSource;
import org.hibernate.metamodel.source.binder.PluralAttributeElementSource;
import org.hibernate.metamodel.source.binder.PluralAttributeKeySource;
import org.hibernate.metamodel.source.binder.PluralAttributeNature;
import org.hibernate.metamodel.source.binder.PluralAttributeSource;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLBagElement;

/**
 * @author Steve Ebersole
 */
public class BagAttributeSourceImpl implements PluralAttributeSource {
	private final XMLBagElement bagElement;
	private final AttributeSourceContainer container;

	// todo : a lot of this could be consolidated with common JAXB interface for collection mappings and moved to a base class

	private final PluralAttributeKeySource keySource;
	private final PluralAttributeElementSource elementSource;

	public BagAttributeSourceImpl(XMLBagElement bagElement, AttributeSourceContainer container) {
		this.bagElement = bagElement;
		this.container = container;

		this.keySource = new PluralAttributeKeySourceImpl( bagElement.getKey(), container );
		this.elementSource = interpretElementType( bagElement );
	}

	private PluralAttributeElementSource interpretElementType(XMLBagElement bagElement) {
		if ( bagElement.getElement() != null ) {
			return new BasicPluralAttributeElementSourceImpl( bagElement.getElement(), container.getLocalBindingContext() );
		}
		else if ( bagElement.getCompositeElement() != null ) {
			return new CompositePluralAttributeElementSourceImpl( bagElement.getCompositeElement(), container.getLocalBindingContext() );
		}
		else if ( bagElement.getOneToMany() != null ) {
			return new OneToManyPluralAttributeElementSourceImpl( bagElement.getOneToMany(), container.getLocalBindingContext() );
		}
		else if ( bagElement.getManyToMany() != null ) {
			return new ManyToManyPluralAttributeElementSourceImpl( bagElement.getManyToMany(), container.getLocalBindingContext() );
		}
		else if ( bagElement.getManyToAny() != null ) {
			throw new NotYetImplementedException( "Support for many-to-any not yet implemented" );
//			return PluralAttributeElementNature.MANY_TO_ANY;
		}
		else {
			throw new MappingException(
					"Unexpected collection element type : " + bagElement.getName(),
					bindingContext().getOrigin()
			);
		}
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
		return bagElement.getTable();
	}

	private LocalBindingContext bindingContext() {
		return container.getLocalBindingContext();
	}

	@Override
	public String getName() {
		return bagElement.getName();
	}

	@Override
	public boolean isSingular() {
		return false;
	}

	@Override
	public String getPropertyAccessorName() {
		return bagElement.getAccess();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return bagElement.isOptimisticLock();
	}

	@Override
	public boolean isInverse() {
		return bagElement.isInverse();
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Helper.buildMetaAttributeSources( bagElement.getMeta() );
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( bagElement.getCascade(), bindingContext() );
	}

	@Override
	public FetchMode getFetchMode() {
		return bagElement.getFetch() == null
				? FetchMode.DEFAULT
				: FetchMode.valueOf( bagElement.getFetch().value() );
	}
}
