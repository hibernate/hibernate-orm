/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.query.criteria.internal.compile.CompilableCriteria;
import org.hibernate.query.criteria.internal.compile.CriteriaInterpretation;
import org.hibernate.query.criteria.internal.compile.ImplicitParameterBinding;
import org.hibernate.query.criteria.internal.compile.InterpretedParameterMetadata;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.path.RootImpl;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.sql.ast.Clause;

/**
 * Base class for commonality between {@link javax.persistence.criteria.CriteriaUpdate} and
 * {@link javax.persistence.criteria.CriteriaDelete}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractManipulationCriteriaQuery<T> implements CompilableCriteria, CommonAbstractCriteria {
	private final CriteriaBuilderImpl criteriaBuilder;

	private RootImpl<T> root;
	private Predicate restriction;

	protected AbstractManipulationCriteriaQuery(CriteriaBuilderImpl criteriaBuilder) {
		this.criteriaBuilder = criteriaBuilder;
	}

	protected CriteriaBuilderImpl criteriaBuilder() {
		return criteriaBuilder;
	}


	// Root ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Root from(Class<T> entityClass) {
		EntityType<T> entityType = criteriaBuilder.getEntityManagerFactory()
				.getMetamodel()
				.entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( entityClass + " is not an entity" );
		}
		return from( entityType );
	}

	public Root<T> from(EntityType<T> entityType) {
		root = new RootImpl<T>( criteriaBuilder, entityType, false );
		return root;
	}

	public Root<T> getRoot() {
		return root;
	}


	// Restriction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected void setRestriction(Expression<Boolean> restriction) {
		this.restriction = criteriaBuilder.wrap( restriction );
	}

	public void setRestriction(Predicate... restrictions) {
		this.restriction = criteriaBuilder.and( restrictions );
	}

	public Predicate getRestriction() {
		return restriction;
	}

	public <U> Subquery<U> subquery(Class<U> type) {
		return new CriteriaSubqueryImpl<U>( criteriaBuilder(), type, this );
	}


	// compiling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void validate() {
		if ( root == null ) {
			throw new IllegalStateException( "UPDATE/DELETE criteria must name root entity" );
		}
	}

	@Override
	public CriteriaInterpretation interpret(RenderingContext renderingContext) {
		final String jpaqlString = renderQuery( renderingContext );
		return new CriteriaInterpretation() {
			@Override
			@SuppressWarnings("unchecked")
			public QueryImplementor buildCompiledQuery(
					SharedSessionContractImplementor entityManager,
					final InterpretedParameterMetadata interpretedParameterMetadata) {

				final Map<String,Class> implicitParameterTypes = extractTypeMap( interpretedParameterMetadata.implicitParameterBindings() );

				QueryImplementor query = entityManager.createQuery(
						jpaqlString,
						null,
						null,
						new HibernateEntityManagerImplementor.QueryOptions() {
							@Override
							public List<ValueHandlerFactory.ValueHandler> getValueHandlers() {
								return null;
							}

							@Override
							public Map<String, Class> getNamedParameterExplicitTypes() {
								return implicitParameterTypes;
							}

							@Override
							public ResultMetadataValidator getResultMetadataValidator() {
								return null;
							}
						}
				);

				for ( ImplicitParameterBinding implicitParameterBinding : interpretedParameterMetadata.implicitParameterBindings() ) {
					implicitParameterBinding.bind( query );
				}

				return query;
			}

			private Map<String, Class> extractTypeMap(List<ImplicitParameterBinding> implicitParameterBindings) {
				final HashMap<String,Class> map = new HashMap<>();
				for ( ImplicitParameterBinding implicitParameter : implicitParameterBindings ) {
					map.put( implicitParameter.getParameterName(), implicitParameter.getJavaType() );
				}
				return map;
			}
		};
	}

	protected abstract String renderQuery(RenderingContext renderingContext);

	protected void renderRoot(StringBuilder jpaql, RenderingContext renderingContext) {
		jpaql.append( ( (FromImplementor) root ).renderTableExpression( renderingContext ) );
	}

	protected void renderRestrictions(StringBuilder jpaql, RenderingContext renderingContext) {
		if ( getRestriction() == null ) {
			return;
		}

		renderingContext.getClauseStack().push( Clause.WHERE );
		try {
			jpaql.append( " where " )
					.append( ( (Renderable) getRestriction() ).render( renderingContext ) );
		}
		finally {
			renderingContext.getClauseStack().pop();
		}
	}
}
