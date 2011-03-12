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
import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.Renderable;

/**
 * ANSI-SQL defines <tt>TRUE</tt>, <tt>FALSE</tt> and <tt>UNKNOWN</tt> as <i>truth values</i>.  These
 * <i>truth values</i> are used to explicitly check the result of a boolean expression (the syntax is like
 * <tt>a > b IS TRUE</tt>.  <tt>IS TRUE</tt> is the assumed default.
 * <p/>
 * JPA defines support for only <tt>IS TRUE</tt> and <tt>IS FALSE</tt>, not <tt>IS UNKNOWN</tt> (<tt>a > NULL</tt>
 * is an example where the result would be UNKNOWN.
 *
 * @author Steve Ebersole
 */
public class ExplicitTruthValueCheck
		extends AbstractSimplePredicate
		implements Serializable {
	// TODO : given that JPA supports only TRUE and FALSE, can this be handled just with negation?
	private final Expression<Boolean> booleanExpression;
	private final TruthValue truthValue;

	public ExplicitTruthValueCheck(CriteriaBuilderImpl criteriaBuilder, Expression<Boolean> booleanExpression, TruthValue truthValue) {
		super( criteriaBuilder );
		this.booleanExpression = booleanExpression;
		this.truthValue = truthValue;
	}

	public Expression<Boolean> getBooleanExpression() {
		return booleanExpression;
	}

	public TruthValue getTruthValue() {
		return truthValue;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getBooleanExpression(), registry );
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return ( (Renderable) getBooleanExpression() ).render( renderingContext )
				+ " = "
				+ ( getTruthValue() == TruthValue.TRUE ? "true" : "false" );
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}

