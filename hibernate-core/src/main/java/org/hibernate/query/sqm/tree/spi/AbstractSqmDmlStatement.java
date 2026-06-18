/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.AbstractQuery;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmQuerySource;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.spi.select.SqmSubQuery;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static java.lang.Character.isAlphabetic;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.acquireUniqueAlias;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmDmlStatement<E>
		extends AbstractSqmStatement<E>
		implements SqmDmlStatement<E> {
	private final Map<String, SqmCteStatement<?>> cteStatements;
	private SqmRoot<E> target;

	public AbstractSqmDmlStatement(SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( querySource, nodeBuilder );
		this.cteStatements = new LinkedHashMap<>();
	}

	public AbstractSqmDmlStatement(SqmRoot<E> target, SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		this( querySource, nodeBuilder );
		this.target = target;
	}

	public AbstractSqmDmlStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			@Nullable Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmRoot<E> target) {
		super( builder, querySource, parameters );
		this.cteStatements = cteStatements;
		this.target = target;
	}

	protected Map<String, SqmCteStatement<?>> copyCteStatements(SqmCopyContext context) {
		final Map<String, SqmCteStatement<?>> copy =
				new LinkedHashMap<>( cteStatements.size() );
		for ( var entry : cteStatements.entrySet() ) {
			copy.put( entry.getKey(), entry.getValue().copy( context ) );
		}
		return copy;
	}

	protected void putAllCtes(SqmCteContainer cteContainer) {
		for ( var cteStatement : cteContainer.getCteStatements() ) {
			final String cteName = cteStatement.getCteTable().getCteName();
			if ( cteStatements.putIfAbsent( cteName, cteStatement ) != null ) {
				throw new IllegalArgumentException( "A CTE with the label " + cteName + " already exists" );
			}
		}
	}

	public abstract void validate(@Nullable String hql);

	@Override
	@Nonnull
	public Collection<SqmCteStatement<?>> getCteStatements() {
		return cteStatements.values();
	}

	@Override
	@Nullable public SqmCteStatement<?> getCteStatement(String cteLabel) {
		return cteStatements.get( cteLabel );
	}

	@Override
	@Nonnull
	public Collection<? extends JpaCteCriteria<?>> getCteCriterias() {
		return cteStatements.values();
	}

	@Override
	@Nullable
	public <X> JpaCteCriteria<X> getCteCriteria(@Nonnull String cteName) {
		return (JpaCteCriteria<X>) cteStatements.get( cteName );
	}

	@Override
	@Deprecated
	@Nonnull
	@SuppressWarnings("removal")
	public <X> JpaCteCriteria<X> with(@Nonnull AbstractQuery<X> criteria) {
		// Use of acquireUniqueAlias() results in interpretation cache miss
		return withInternal( "_" + acquireUniqueAlias(), criteria );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionAll(
			@Nonnull AbstractQuery<X> baseCriteria,
			@Nonnull Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( generateAlias(), baseCriteria, false, recursiveCriteriaProducer );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionDistinct(
			@Nonnull AbstractQuery<X> baseCriteria,
			@Nonnull Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( generateAlias(), baseCriteria, true, recursiveCriteriaProducer );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> with(@Nonnull String name, @Nonnull AbstractQuery<X> criteria) {
		return withInternal( validateCteName( name ), criteria );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionAll(
			@Nonnull String name,
			@Nonnull AbstractQuery<X> baseCriteria,
			@Nonnull Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( validateCteName( name ), baseCriteria, false, recursiveCriteriaProducer );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionDistinct(
			@Nonnull String name,
			@Nonnull AbstractQuery<X> baseCriteria,
			@Nonnull Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( validateCteName( name ), baseCriteria, true, recursiveCriteriaProducer );
	}

	private String validateCteName(String name) {
		if ( name == null || name.isBlank() ) {
			throw new IllegalArgumentException( "Illegal empty CTE name" );
		}
		if ( !isAlphabetic( name.charAt( 0 ) ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Illegal CTE name [%s]. Names must start with an alphabetic character!",
							name
					)
			);
		}
		return name;
	}

	private <X> JpaCteCriteria<X> withInternal(String name, AbstractQuery<X> criteria) {
		final var cteStatement = new SqmCteStatement<>(
				name,
				(SqmSelectQuery<X>) criteria,
				this,
				nodeBuilder()
		);
		if ( cteStatements.putIfAbsent( name, cteStatement ) != null ) {
			throw new IllegalArgumentException( "A CTE with the label " + cteStatement.getCteTable().getCteName() + " already exists" );
		}
		return cteStatement;
	}

	private <X> JpaCteCriteria<X> withInternal(
			String name,
			AbstractQuery<X> baseCriteria,
			boolean unionDistinct,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		final var cteStatement = new SqmCteStatement<>(
				name,
				(SqmSelectQuery<X>) baseCriteria,
				unionDistinct,
				recursiveCriteriaProducer,
				this,
				nodeBuilder()
		);
		if ( cteStatements.putIfAbsent( name, cteStatement ) != null ) {
			throw new IllegalArgumentException( "A CTE with the label " + cteStatement.getCteTable().getCteName() + " already exists" );
		}
		return cteStatement;
	}

	@Nonnull
	@Override
	public SqmRoot<E> getTarget() {
		return target;
	}

	@Override
	@NullnessUtil.Initializer
	public void setTarget(@Nonnull JpaRoot<E> root) {
		this.target = (SqmRoot<E>) root;
	}

	@Nonnull
	@Override
	public <U> SqmSubQuery<U> subquery(@Nonnull Class<U> type) {
		return new SqmSubQuery<>( this, type, nodeBuilder() );
	}

	protected void appendHqlCteString(StringBuilder sb, SqmRenderContext context) {
		if ( !cteStatements.isEmpty() ) {
			sb.append( "with " );
			for ( var value : cteStatements.values() ) {
				value.appendHqlString( sb, context );
				sb.append( ", " );
			}
			sb.setLength( sb.length() - 2 );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof AbstractSqmDmlStatement<?> that
			&& getClass() == that.getClass()
			&& getTarget().equals( that.getTarget() )
			&& Objects.equals( cteStatements, that.cteStatements );
	}

	@Override
	public int hashCode() {
		int result = getTarget().hashCode();
		result = 31 * result + Objects.hashCode( cteStatements );
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof AbstractSqmDmlStatement<?> that
			&& getClass() == that.getClass()
			&& getTarget().isCompatible( that.getTarget() )
			&& SqmCacheable.areCompatible( cteStatements, that.cteStatements );
	}

	@Override
	public int cacheHashCode() {
		int result = getTarget().cacheHashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( cteStatements );
		return result;
	}
}
