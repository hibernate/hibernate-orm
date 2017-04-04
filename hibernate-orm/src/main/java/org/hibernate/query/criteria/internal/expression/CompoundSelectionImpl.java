/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.TupleElementImplementor;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

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
