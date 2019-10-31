/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;

/**
 * @author Steve Ebersole
 */
public class TableGroupBuilder implements TableReferenceCollector {
	public static TableGroupBuilder builder(
			NavigablePath path,
			TableGroupProducer producer,
			LockMode lockMode,
			SqlAliasBase sqlAliasBase,
			SessionFactoryImplementor sessionFactory) {
		return new TableGroupBuilder( path, producer, lockMode, sqlAliasBase, sessionFactory );
	}

	private final NavigablePath path;
	private final TableGroupProducer producer;
	private final SessionFactoryImplementor sessionFactory;

	private final SqlAliasBase sqlAliasBase;
	private final LockMode lockMode;

	private final Function<TableReference,TableReferenceJoin> primaryJoinProducer;

	private TableReference primaryTableReference;
	private TableReference secondaryTableLhs;

	private java.util.List<TableReferenceJoin> tableJoins;

	private TableGroupBuilder(
			NavigablePath path,
			TableGroupProducer producer,
			LockMode lockMode,
			SqlAliasBase sqlAliasBase,
			SessionFactoryImplementor sessionFactory) {
		this( path, producer, lockMode, sqlAliasBase, null, sessionFactory );
	}

	private TableGroupBuilder(
			NavigablePath path,
			TableGroupProducer producer,
			LockMode lockMode,
			SqlAliasBase sqlAliasBase,
			Function<TableReference,TableReferenceJoin> primaryJoinProducer,
			SessionFactoryImplementor sessionFactory) {
		this.path = path;
		this.producer = producer;
		this.lockMode = lockMode;
		this.sqlAliasBase = sqlAliasBase;
		this.primaryJoinProducer = primaryJoinProducer;
		this.sessionFactory = sessionFactory;
	}

	public TableGroup build() {
		if ( primaryTableReference == null ) {
			throw new IllegalStateException( "Primary TableReference was not specified : " + path );
		}

		return new StandardTableGroup(
				path,
				producer,
				lockMode,
				primaryTableReference,
				tableJoins == null ? Collections.emptyList() : tableJoins,
				sqlAliasBase,
				sessionFactory
		);
	}

	@Override
	public void applyPrimaryReference(TableReference tableReference) {
		if ( primaryTableReference != null ) {
			assert primaryJoinProducer != null;

			addTableReferenceJoin( primaryJoinProducer.apply( tableReference ) );

		}
		else {
			primaryTableReference = tableReference;
		}

		secondaryTableLhs = tableReference;
	}

	@Override
	public void applySecondaryTableReferences(
			TableReference tableReference,
			JoinType tableReferenceJoinType,
			TableReferenceJoinPredicateProducer predicateProducer) {
		if ( primaryTableReference == null ) {
			primaryTableReference = tableReference;
			secondaryTableLhs = primaryTableReference;
		}
		else {
			addTableReferenceJoin(
					new TableReferenceJoin(
							tableReferenceJoinType,
							tableReference,
							predicateProducer.producePredicate(
									secondaryTableLhs,
									tableReference,
									tableReferenceJoinType
							)
					)
			);
		}
	}

	public void addTableReferenceJoin(TableReferenceJoin join) {
		if ( tableJoins == null ) {
			tableJoins = new ArrayList<>();
		}
		tableJoins.add( join );
	}
}
