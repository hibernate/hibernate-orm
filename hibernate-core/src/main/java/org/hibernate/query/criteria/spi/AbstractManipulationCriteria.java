/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaManipulationCriteria;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSubQuery;

/**
 * Base class for commonality between {@link javax.persistence.criteria.CriteriaUpdate} and
 * {@link javax.persistence.criteria.CriteriaDelete}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractManipulationCriteria<T>
		extends AbstractNode
		implements JpaManipulationCriteria {

	private RootImpl<T> root;
	private JpaPredicate restriction;
	private List<JpaSubQuery<?>> subQueries;

	protected AbstractManipulationCriteria(CriteriaNodeBuilder criteriaBuilder) {
		super(  criteriaBuilder );
	}


	// Root ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public RootImpl from(Class<T> entityClass) {
		final EntityTypeDescriptor<T> entityType = nodeBuilder().getSessionFactory()
				.getMetamodel()
				.entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( entityClass + " is not an entity" );
		}

		return from( entityType );
	}

	public RootImpl<T> from(EntityTypeDescriptor<T> entityType) {
		if ( root != null ) {
			// warn?
		}

		root = new RootImpl<>( entityType, nodeBuilder() );

		return root;
	}

	public RootImpl<T> getRoot() {
		return root;
	}


	// Restriction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected void setRestriction(JpaExpression<Boolean> restriction) {
		this.restriction = nodeBuilder().wrap( restriction );
	}

	public void setRestriction(JpaPredicate... restrictions) {
		this.restriction = nodeBuilder().and( restrictions );
	}

	@Override
	public JpaPredicate getRestriction() {
		return restriction;
	}

	@Override
	public <U> JpaSubQuery<U> subquery(Class<U> type) {
		throw new NotYetImplementedFor6Exception();
	}


	// compiling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void validate() {
		if ( root == null ) {
			throw new IllegalStateException( "UPDATE/DELETE criteria must name root entity" );
		}
	}

}
