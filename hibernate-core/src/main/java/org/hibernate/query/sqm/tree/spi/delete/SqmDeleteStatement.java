package org.hibernate.query.sqm.tree.spi.delete;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.query.criteria.JpaCriteriaDelete;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.internal.SimpleSqmCopyContext;
import org.hibernate.query.sqm.tree.spi.AbstractSqmRestrictedDmlStatement;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;
import org.hibernate.query.sqm.tree.spi.from.SqmFromClause;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.query.sqm.spi.SqmQuerySource.CRITERIA;

/**
 * @author Steve Ebersole
 */
public class SqmDeleteStatement<T>
		extends AbstractSqmRestrictedDmlStatement<T>
		implements SqmDeleteOrUpdateStatement<T>, JpaCriteriaDelete<T> {

	public SqmDeleteStatement(@Nonnull NodeBuilder nodeBuilder) {
		super( SqmQuerySource.HQL, nodeBuilder );
	}

	public SqmDeleteStatement(@Nonnull Class<T> targetEntity, @Nonnull NodeBuilder nodeBuilder) {
		super(
				new SqmRoot<>(
						nodeBuilder.getDomainModel().entity( targetEntity ),
						"_0",
						!nodeBuilder.isJpaQueryComplianceEnabled(),
						nodeBuilder
				),
				SqmQuerySource.CRITERIA,
				nodeBuilder
		);
	}

	public SqmDeleteStatement(
			@Nonnull NodeBuilder builder,
			@Nonnull SqmQuerySource querySource,
			@Nullable Set<SqmParameter<?>> parameters,
			@Nonnull Map<String, SqmCteStatement<?>> cteStatements,
			@Nonnull SqmRoot<T> target) {
		super( builder, querySource, parameters, cteStatements, target );
	}

	/**
	 * @implNote This form is used when transforming HQL to criteria.
	 *           All it does is change the SqmQuerySource to CRITERIA
	 *           in order to allow correct parameter handing.
	 */
	public SqmDeleteStatement(@Nonnull SqmDeleteStatement<?> original) {
		super(
				original.nodeBuilder(),
				CRITERIA,
				null,
				original.copyCteStatements( new SimpleSqmCopyContext() ),
				(SqmRoot<T>) original.getTarget()
		);
		whereClause = original.copyWhereClause( new SimpleSqmCopyContext() );
	}

	@Nonnull
	@Override
	public SqmDeleteStatement<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var newQuerySource = context.getQuerySource();
		final SqmDeleteStatement<T> statement = context.registerCopy(
				this,
				new SqmDeleteStatement<>(
						nodeBuilder(),
						newQuerySource == null ? getQuerySource() : newQuerySource,
						copyParameters( context ),
						copyCteStatements( context ),
						getTarget().copy( context )
				)
		);
		statement.setWhereClause( copyWhereClause( context ) );
		return statement;
	}

	@Override
	public void validate(@Nullable String hql) {
		if ( getQuerySource() == SqmQuerySource.CRITERIA ) {
			SqmUtil.validateCriteriaTree( this );
		}
	}

	@Nonnull
	@Override
	public SqmDeleteStatement<T> where(@Nonnull Expression<Boolean> restriction) {
		setWhere( restriction );
		return this;
	}

	@Nonnull
	@Override
	public SqmDeleteStatement<T> where(@Nonnull BooleanExpression... restrictions) {
		setWhere( restrictions );
		return this;
	}

	@Nonnull
	@Override
	public SqmDeleteStatement<T> where(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		setWhere( restrictions );
		return this;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitDeleteStatement( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		appendHqlCteString( hql, context );
		hql.append( "delete from " );
		final SqmRoot<T> root = getTarget();
		hql.append( root.getEntityName() );
		hql.append( ' ' ).append( root.resolveAlias( context ) );
		SqmFromClause.appendJoins( root, hql, context );
		SqmFromClause.appendTreatJoins( root, hql, context );
		super.appendHqlString( hql, context );
	}

	@Nonnull
	@Override
	public <U> Subquery<U> subquery(@Nonnull EntityType<U> type) {
		throw new UnsupportedOperationException( "DELETE query cannot be sub-query" );
	}
}
