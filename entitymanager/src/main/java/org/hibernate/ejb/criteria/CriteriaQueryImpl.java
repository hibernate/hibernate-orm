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
package org.hibernate.ejb.criteria;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.Tuple;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.hibernate.ejb.HibernateEntityManagerImplementor;
import org.hibernate.type.Type;

/**
 * The Hibernate implementation of the JPA {@link CriteriaQuery} contract.  Mostly a set of delegation to its
 * internal {@link QueryStructure}.
 *
 * @author Steve Ebersole
 */
public class CriteriaQueryImpl<T> extends AbstractNode implements CriteriaQuery<T>, Serializable {
	private final Class<T> returnType;

	private final QueryStructure<T> queryStructure;
	private List<Order> orderSpecs = Collections.emptyList();


	public CriteriaQueryImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> returnType) {
		super( criteriaBuilder );
		this.returnType = returnType;
		this.queryStructure = new QueryStructure<T>( this, criteriaBuilder );
	}

	/**
	 * {@inheritDoc}
	 */
	public Class<T> getResultType() {
		return returnType;
	}


	// SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<T> distinct(boolean applyDistinction) {
		queryStructure.setDistinct( applyDistinction );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDistinct() {
		return queryStructure.isDistinct();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public Selection<T> getSelection() {
		return ( Selection<T> ) queryStructure.getSelection();
	}

	public void applySelection(Selection<? extends T> selection) {
		queryStructure.setSelection( selection );
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<T> select(Selection<? extends T> selection) {
		applySelection( selection );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public CriteriaQuery<T> multiselect(Selection<?>... selections) {
		return multiselect( Arrays.asList( selections ) );
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public CriteriaQuery<T> multiselect(List<Selection<?>> selections) {
		final Selection<? extends T> selection;

		if ( Tuple.class.isAssignableFrom( getResultType() ) ) {
			selection = ( Selection<? extends T> ) criteriaBuilder().tuple( selections );
		}
		else if ( getResultType().isArray() ) {
			selection = ( Selection<? extends T> )  criteriaBuilder().array(
					( Class<? extends Object[]> ) getResultType(),
					selections
			);
		}
		else if ( Object.class.equals( getResultType() ) ) {
			switch ( selections.size() ) {
				case 0: {
					throw new IllegalArgumentException(
							"empty selections passed to criteria query typed as Object"
					);
				}
				case 1: {
					selection = ( Selection<? extends T> ) selections.get( 0 );
					break;
				}
				default: {
					selection = ( Selection<? extends T> ) criteriaBuilder().array( selections );
				}
			}
		}
		else {
			selection = criteriaBuilder().construct( getResultType(), selections );
		}
		applySelection( selection );
		return this;
	}


	// ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public Set<Root<?>> getRoots() {
		return queryStructure.getRoots();
	}

	/**
	 * {@inheritDoc}
	 */
	public <X> Root<X> from(EntityType<X> entityType) {
		return queryStructure.from( entityType );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X> Root<X> from(Class<X> entityClass) {
		return queryStructure.from( entityClass );
	}


	// RESTRICTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public Predicate getRestriction() {
		return queryStructure.getRestriction();
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<T> where(Expression<Boolean> expression) {
		queryStructure.setRestriction( criteriaBuilder().wrap( expression ) );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<T> where(Predicate... predicates) {
		// TODO : assuming this should be a conjuntion, but the spec does not say specifically...
		queryStructure.setRestriction( criteriaBuilder().and( predicates ) );
		return this;
	}


	// GROUPING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public List<Expression<?>> getGroupList() {
		return queryStructure.getGroupings();
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<T> groupBy(Expression<?>... groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	public CriteriaQuery<T> groupBy(List<Expression<?>> groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate getGroupRestriction() {
		return queryStructure.getHaving();
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<T> having(Expression<Boolean> expression) {
		queryStructure.setHaving( criteriaBuilder().wrap( expression ) );
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<T> having(Predicate... predicates) {
		queryStructure.setHaving( criteriaBuilder().and( predicates ) );
		return this;
	}


	// ORDERING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public List<Order> getOrderList() {
		return orderSpecs;
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<T> orderBy(Order... orders) {
		if ( orders != null && orders.length > 0 ) {
			orderSpecs = Arrays.asList( orders );
		}
		else {
			orderSpecs = Collections.emptyList();
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<T> orderBy(List<Order> orders) {
		orderSpecs = orders;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<ParameterExpression<?>> getParameters() {
		return queryStructure.getParameters();
	}

	/**
	 * {@inheritDoc}
	 */
	public <U> Subquery<U> subquery(Class<U> subqueryType) {
		return queryStructure.subquery( subqueryType );
	}

	public void validate() {
		// getRoots() is explicitly supposed to return empty if none defined, no need to check for null
		if ( getRoots().isEmpty() ) {
			throw new IllegalStateException( "No criteria query roots were specified" );
		}

		// if there is not an explicit selection, there is an *implicit* selection of the root entity provided only
		// a single query root was defined.
		if ( getSelection() == null && !hasImplicitSelection() ) {
			throw new IllegalStateException( "No explicit selection and an implicit one cold not be determined" );
		}
	}

	/**
	 * If no explicit selection was defined, we have a condition called an implicit selection if the query specified
	 * a single {@link Root} and the java type of that {@link Root root's} model is the same as this criteria's
	 * {@link #getResultType() result type}.
	 *
	 * @return True if there is an explicit selection; false otherwise.
	 */
	private boolean hasImplicitSelection() {
		if ( getRoots().size() != 1 ) {
			return false;
		}

		Root root = getRoots().iterator().next();
		if ( root.getModel().getJavaType() != returnType ) {
			return false;
		}

		// if we get here, the query defined no selection but defined a single root of the same type as the
		// criteria query return, so we use that as the implicit selection
		//
		// todo : should we put an implicit marker in the selection to this fact to make later processing easier?
		return true;
	}

	public CriteriaQueryCompiler.RenderedCriteriaQuery render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		final StringBuilder jpaqlQuery = new StringBuilder();

		queryStructure.render( jpaqlQuery, renderingContext );

		if ( ! getOrderList().isEmpty() ) {
			jpaqlQuery.append( " order by " );
			String sep = "";
			for ( Order orderSpec : getOrderList() ) {
				jpaqlQuery.append( sep )
						.append( ( ( Renderable ) orderSpec.getExpression() ).render( renderingContext ) )
						.append( orderSpec.isAscending() ? " asc" : " desc" );
				sep = ", ";
			}
		}

		return new CriteriaQueryCompiler.RenderedCriteriaQuery() {
			public String getQueryString() {
				return jpaqlQuery.toString();
			}

			@SuppressWarnings({ "unchecked" })
			public List<ValueHandlerFactory.ValueHandler> getValueHandlers() {
				SelectionImplementor selection = (SelectionImplementor) queryStructure.getSelection();
				return selection == null
						? null
						: selection.getValueHandlers();
			}

			public HibernateEntityManagerImplementor.Options.ResultMetadataValidator getResultMetadataValidator() {
				return new HibernateEntityManagerImplementor.Options.ResultMetadataValidator() {
					public void validate(Type[] returnTypes) {
						SelectionImplementor selection = (SelectionImplementor) queryStructure.getSelection();
						if ( selection != null ) {
							if ( selection.isCompoundSelection() ) {
								if ( returnTypes.length != selection.getCompoundSelectionItems().size() ) {
									throw new IllegalStateException(
											"Number of return values [" + returnTypes.length +
													"] did not match expected [" +
													selection.getCompoundSelectionItems().size() + "]"
									);
								}
							}
							else {
								if ( returnTypes.length > 1 ) {
									throw new IllegalStateException(
											"Number of return values [" + returnTypes.length +
													"] did not match expected [1]"
									);
								}
							}
						}
					}
				};
			}
		};
	}
}
