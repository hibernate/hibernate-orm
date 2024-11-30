/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.cte;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaCteCriteriaType;
import org.hibernate.query.derived.AnonymousTupleSimpleSqmPathSource;
import org.hibernate.query.derived.AnonymousTupleType;
import org.hibernate.query.derived.CteTupleTableGroupProducer;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteTable<T> extends AnonymousTupleType<T> implements JpaCteCriteriaType<T> {
	private final String name;
	private final SqmCteStatement<T> cteStatement;
	private final List<SqmCteTableColumn> columns;

	private SqmCteTable(
			String name,
			SqmCteStatement<T> cteStatement,
			SqmSelectableNode<?>[] sqmSelectableNodes) {
		super( sqmSelectableNodes );
		this.name = name;
		this.cteStatement = cteStatement;
		final List<SqmCteTableColumn> columns = new ArrayList<>( componentCount() );
		for ( int i = 0; i < componentCount(); i++ ) {
			columns.add(
					new SqmCteTableColumn(
							this,
							getComponentName( i ),
							get( i )
					)
			);
		}
		this.columns = columns;
	}

	public static <X> SqmCteTable<X> createStatementTable(
			String name,
			SqmCteStatement<X> cteStatement,
			SqmSelectQuery<X> selectStatement) {
		final SqmSelectableNode<?>[] sqmSelectableNodes = selectStatement.getQueryPart()
				.getFirstQuerySpec()
				.getSelectClause()
				.getSelectionItems()
				.toArray( SqmSelectableNode[]::new );
		return new SqmCteTable<>( name, cteStatement, sqmSelectableNodes );
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
	public String getName() {
		if ( Character.isDigit( name.charAt( 0 ) ) ) {
			// Created through JPA criteria without an explicit name
			return null;
		}
		return name;
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
	public JpaCteCriteriaAttribute getAttribute(String name) {
		final Integer index = getIndex( name );
		if ( index == null ) {
			return null;
		}
		return columns.get( index );
	}

	@Override
	public SqmExpressible<?> get(String componentName) {
		final SqmExpressible<?> sqmExpressible = super.get( componentName );
		if ( sqmExpressible != null ) {
			return sqmExpressible;
		}
		return determineRecursiveCteAttributeType( name );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
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

	private BasicType<?> determineRecursiveCteAttributeType(String name) {
		if ( name.equals( cteStatement.getSearchAttributeName() ) ) {
			return cteStatement.nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( String.class );
		}
		if ( name.equals( cteStatement.getCyclePathAttributeName() ) ) {
			return cteStatement.nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( String.class );
		}
		if ( name.equals( cteStatement.getCycleMarkAttributeName() ) ) {
			return (BasicType<?>) cteStatement.getCycleLiteral().getNodeType();
		}
		return null;
	}
}
