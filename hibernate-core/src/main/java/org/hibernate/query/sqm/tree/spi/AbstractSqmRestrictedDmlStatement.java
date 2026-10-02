package org.hibernate.query.sqm.tree.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.criteria.BooleanExpression;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCriteriaBase;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmQuerySource;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmWhereClause;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.EntityType;

/**
 * @author Christian Beikov
 */
public abstract class AbstractSqmRestrictedDmlStatement<T> extends AbstractSqmDmlStatement<T>
		implements JpaCriteriaBase {

	protected @Nullable SqmWhereClause whereClause;

	/**
	 * Constructor for HQL statements.
	 */
	public AbstractSqmRestrictedDmlStatement(@Nonnull SqmQuerySource querySource, @Nonnull NodeBuilder nodeBuilder) {
		super( querySource, nodeBuilder );
	}

	/**
	 * Constructor for Criteria statements.
	 */
	public AbstractSqmRestrictedDmlStatement(@Nonnull SqmRoot<T> target, @Nonnull SqmQuerySource querySource, @Nonnull NodeBuilder nodeBuilder) {
		super( target, querySource, nodeBuilder );
	}

	protected AbstractSqmRestrictedDmlStatement(
			@Nonnull NodeBuilder builder,
			@Nonnull SqmQuerySource querySource,
			@Nullable Set<SqmParameter<?>> parameters,
			@Nonnull Map<String, SqmCteStatement<?>> cteStatements,
			@Nonnull SqmRoot<T> target) {
		super( builder, querySource, parameters, cteStatements, target );
	}

	protected @Nullable SqmWhereClause copyWhereClause(@Nonnull SqmCopyContext context) {
		if ( whereClause == null ) {
			return null;
		}
		else {
			final var predicate = whereClause.getPredicate();
			return new SqmWhereClause( predicate == null ? null : predicate.copy( context ), nodeBuilder() );
		}
	}

	public @Nonnull SqmRoot<T> from(@Nonnull Class<T> entityClass) {
		return from( nodeBuilder().getDomainModel().entity( entityClass ) );
	}

	public @Nonnull SqmRoot<T> from(@Nonnull EntityType<T> entity) {
		final var entityDomainType = (EntityDomainType<T>) entity;
		final var root = getTarget();
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

	public @Nonnull SqmRoot<T> getRoot() {
		return getTarget();
	}

	public @Nullable SqmWhereClause getWhereClause() {
		return whereClause;
	}

	public void applyPredicate(@Nullable SqmPredicate predicate) {
		if ( predicate != null ) {
			initAndGetWhereClause().applyPredicate( predicate );
		}
	}

	public void setWhereClause(@Nullable SqmWhereClause whereClause) {
		this.whereClause = whereClause;
	}

	@Nullable
	@Override
	public JpaPredicate getRestriction() {
		return whereClause == null ? null : whereClause.getPredicate();
	}

	protected void setWhere(@Nullable Expression<Boolean> restriction) {
		// Replaces the current predicate if one is present
		initAndGetWhereClause().setPredicate( (SqmPredicate) restriction );
	}



	@Nonnull
	protected SqmWhereClause initAndGetWhereClause() {
		if ( whereClause == null ) {
			whereClause = new SqmWhereClause( nodeBuilder() );
		}
		return whereClause;
	}

	protected void setWhere(@Nullable Predicate ... restrictions) {
		final SqmWhereClause whereClause = initAndGetWhereClause();
		// Clear the current predicate if one is present
		whereClause.setPredicate( null );
		if ( restrictions != null ) {
			for ( var restriction : restrictions ) {
				whereClause.applyPredicate( (SqmPredicate) restriction );
			}
		}
	}

	protected void setWhere(@Nullable BooleanExpression... restrictions) {
		final SqmWhereClause whereClause = initAndGetWhereClause();
		// Clear the current predicate if one is present
		whereClause.setPredicate( null );
		if ( restrictions != null ) {
			for ( var restriction : restrictions ) {
				whereClause.applyPredicate( nodeBuilder().wrap( restriction ) );
			}
		}
	}

	protected void setWhere(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		final SqmWhereClause whereClause = initAndGetWhereClause();
		// Clear the current predicate if one is present
		whereClause.setPredicate( null );
		for ( var restriction : restrictions ) {
			whereClause.applyPredicate( nodeBuilder().wrap( restriction ) );
		}
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		if ( whereClause != null ) {
			final var predicate = whereClause.getPredicate();
			if ( predicate != null ) {
				hql.append( " where " );
				predicate.appendHqlString( hql, context );
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof AbstractSqmRestrictedDmlStatement<?> that
			&& super.equals( object )
			&& Objects.equals( getWhereClause(), that.getWhereClause() );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Objects.hashCode( getWhereClause() );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof AbstractSqmRestrictedDmlStatement<?> that
			&& super.isCompatible( object )
			&& SqmCacheable.areCompatible( getWhereClause(), that.getWhereClause() );
	}

	@Override
	public int cacheHashCode() {
		int result = super.cacheHashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( getWhereClause() );
		return result;
	}
}
