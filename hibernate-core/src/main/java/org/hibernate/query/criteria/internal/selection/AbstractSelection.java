/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.internal.selection;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.internal.ParameterContainer;
import org.hibernate.query.criteria.JpaSelectionImplementor;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.query.criteria.internal.expression.AbstractTupleElement;

/**
 * The Hibernate implementation of the JPA {@link Selection}
 * contract.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSelection<X>
		extends AbstractTupleElement<X>
		implements JpaSelectionImplementor<X>, ParameterContainer, Serializable {
	public AbstractSelection(HibernateCriteriaBuilder criteriaBuilder, Class<X> javaType) {
		super( criteriaBuilder, javaType );
	}

	public Selection<X> alias(String alias) {
		setAlias( alias );
		return this;
	}

	public List<ValueHandlerFactory.ValueHandler> getValueHandlers() {
		return getValueHandler() == null
				? null
				: Collections.singletonList( (ValueHandlerFactory.ValueHandler) getValueHandler() );
	}
}
