/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.update;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaCriteriaUpdate;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmRestrictedDmlStatement;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.sqm.internal.TypecheckUtil.assertAssignable;

/**
 * @author Steve Ebersole
 */
public class SqmUpdateStatement<T>
		extends AbstractSqmRestrictedDmlStatement<T>
		implements SqmDeleteOrUpdateStatement<T>, JpaCriteriaUpdate<T> {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SqmUpdateStatement.class );

	private boolean versioned;
	private SqmSetClause setClause;

	public SqmUpdateStatement(NodeBuilder nodeBuilder) {
		super( SqmQuerySource.HQL, nodeBuilder );
	}

	/**
	 * @deprecated was previously used for HQL. Use {@link SqmUpdateStatement#SqmUpdateStatement(NodeBuilder)} instead
	 */
	@Deprecated(forRemoval = true)
	public SqmUpdateStatement(SqmRoot<T> target, NodeBuilder nodeBuilder) {
		super( target, SqmQuerySource.HQL, nodeBuilder );
	}

	/**
	 * @deprecated was previously used for Criteria. Use {@link SqmUpdateStatement#SqmUpdateStatement(Class, SqmCriteriaNodeBuilder)} instead.
	 */
	@Deprecated(forRemoval = true)
	public SqmUpdateStatement(SqmRoot<T> target, SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( target, querySource, nodeBuilder );
	}

	public SqmUpdateStatement(Class<T> targetEntity, SqmCriteriaNodeBuilder nodeBuilder) {
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

	public SqmUpdateStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmRoot<T> target) {
		super( builder, querySource, parameters, cteStatements, target );
	}

	@Override
	public SqmUpdateStatement<T> copy(SqmCopyContext context) {
		final SqmUpdateStatement<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmUpdateStatement<T> statement = context.registerCopy(
				this,
				new SqmUpdateStatement<>(
						nodeBuilder(),
						context.getQuerySource() == null ? getQuerySource() : context.getQuerySource(),
						copyParameters( context ),
						copyCteStatements( context ),
						getTarget().copy( context )
				)
		);
		statement.setWhereClause( copyWhereClause( context ) );
		statement.versioned = versioned;
		if ( setClause != null ) {
			statement.setClause = setClause.copy( context );
		}
		return statement;
	}

	@Override
	public void validate(@Nullable String hql) {
		verifyImmutableEntityUpdate( hql );
		if ( getSetClause() == null || getSetClause().getAssignments().isEmpty() ) {
			throw new IllegalArgumentException( "No assignments specified as part of UPDATE criteria" );
		}
		verifyUpdateTypesMatch();
	}

	private void verifyImmutableEntityUpdate(String hql) {
		final EntityPersister persister =
				nodeBuilder().getMappingMetamodel().getEntityDescriptor( getTarget().getEntityName() );
		if ( !persister.isMutable() ) {
			final String querySpaces = Arrays.toString( persister.getQuerySpaces() );
			switch ( nodeBuilder().getImmutableEntityUpdateQueryHandlingMode() ) {
				case ALLOW :
					LOG.immutableEntityUpdateQueryAllowed( hql, querySpaces );
					break;
				case WARNING:
					LOG.immutableEntityUpdateQuery( hql, querySpaces );
					break;
				case EXCEPTION:
					throw new HibernateException( "The query attempts to update an immutable entity: "
												+ querySpaces
												+ " (set '"
												+ AvailableSettings.IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE
												+ "' to suppress)");
			}
		}
	}

	private void verifyUpdateTypesMatch() {
		final List<SqmAssignment<?>> assignments = getSetClause().getAssignments();
		for ( int i = 0; i < assignments.size(); i++ ) {
			final SqmAssignment<?> assignment = assignments.get( i );
			final SqmPath<?> targetPath = assignment.getTargetPath();
			final SqmExpression<?> expression = assignment.getValue();
			assertAssignable( null, targetPath, expression, nodeBuilder() );
		}
	}

	public SqmSetClause getSetClause() {
		return setClause;
	}

	public void setSetClause(SqmSetClause setClause) {
		this.setClause = setClause;
	}

	@Override
	public <Y, X extends Y> SqmUpdateStatement<T> set(SingularAttribute<? super T, Y> attribute, X value) {
		applyAssignment( getTarget().get( attribute ), (SqmExpression<? extends Y>) nodeBuilder().value( value ) );
		return this;
	}

	@Override
	public <Y> SqmUpdateStatement<T> set(SingularAttribute<? super T, Y> attribute, Expression<? extends Y> value) {
		applyAssignment( getTarget().get( attribute ), (SqmExpression<? extends Y>) value );
		return this;
	}

	@Override
	public <Y, X extends Y> SqmUpdateStatement<T> set(Path<Y> attribute, X value) {
		final SqmCriteriaNodeBuilder nodeBuilder = (SqmCriteriaNodeBuilder) nodeBuilder();
		final SqmPath<Y> sqmAttribute = (SqmPath<Y>) attribute;
		applyAssignment( sqmAttribute, nodeBuilder.value( value, sqmAttribute ) );
		return this;
	}

	@Override
	public <Y> SqmUpdateStatement<T> set(Path<Y> attribute, Expression<? extends Y> value) {
		applyAssignment( (SqmPath<Y>) attribute, (SqmExpression<? extends Y>) value );
		return this;
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public SqmUpdateStatement<T> set(String attributeName, Object value) {
		final SqmPath sqmPath = getTarget().get( attributeName );
		final SqmExpression expression;
		if ( value instanceof SqmExpression ) {
			expression = (SqmExpression) value;
		}
		else {
			final SqmCriteriaNodeBuilder nodeBuilder = (SqmCriteriaNodeBuilder) nodeBuilder();
			expression = nodeBuilder.value( value, sqmPath );
		}
		applyAssignment( sqmPath, expression );
		return this;
	}

	@Override
	public boolean isVersioned() {
		return versioned;
	}

	@Override
	public SqmUpdateStatement<T> versioned() {
		this.versioned = true;
		return this;
	}

	@Override
	public SqmUpdateStatement<T> versioned(boolean versioned) {
		this.versioned = versioned;
		return this;
	}

	@Override
	public void setTarget(JpaRoot<T> root) {
		if ( root.getModel() instanceof SqmPolymorphicRootDescriptor<?> ) {
			throw new SemanticException(
					String.format(
							"Target type '%s' is not an entity",
							root.getModel().getHibernateEntityName()
					)
			);
		}
		super.setTarget( root );
	}

	@Override
	public SqmUpdateStatement<T> where(Expression<Boolean> restriction) {
		setWhere( restriction );
		return this;
	}

	@Override
	public SqmUpdateStatement<T> where(Predicate... restrictions) {
		setWhere( restrictions );
		return this;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitUpdateStatement( this );
	}

	@Override
	public <U> SqmSubQuery<U> subquery(EntityType<U> type) {
		return new SqmSubQuery<>( this, type, nodeBuilder() );
	}

	public <Y> void applyAssignment(SqmPath<Y> targetPath, SqmExpression<? extends Y> value) {
		applyAssignment( new SqmAssignment<>( targetPath, value ) );
	}

	public <Y> void applyAssignment(SqmAssignment<Y> assignment) {
		if ( setClause == null ) {
			setClause = new SqmSetClause();
		}
		setClause.addAssignment( assignment );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		appendHqlCteString( hql, context );
		hql.append( "update " );
		if ( versioned ) {
			hql.append( "versioned " );
		}
		final SqmRoot<T> root = getTarget();
		hql.append( root.getEntityName() );
		hql.append( ' ' ).append( root.resolveAlias( context ) );
		SqmFromClause.appendJoins( root, hql, context );
		SqmFromClause.appendTreatJoins( root, hql, context );
		setClause.appendHqlString( hql, context );
		super.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(Object node) {
		return node instanceof SqmUpdateStatement<?> that
			&& super.equals( node )
			&& this.versioned == that.versioned
			&& Objects.equals( this.setClause, that.setClause )
			&& Objects.equals( this.getTarget(), that.getTarget() )
			&& Objects.equals( this.getWhereClause(), that.getWhereClause() )
			&& Objects.equals( this.getCteStatements(), that.getCteStatements() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( versioned, setClause, getTarget(), getWhereClause(), getCteStatements() );
	}
}
