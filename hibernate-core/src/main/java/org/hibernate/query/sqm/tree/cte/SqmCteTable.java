/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.cte;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaCteCriteriaType;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleSimpleSqmPathSource;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.tuple.internal.CteTupleTableGroupProducer;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.type.BasicType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteTable<T> extends AnonymousTupleType<T> implements JpaCteCriteriaType<T>, SqmCacheable {
	private final String name;
	private final SqmCteStatement<T> cteStatement;
	private final List<SqmCteTableColumn> columns;

	// Need to suppress some Checker Framework errors, because passing the `this` reference is unsafe,
	// though we make it safe by not calling any methods on it until initialization finishes
	@SuppressWarnings({"uninitialized", "method.invocation", "argument"})
	private SqmCteTable(
			String name,
			SqmCteStatement<T> cteStatement,
			SqmSelectQuery<T> selectStatement) {
		super( selectStatement );
		this.name = name;
		this.cteStatement = cteStatement;
		final List<SqmCteTableColumn> columns = new ArrayList<>( componentCount() );
		for ( int i = 0; i < componentCount(); i++ ) {
			columns.add( new SqmCteTableColumn( this, getComponentName( i ), get( i ) ) );
		}
		this.columns = columns;
	}

	public static <X> SqmCteTable<X> createStatementTable(
			String name,
			SqmCteStatement<X> cteStatement,
			SqmSelectQuery<X> selectStatement) {
		return new SqmCteTable<>( name, cteStatement, selectStatement );
	}

	@Override
	public CteTupleTableGroupProducer resolveTableGroupProducer(
			String aliasStem,
			List<SqlSelection> sqlSelections,
			FromClauseAccess fromClauseAccess) {
		return new CteTupleTableGroupProducer( this, aliasStem, toSqlTypedMappings( sqlSelections ), fromClauseAccess );
	}

	@Override
	public CteTupleTableGroupProducer resolveTableGroupProducer(
			String aliasStem,
			SqlTypedMapping[] sqlTypedMappings,
			FromClauseAccess fromClauseAccess) {
		return new CteTupleTableGroupProducer( this, aliasStem, sqlTypedMappings, fromClauseAccess );
	}

	public String getCteName() {
		return name;
	}

	public AnonymousTupleType<?> getTupleType() {
		return this;
	}

	public List<SqmCteTableColumn> getColumns() {
		return columns;
	}

	public SqmCteStatement<T> getCteStatement() {
		return cteStatement;
	}

	@Override
	public @Nullable String getName() {
		// TODO: this is extremely fragile!
		//       better to distinguish between generated and explicit aliases
		return name.charAt( 0 ) == '_'
				? null // Created through JPA criteria without an explicit name
				: name;
	}

	@Override
	public DomainType<T> getType() {
		return this;
	}

	@Override
	public List<JpaCteCriteriaAttribute> getAttributes() {
		//noinspection unchecked
		return (List<JpaCteCriteriaAttribute>) (List<?>) columns;
	}

	@Override
	public @Nullable JpaCteCriteriaAttribute getAttribute(String name) {
		final Integer index = getIndex( name );
		return index == null ? null : columns.get( index );
	}

	@Override
	public @Nullable SqmBindableType<?> get(String componentName) {
		final SqmBindableType<?> sqmExpressible = super.get( componentName );
		if ( sqmExpressible != null ) {
			return sqmExpressible;
		}
		return determineRecursiveCteAttributeType( name );
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		final SqmPathSource<?> subPathSource = super.findSubPathSource( name );
		if ( subPathSource != null ) {
			return subPathSource;
		}
		final BasicType<?> type = determineRecursiveCteAttributeType( name );
		if ( type == null ) {
			return null;
		}
		return new AnonymousTupleSimpleSqmPathSource<>(
				name,
				type,
				BindableType.SINGULAR_ATTRIBUTE
		);
	}

	private @Nullable BasicType<?> determineRecursiveCteAttributeType(String name) {
		if ( name.equals( cteStatement.getSearchAttributeName() ) ) {
			return cteStatement.nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( String.class );
		}
		if ( name.equals( cteStatement.getCyclePathAttributeName() ) ) {
			return cteStatement.nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( String.class );
		}
		if ( name.equals( cteStatement.getCycleMarkAttributeName() ) ) {
			return (BasicType<?>) castNonNull( cteStatement.getCycleLiteral() ).getNodeType();
		}
		return null;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		return o instanceof SqmCteTable<?> that
			&& Objects.equals( name, that.name )
			&& Objects.equals( columns, that.columns );
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + Objects.hashCode( columns );
		return result;
	}

	@Override
	public boolean isCompatible(Object o) {
		return o instanceof SqmCteTable<?> that
			&& Objects.equals( name, that.name )
			&& SqmCacheable.areCompatible( columns, that.columns );
	}

	@Override
	public int cacheHashCode() {
		int result = name.hashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( columns );
		return result;
	}
}
