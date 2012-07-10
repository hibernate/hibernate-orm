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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbCompositeElementElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbTuplizerElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.binder.AttributeSource;
import org.hibernate.metamodel.source.binder.CompositePluralAttributeElementSource;
import org.hibernate.metamodel.source.binder.PluralAttributeElementNature;

/**
 * @author Steve Ebersole
 */
public class CompositePluralAttributeElementSourceImpl implements CompositePluralAttributeElementSource {
	private final JaxbCompositeElementElement compositeElement;
	private final LocalBindingContext bindingContext;

	public CompositePluralAttributeElementSourceImpl(
			JaxbCompositeElementElement compositeElement,
			LocalBindingContext bindingContext) {
		this.compositeElement = compositeElement;
		this.bindingContext = bindingContext;
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.COMPONENT;
	}

	@Override
	public String getClassName() {
		return bindingContext.qualifyClassName( compositeElement.getClazz() );
	}

	@Override
	public ValueHolder<Class<?>> getClassReference() {
		return bindingContext.makeClassReference( getClassName() );
	}

	@Override
	public String getParentReferenceAttributeName() {
		return compositeElement.getParent() != null
				? compositeElement.getParent().getName()
				: null;
	}

	@Override
	public String getExplicitTuplizerClassName() {
		if ( compositeElement.getTuplizer() == null ) {
			return null;
		}
		final EntityMode entityMode = StringHelper.isEmpty( compositeElement.getClazz() ) ? EntityMode.MAP : EntityMode.POJO;
		for ( JaxbTuplizerElement tuplizerElement : compositeElement.getTuplizer() ) {
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode() ) ) {
				return tuplizerElement.getClazz();
			}
		}
		return null;
	}

	@Override
	public String getPath() {
		// todo : implementing this requires passing in the collection source and being able to resolve the collection's role
		return null;
	}

	@Override
	public Iterable<AttributeSource> attributeSources() {
		List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
		for ( Object attribute : compositeElement.getPropertyOrManyToOneOrAny() ) {

		}
		return attributeSources;
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return bindingContext;
	}
}
