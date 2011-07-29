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
import org.hibernate.metamodel.source.binder.PluralAttributeSource;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.PluralAttributeElement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPluralAttributeSourceImpl implements PluralAttributeSource {
	private final PluralAttributeElement pluralAttributeElement;
	private final AttributeSourceContainer container;

	private final PluralAttributeKeySource keySource;
	private final PluralAttributeElementSource elementSource;

	protected AbstractPluralAttributeSourceImpl(
			PluralAttributeElement pluralAttributeElement,
			AttributeSourceContainer container) {
		this.pluralAttributeElement = pluralAttributeElement;
		this.container = container;

		this.keySource = new PluralAttributeKeySourceImpl( pluralAttributeElement.getKey(), container );
		this.elementSource = interpretElementType();
	}

	private PluralAttributeElementSource interpretElementType() {
		if ( pluralAttributeElement.getElement() != null ) {
			return new BasicPluralAttributeElementSourceImpl(
					pluralAttributeElement.getElement(), container.getLocalBindingContext()
			);
		}
		else if ( pluralAttributeElement.getCompositeElement() != null ) {
			return new CompositePluralAttributeElementSourceImpl(
					pluralAttributeElement.getCompositeElement(), container.getLocalBindingContext()
			);
		}
		else if ( pluralAttributeElement.getOneToMany() != null ) {
			return new OneToManyPluralAttributeElementSourceImpl(
					pluralAttributeElement.getOneToMany(), container.getLocalBindingContext()
			);
		}
		else if ( pluralAttributeElement.getManyToMany() != null ) {
			return new ManyToManyPluralAttributeElementSourceImpl(
					pluralAttributeElement.getManyToMany(), container.getLocalBindingContext()
			);
		}
		else if ( pluralAttributeElement.getManyToAny() != null ) {
			throw new NotYetImplementedException( "Support for many-to-any not yet implemented" );
//			return PluralAttributeElementNature.MANY_TO_ANY;
		}
		else {
			throw new MappingException(
					"Unexpected collection element type : " + pluralAttributeElement.getName(),
					bindingContext().getOrigin()
			);
		}
	}

	public PluralAttributeElement getPluralAttributeElement() {
		return pluralAttributeElement;
	}

	protected AttributeSourceContainer container() {
		return container;
	}

	protected LocalBindingContext bindingContext() {
		return container().getLocalBindingContext();
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
	public String getExplicitSchemaName() {
		return pluralAttributeElement.getSchema();
	}

	@Override
	public String getExplicitCatalogName() {
		return pluralAttributeElement.getCatalog();
	}

	@Override
	public String getExplicitCollectionTableName() {
		return pluralAttributeElement.getTable();
	}

	@Override
	public String getCollectionTableComment() {
		return pluralAttributeElement.getComment();
	}

	@Override
	public String getCollectionTableCheck() {
		return pluralAttributeElement.getCheck();
	}

	@Override
	public String getWhere() {
		return pluralAttributeElement.getWhere();
	}

	@Override
	public String getName() {
		return pluralAttributeElement.getName();
	}

	@Override
	public boolean isSingular() {
		return false;
	}

	@Override
	public String getPropertyAccessorName() {
		return pluralAttributeElement.getAccess();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return pluralAttributeElement.isOptimisticLock();
	}

	@Override
	public boolean isInverse() {
		return pluralAttributeElement.isInverse();
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Helper.buildMetaAttributeSources( pluralAttributeElement.getMeta() );
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( pluralAttributeElement.getCascade(), bindingContext() );
	}

	@Override
	public FetchMode getFetchMode() {
		return pluralAttributeElement.getFetch() == null
				? FetchMode.DEFAULT
				: FetchMode.valueOf( pluralAttributeElement.getFetch().value() );
	}
}
