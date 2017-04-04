/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterContainer;
import org.hibernate.query.criteria.internal.SelectionImplementor;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;

/**
 * The Hibernate implementation of the JPA {@link Selection}
 * contract.
 *
 * @author Steve Ebersole
 */
public abstract class SelectionImpl<X>
		extends AbstractTupleElement<X>
		implements SelectionImplementor<X>, ParameterContainer, Serializable {
	public SelectionImpl(CriteriaBuilderImpl criteriaBuilder, Class<X> javaType) {
		super( criteriaBuilder, javaType );
	}

	public Selection<X> alias(String alias) {
		setAlias( alias );
		return this;
	}

	public boolean isCompoundSelection() {
		return false;
	}

	public List<ValueHandlerFactory.ValueHandler> getValueHandlers() {
		return getValueHandler() == null
				? null
				: Collections.singletonList( (ValueHandlerFactory.ValueHandler) getValueHandler() );
	}

	public List<Selection<?>> getCompoundSelectionItems() {
		throw new IllegalStateException( "Not a compound selection" );
	}
}
