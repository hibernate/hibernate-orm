/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.query.criteria.internal.path.RootImpl;

/**
 * Base class for commonality between {@link javax.persistence.criteria.CriteriaUpdate} and
 * {@link javax.persistence.criteria.CriteriaDelete}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractManipulationCriteriaQuery<T> implements CommonAbstractCriteria {
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

}
