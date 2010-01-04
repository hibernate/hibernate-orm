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
package org.hibernate.ejb.criteria.predicate;

import java.io.Serializable;
import java.util.List;
import java.util.Collections;
import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSimplePredicate
		extends AbstractPredicateImpl 
		implements Serializable {
	private static final List<Expression<Boolean>> NO_EXPRESSIONS = Collections.emptyList();

	public AbstractSimplePredicate(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder );
	}

	public BooleanOperator getOperator() {
		return BooleanOperator.AND;
	}

	public final List<Expression<Boolean>> getExpressions() {
		return NO_EXPRESSIONS;
	}

}
