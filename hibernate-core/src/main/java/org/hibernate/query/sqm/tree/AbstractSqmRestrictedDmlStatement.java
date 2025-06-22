/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import java.util.Map;
import java.util.Set;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCriteriaBase;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.EntityType;

/**
 * @author Christian Beikov
 */
public abstract class AbstractSqmRestrictedDmlStatement<T> extends AbstractSqmDmlStatement<T>
		implements JpaCriteriaBase {

	private SqmWhereClause whereClause;

	/**
	 * Constructor for HQL statements.
	 */
	public AbstractSqmRestrictedDmlStatement(SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( querySource, nodeBuilder );
	}

	/**
	 * Constructor for Criteria statements.
	 */
	public AbstractSqmRestrictedDmlStatement(SqmRoot<T> target, SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( target, querySource, nodeBuilder );
	}

	protected AbstractSqmRestrictedDmlStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmRoot<T> target) {
		super( builder, querySource, parameters, cteStatements, target );
	}

	protected SqmWhereClause copyWhereClause(SqmCopyContext context) {
		if ( getWhereClause() == null ) {
			return null;
		}
		else {
			final SqmWhereClause whereClause = new SqmWhereClause( nodeBuilder() );
			final SqmPredicate predicate = getWhereClause().getPredicate();
			whereClause.setPredicate( predicate==null ? null : predicate.copy( context ) );
			return whereClause;
		}
	}

	public SqmRoot<T> from(Class<T> entityClass) {
		return from( nodeBuilder().getDomainModel().entity( entityClass ) );
	}

	public SqmRoot<T> from(EntityType<T> entity) {
		final EntityDomainType<T> entityDomainType = (EntityDomainType<T>) entity;
		final SqmRoot<T> root = getTarget();
		if ( root.getModel() != entity ) {
			throw new IllegalArgumentException(
					String.format(
							"Expecting DML target entity type [%s] but got [%s]",
							root.getModel().getHibernateEntityName(),
							entityDomainType.getName()
					)
			);
		}
		return root;
	}

	public SqmRoot<T> getRoot() {
		return getTarget();
	}

	public SqmWhereClause getWhereClause() {
		return whereClause;
	}

	public void applyPredicate(SqmPredicate predicate) {
		if ( predicate != null ) {
			initAndGetWhereClause().applyPredicate( predicate );
		}
	}

	public void setWhereClause(SqmWhereClause whereClause) {
		this.whereClause = whereClause;
	}

	@Override
	public JpaPredicate getRestriction() {
		return whereClause == null ? null : whereClause.getPredicate();
	}

	protected void setWhere(Expression<Boolean> restriction) {
		// Replaces the current predicate if one is present
		initAndGetWhereClause().setPredicate( (SqmPredicate) restriction );
	}

	protected SqmWhereClause initAndGetWhereClause() {
		if ( whereClause == null ) {
			whereClause = new SqmWhereClause( nodeBuilder() );
		}
		return whereClause;
	}

	protected void setWhere(Predicate... restrictions) {
		final SqmWhereClause whereClause = initAndGetWhereClause();
		// Clear the current predicate if one is present
		whereClause.setPredicate(null);
		for ( Predicate restriction : restrictions ) {
			whereClause.applyPredicate( (SqmPredicate) restriction );
		}
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		if ( whereClause != null && whereClause.getPredicate() != null ) {
			hql.append( " where " );
			whereClause.getPredicate().appendHqlString( hql, context );
		}
	}
}
