/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

import jakarta.persistence.criteria.AbstractQuery;

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
			Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmRoot<E> target) {
		super( builder, querySource, parameters );
		this.cteStatements = cteStatements;
		this.target = target;
	}

	protected Map<String, SqmCteStatement<?>> copyCteStatements(SqmCopyContext context) {
		final Map<String, SqmCteStatement<?>> cteStatements = new LinkedHashMap<>( this.cteStatements.size() );
		for ( Map.Entry<String, SqmCteStatement<?>> entry : this.cteStatements.entrySet() ) {
			cteStatements.put( entry.getKey(), entry.getValue().copy( context ) );
		}
		return cteStatements;
	}

	protected void putAllCtes(SqmCteContainer cteContainer) {
		for ( SqmCteStatement<?> cteStatement : cteContainer.getCteStatements() ) {
			if ( cteStatements.putIfAbsent( cteStatement.getName(), cteStatement ) != null ) {
				throw new IllegalArgumentException( "A CTE with the label " + cteStatement.getCteTable().getCteName() + " already exists" );
			}
		}
	}

	@Override
	public Collection<SqmCteStatement<?>> getCteStatements() {
		return cteStatements.values();
	}

	@Override
	public SqmCteStatement<?> getCteStatement(String cteLabel) {
		return cteStatements.get( cteLabel );
	}

	@Override
	public Collection<? extends JpaCteCriteria<?>> getCteCriterias() {
		return cteStatements.values();
	}

	@Override
	public <X> JpaCteCriteria<X> getCteCriteria(String cteName) {
		return (JpaCteCriteria<X>) cteStatements.get( cteName );
	}

	@Override
	public <X> JpaCteCriteria<X> with(AbstractQuery<X> criteria) {
		return withInternal( SqmCreationHelper.acquireUniqueAlias(), criteria );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionAll(
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( SqmCreationHelper.acquireUniqueAlias(), baseCriteria, false, recursiveCriteriaProducer );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionDistinct(
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( SqmCreationHelper.acquireUniqueAlias(), baseCriteria, true, recursiveCriteriaProducer );
	}

	@Override
	public <X> JpaCteCriteria<X> with(String name, AbstractQuery<X> criteria) {
		return withInternal( validateCteName( name ), criteria );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionAll(
			String name,
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( validateCteName( name ), baseCriteria, false, recursiveCriteriaProducer );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionDistinct(
			String name,
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( validateCteName( name ), baseCriteria, true, recursiveCriteriaProducer );
	}

	private String validateCteName(String name) {
		if ( name == null || name.isBlank() ) {
			throw new IllegalArgumentException( "Illegal empty CTE name" );
		}
		if ( !Character.isAlphabetic( name.charAt( 0 ) ) ) {
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
		final SqmCteStatement<X> cteStatement = new SqmCteStatement<>(
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
		final SqmCteStatement<X> cteStatement = new SqmCteStatement<>(
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

	@Override
	public SqmRoot<E> getTarget() {
		return target;
	}

	@Override
	public void setTarget(JpaRoot<E> root) {
		this.target = (SqmRoot<E>) root;
	}

	@Override
	public <U> SqmSubQuery<U> subquery(Class<U> type) {
		return new SqmSubQuery<>( this, type, nodeBuilder() );
	}

	protected void appendHqlCteString(StringBuilder sb) {
		if ( !cteStatements.isEmpty() ) {
			sb.append( "with " );
			for ( SqmCteStatement<?> value : cteStatements.values() ) {
				value.appendHqlString( sb );
				sb.append( ", " );
			}
			sb.setLength( sb.length() - 2 );
		}
	}
}
