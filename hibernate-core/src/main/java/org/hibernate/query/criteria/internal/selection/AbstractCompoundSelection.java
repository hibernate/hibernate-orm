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
import java.util.stream.Collectors;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.internal.expression.AbstractTupleElement;
import org.hibernate.query.criteria.spi.JpaCriteriaBuilderImplementor;
import org.hibernate.query.sqm.produce.spi.criteria.JpaExpression;
import org.hibernate.query.sqm.produce.spi.criteria.select.JpaCompoundSelection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCompoundSelection<X>
		extends AbstractTupleElement<X>
		implements JpaCompoundSelection<X>, Serializable {
	private List<JpaExpression<?>> expressions;

	public AbstractCompoundSelection(
			JpaCriteriaBuilderImplementor criteriaBuilder,
			JavaTypeDescriptor<X> javaType,
			List<JpaExpression<?>> expressions) {
		super( criteriaBuilder, javaType );
		this.expressions = expressions;
	}

	public List<JpaExpression<?>> getExpressions() {
		return expressions == null ? Collections.emptyList() : expressions;
	}

	public JpaCompoundSelection<X> alias(String alias) {
		setAlias( alias );
		return this;
	}

	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public List<Selection<?>> getCompoundSelectionItems() {
		return expressions == null ? Collections.emptyList() : expressions.stream().collect( Collectors.toList() );
	}
}
