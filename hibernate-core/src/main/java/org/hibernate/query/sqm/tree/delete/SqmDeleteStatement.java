/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.delete;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.criteria.JpaCriteriaDelete;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;

/**
 * @author Steve Ebersole
 */
public class SqmDeleteStatement<T>
		extends AbstractSqmDmlStatement<T>
		implements SqmDeleteOrUpdateStatement<T>, JpaCriteriaDelete<T> {
	private final SqmQuerySource querySource;

	private SqmWhereClause whereClause;

	public SqmDeleteStatement(SqmRoot<T> target, NodeBuilder nodeBuilder) {
		super( target, nodeBuilder );
		this.querySource = SqmQuerySource.HQL;

	}

	public SqmDeleteStatement(Class<T> targetEntity, NodeBuilder nodeBuilder) {
		super(
				new SqmRoot<>(
						nodeBuilder.getDomainModel().entity( targetEntity ),
						null,
						nodeBuilder
				),
				nodeBuilder
		);
		this.querySource = SqmQuerySource.CRITERIA;
	}

	@Override
	public SqmQuerySource getQuerySource() {
		return querySource;
	}

	@Override
	public SqmWhereClause getWhereClause() {
		return whereClause;
	}

	public void setWhereClause(SqmWhereClause whereClause) {
		this.whereClause = whereClause;
	}

	@Override
	public Root<T> from(Class<T> entityClass) {
		final EntityTypeDescriptor<T> entity = nodeBuilder().getDomainModel().entity( entityClass );
		SqmRoot<T> root = new SqmRoot<>( entity, null, nodeBuilder() );
		setTarget( root );
		return root;
	}

	@Override
	public Root<T> from(EntityType<T> entity) {
		SqmRoot<T> root = new SqmRoot<>( (EntityTypeDescriptor<T>) entity, null, nodeBuilder() );
		setTarget( root );
		return root;
	}

	@Override
	public Root<T> getRoot() {
		return getTarget();
	}

	@Override
	public SqmDeleteStatement<T> where(Expression<Boolean> restriction) {
		getWhereClause().setPredicate( (SqmPredicate) restriction );
		return this;
	}

	@Override
	public SqmDeleteStatement<T> where(Predicate... restrictions) {
		getWhereClause().setPredicate( null );
		for ( Predicate restriction : restrictions ) {
			getWhereClause().applyPredicate( (SqmPredicate) restriction );
		}
		return this;
	}

	@Override
	public JpaPredicate getRestriction() {
		return whereClause == null ? null : whereClause.getPredicate();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitDeleteStatement( this );
	}
}
