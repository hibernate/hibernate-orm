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
package org.hibernate.jpa.criteria;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.hibernate.jpa.criteria.compile.CompilableCriteria;
import org.hibernate.jpa.criteria.compile.CriteriaInterpretation;
import org.hibernate.jpa.criteria.compile.CriteriaQueryTypeQueryAdapter;
import org.hibernate.jpa.criteria.compile.ImplicitParameterBinding;
import org.hibernate.jpa.criteria.compile.InterpretedParameterMetadata;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * The Hibernate implementation of the JPA {@link CriteriaQuery} contract.  Mostly a set of delegation to its
 * internal {@link QueryStructure}.
 *
 * @author Steve Ebersole
 */
public class CriteriaQueryImpl<T> extends AbstractNode implements CriteriaQuery<T>, CompilableCriteria, Serializable {
	private static final Logger log = Logger.getLogger( CriteriaQueryImpl.class );

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

	@Override
	public Class<T> getResultType() {
		return returnType;
	}


	// SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public CriteriaQuery<T> distinct(boolean applyDistinction) {
		queryStructure.setDistinct( applyDistinction );
		return this;
	}

	@Override
	public boolean isDistinct() {
		return queryStructure.isDistinct();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Selection<T> getSelection() {
		return ( Selection<T> ) queryStructure.getSelection();
	}

	public void applySelection(Selection<? extends T> selection) {
		queryStructure.setSelection( selection );
	}

	@Override
	public CriteriaQuery<T> select(Selection<? extends T> selection) {
		applySelection( selection );
		return this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public CriteriaQuery<T> multiselect(Selection<?>... selections) {
		return multiselect( Arrays.asList( selections ) );
	}

	@Override
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

	@Override
	public Set<Root<?>> getRoots() {
		return queryStructure.getRoots();
	}

	@Override
	public <X> Root<X> from(EntityType<X> entityType) {
		return queryStructure.from( entityType );
	}

	@Override
	public <X> Root<X> from(Class<X> entityClass) {
		return queryStructure.from( entityClass );
	}


	// RESTRICTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Predicate getRestriction() {
		return queryStructure.getRestriction();
	}

	@Override
	public CriteriaQuery<T> where(Expression<Boolean> expression) {
		queryStructure.setRestriction( criteriaBuilder().wrap( expression ) );
		return this;
	}

	@Override
	public CriteriaQuery<T> where(Predicate... predicates) {
		// TODO : assuming this should be a conjuntion, but the spec does not say specifically...
		queryStructure.setRestriction( criteriaBuilder().and( predicates ) );
		return this;
	}


	// GROUPING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public List<Expression<?>> getGroupList() {
		return queryStructure.getGroupings();
	}

	@Override
	public CriteriaQuery<T> groupBy(Expression<?>... groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	@Override
	public CriteriaQuery<T> groupBy(List<Expression<?>> groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	@Override
	public Predicate getGroupRestriction() {
		return queryStructure.getHaving();
	}

	@Override
	public CriteriaQuery<T> having(Expression<Boolean> expression) {
		queryStructure.setHaving( criteriaBuilder().wrap( expression ) );
		return this;
	}

	@Override
	public CriteriaQuery<T> having(Predicate... predicates) {
		queryStructure.setHaving( criteriaBuilder().and( predicates ) );
		return this;
	}


	// ORDERING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public List<Order> getOrderList() {
		return orderSpecs;
	}

	@Override
	public CriteriaQuery<T> orderBy(Order... orders) {
		if ( orders != null && orders.length > 0 ) {
			orderSpecs = Arrays.asList( orders );
		}
		else {
			orderSpecs = Collections.emptyList();
		}
		return this;
	}

	@Override
	public CriteriaQuery<T> orderBy(List<Order> orders) {
		orderSpecs = orders;
		return this;
	}

	@Override
	public Set<ParameterExpression<?>> getParameters() {
		return queryStructure.getParameters();
	}

	@Override
	public <U> Subquery<U> subquery(Class<U> subqueryType) {
		return queryStructure.subquery( subqueryType );
	}

	@Override
	public void validate() {
		// getRoots() is explicitly supposed to return empty if none defined, no need to check for null
		if ( getRoots().isEmpty() ) {
			throw new IllegalStateException( "No criteria query roots were specified" );
		}

		// if there is not an explicit selection, there is an *implicit* selection of the root entity provided only
		// a single query root was defined.
		if ( getSelection() == null && !hasImplicitSelection() ) {
			throw new IllegalStateException( "No explicit selection and an implicit one could not be determined" );
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
        Class<?> javaType = root.getModel().getJavaType();
        if ( javaType != null && javaType != returnType ) {
			return false;
		}

		// if we get here, the query defined no selection but defined a single root of the same type as the
		// criteria query return, so we use that as the implicit selection
		//
		// todo : should we put an implicit marker in the selection to this fact to make later processing easier?
		return true;
	}

	@Override
	public CriteriaInterpretation interpret(RenderingContext renderingContext) {
		final StringBuilder jpaqlBuffer = new StringBuilder();

		queryStructure.render( jpaqlBuffer, renderingContext );

		if ( ! getOrderList().isEmpty() ) {
			jpaqlBuffer.append( " order by " );
			String sep = "";
			for ( Order orderSpec : getOrderList() ) {
				jpaqlBuffer.append( sep )
						.append( ( ( Renderable ) orderSpec.getExpression() ).render( renderingContext ) )
						.append( orderSpec.isAscending() ? " asc" : " desc" );
				sep = ", ";
			}
		}

		final String jpaqlString = jpaqlBuffer.toString();

		log.debugf( "Rendered criteria query -> %s", jpaqlString );

		return new CriteriaInterpretation() {
			@Override
			@SuppressWarnings("unchecked")
			public Query buildCompiledQuery(HibernateEntityManagerImplementor entityManager, final InterpretedParameterMetadata parameterMetadata) {

				final Map<String,Class> implicitParameterTypes = extractTypeMap( parameterMetadata.implicitParameterBindings() );

				QueryImpl jpaqlQuery = entityManager.createQuery(
						jpaqlString,
						getResultType(),
						getSelection(),
						new HibernateEntityManagerImplementor.QueryOptions() {
							@Override
							public List<ValueHandlerFactory.ValueHandler> getValueHandlers() {
								SelectionImplementor selection = (SelectionImplementor) queryStructure.getSelection();
								return selection == null
										? null
										: selection.getValueHandlers();
							}

							@Override
							public Map<String, Class> getNamedParameterExplicitTypes() {
								return implicitParameterTypes;
							}

							@Override
							public ResultMetadataValidator getResultMetadataValidator() {
								return new HibernateEntityManagerImplementor.QueryOptions.ResultMetadataValidator() {
									@Override
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
						}
				);

				for ( ImplicitParameterBinding implicitParameterBinding : parameterMetadata.implicitParameterBindings() ) {
					implicitParameterBinding.bind( jpaqlQuery );
				}

				return new CriteriaQueryTypeQueryAdapter(
						entityManager,
						jpaqlQuery,
						parameterMetadata.explicitParameterInfoMap()
				);
			}

			private Map<String, Class> extractTypeMap(List<ImplicitParameterBinding> implicitParameterBindings) {
				final HashMap<String,Class> map = new HashMap<String, Class>();
				for ( ImplicitParameterBinding implicitParameter : implicitParameterBindings ) {
					map.put( implicitParameter.getParameterName(), implicitParameter.getJavaType() );
				}
				return map;
			}
		};
	}
}
