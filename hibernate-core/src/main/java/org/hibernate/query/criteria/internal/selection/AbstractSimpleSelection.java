/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.criteria.internal.selection;

import java.io.Serializable;
import java.util.List;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaSelectionImplementor;
import org.hibernate.sqm.parser.criteria.tree.CriteriaVisitor;
import org.hibernate.sqm.parser.criteria.tree.select.JpaSelection;
import org.hibernate.sqm.query.expression.SqmExpression;
import org.hibernate.sqm.query.select.SqmAliasedExpressionContainer;

/**
 * The Hibernate implementation of the JPA {@link Selection}
 * contract.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSimpleSelection<X>
		extends AbstractSelection<X>
		implements JpaSelection<X>, Serializable {
	public AbstractSimpleSelection(HibernateCriteriaBuilder criteriaBuilder, Class<X> javaType) {
		super( criteriaBuilder, javaType );
	}

	public JpaSelectionImplementor<X> alias(String alias) {
		setAlias( alias );
		return this;
	}

	@Override
	public void visitSelections(CriteriaVisitor visitor, SqmAliasedExpressionContainer container) {
		container.add( visitExpression( visitor ), getAlias() );
	}

	protected abstract SqmExpression visitExpression(CriteriaVisitor visitor);

	public boolean isCompoundSelection() {
		return false;
	}

	public List<Selection<?>> getCompoundSelectionItems() {
		throw new IllegalStateException( "Not a compound selection" );
	}
}
