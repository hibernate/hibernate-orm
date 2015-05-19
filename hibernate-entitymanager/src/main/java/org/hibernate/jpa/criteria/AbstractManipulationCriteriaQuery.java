/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.hibernate.jpa.criteria.compile.CompilableCriteria;
import org.hibernate.jpa.criteria.compile.CriteriaInterpretation;
import org.hibernate.jpa.criteria.compile.ImplicitParameterBinding;
import org.hibernate.jpa.criteria.compile.InterpretedParameterMetadata;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.path.RootImpl;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;

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
	private List<Subquery<?>> subQueries;

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
			public Query buildCompiledQuery(
					HibernateEntityManagerImplementor entityManager,
					final InterpretedParameterMetadata interpretedParameterMetadata) {

				final Map<String,Class> implicitParameterTypes = extractTypeMap( interpretedParameterMetadata.implicitParameterBindings() );

				QueryImpl jpaqlQuery = entityManager.createQuery(
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
					implicitParameterBinding.bind( jpaqlQuery );
				}

				return jpaqlQuery;
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

	protected abstract String renderQuery(RenderingContext renderingContext);

	protected void renderRoot(StringBuilder jpaql, RenderingContext renderingContext) {
		jpaql.append( ( (FromImplementor) root ).renderTableExpression( renderingContext ) );
	}

	protected void renderRestrictions(StringBuilder jpaql, RenderingContext renderingContext) {
		if ( getRestriction() != null) {
			jpaql.append( " where " )
					.append( ( (Renderable) getRestriction() ).render( renderingContext ) );
		}
	}
}
