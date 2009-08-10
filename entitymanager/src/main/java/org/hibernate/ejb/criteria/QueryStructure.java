/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.LinkedHashSet;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

/**
 * Models basic query structure.  Used as a delegate in implementing both
 * {@link org.hibernate.criterion.CriteriaQuery} and
 * {@link javax.persistence.criteria.Subquery}.
 * <p/>
 * Note the <tt>ORDER BY</tt> specs are neglected here.  That's because it is not valid
 * for a subquery to define an <tt>ORDER BY</tt> clause.  So we just handle them on the
 * root query directly...
 *
 * @author Steve Ebersole
 */
public class QueryStructure<T> {
	private final AbstractQuery<T> owner;
	private final QueryBuilderImpl queryBuilder;

	public QueryStructure(AbstractQuery<T> owner, QueryBuilderImpl queryBuilder) {
		this.owner = owner;
		this.queryBuilder = queryBuilder;
	}

	private boolean distinction;
	private Selection<? extends T> selection;
	private Set<Root<?>> roots = new HashSet<Root<?>>();
	private Predicate restriction;
	private List<Expression<?>> groupings = Collections.emptyList();
	private Predicate having;
	private List<Subquery<?>> subqueries;


	// PARAMETERS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<ParameterExpression<?>> getParameters() {
		final Set<ParameterExpression<?>> parameters = new LinkedHashSet<ParameterExpression<?>>();
		final ParameterRegistry registry = new ParameterRegistry() {
			public void registerParameter(ParameterExpression<?> parameter) {
				parameters.add( parameter );
			}
		};

		ParameterContainer.Helper.possibleParameter(selection, registry);
		ParameterContainer.Helper.possibleParameter(restriction, registry);
		ParameterContainer.Helper.possibleParameter(having, registry);
		for ( Subquery subquery : getSubqueries() ) {
			ParameterContainer.Helper.possibleParameter(subquery, registry);
		}

		return parameters;
	}


	// SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean isDistinction() {
		return distinction;
	}

	public void setDistinction(boolean distinction) {
		this.distinction = distinction;
	}

	public Selection<? extends T> getSelection() {
		return selection;
	}

	public void setSelection(Selection<? extends T> selection) {
		this.selection = selection;
	}


	// ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<Root<?>> getRoots() {
		return roots;
	}

	public <X> Root<X> from(Class<X> entityClass) {
		EntityType<X> entityType = queryBuilder.getEntityManagerFactory()
				.getMetamodel()
				.entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( entityClass + " is not an entity" );
		}
		return from( entityType );
	}

	public <X> Root<X> from(EntityType<X> entityType) {
		RootImpl<X> root = new RootImpl( queryBuilder, entityType );
		roots.add( root );
		return root;
	}


	// RESTRICTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Predicate getRestriction() {
		return restriction;
	}

	public void setRestriction(Predicate restriction) {
		this.restriction = restriction;
	}


	// GROUPINGS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Expression<?>> getGroupings() {
		return groupings;
	}

	public void setGroupings(List<Expression<?>> groupings) {
		this.groupings = groupings;
	}

	public void setGroupings(Expression<?>... groupings) {
		if ( groupings != null && groupings.length > 0 ) {
			this.groupings = Arrays.asList( groupings );
		}
		else {
			this.groupings = Collections.emptyList();
		}
	}

	public Predicate getHaving() {
		return having;
	}

	public void setHaving(Predicate having) {
		this.having = having;
	}


	// SUBQUERIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Subquery<?>> getSubqueries() {
		return subqueries;
	}

	public <U> Subquery<U> subquery(Class<U> subqueryType) {
		CriteriaSubqueryImpl<U> subquery = new CriteriaSubqueryImpl<U>( queryBuilder, subqueryType, owner );
		subqueries.add( subquery );
		return subquery;
	}
}
