/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.Selection;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.Renderable;
import org.hibernate.jpa.criteria.TupleElementImplementor;
import org.hibernate.jpa.criteria.ValueHandlerFactory;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * The Hibernate implementation of the JPA {@link CompoundSelection}
 * contract.
 *
 * @author Steve Ebersole
 */
public class CompoundSelectionImpl<X>
		extends SelectionImpl<X>
		implements CompoundSelection<X>, Renderable, Serializable {
	private final boolean isConstructor;
	private List<Selection<?>> selectionItems;

	public CompoundSelectionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			List<Selection<?>> selectionItems) {
		super( criteriaBuilder, javaType );
		this.isConstructor = !javaType.isArray() && !Tuple.class.isAssignableFrom( javaType );
		this.selectionItems = selectionItems;
	}

	@Override
	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public List<Selection<?>> getCompoundSelectionItems() {
		return selectionItems;
	}

	@Override
	public List<ValueHandlerFactory.ValueHandler> getValueHandlers() {
		if ( isConstructor ) {
			return null;
		}
		boolean foundHandlers = false;
		ArrayList<ValueHandlerFactory.ValueHandler> valueHandlers = new ArrayList<ValueHandlerFactory.ValueHandler>();
		for ( Selection selection : getCompoundSelectionItems() ) {
			ValueHandlerFactory.ValueHandler valueHandler = ( (TupleElementImplementor) selection ).getValueHandler();
			valueHandlers.add( valueHandler );
			foundHandlers = foundHandlers || valueHandler != null;
		}
		return foundHandlers ? null : valueHandlers;
	}

	public void registerParameters(ParameterRegistry registry) {
		for ( Selection selectionItem : getCompoundSelectionItems() ) {
			Helper.possibleParameter(selectionItem, registry);
		}
	}

	public String render(RenderingContext renderingContext) {
		StringBuilder buff = new StringBuilder();
		if ( isConstructor ) {
			buff.append( "new " ).append( getJavaType().getName() ).append( '(' );
		}
		String sep = "";
		for ( Selection selection : selectionItems ) {
			buff.append( sep )
					.append( ( (Renderable) selection ).renderProjection( renderingContext ) );
			sep = ", ";
		}
		if ( isConstructor ) {
			buff.append( ')' );
		}
		return buff.toString();
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
