/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.ArrayList;
import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
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
			BiFunction<String,TableGroup,TableReferenceJoin> tableReferenceJoinCreator,
			SessionFactoryImplementor sessionFactory) {
		return new TableGroupBuilder( path, producer, lockMode, sqlAliasBase, tableReferenceJoinCreator, sessionFactory );
	}

	private final NavigablePath path;
	private final TableGroupProducer producer;
	private final BiFunction<String, TableGroup, TableReferenceJoin> tableReferenceJoinCreator;
	private final SessionFactoryImplementor sessionFactory;

	private final SqlAliasBase sqlAliasBase;
	private final LockMode lockMode;

	private BiFunction<TableReference, TableReference,TableReferenceJoin> primaryJoinProducer;

	private TableReference primaryTableReference;
	private TableReference secondaryTableLhs;

	private java.util.List<TableReferenceJoin> tableJoins;

	private TableGroupBuilder(
			NavigablePath path,
			TableGroupProducer producer,
			LockMode lockMode,
			SqlAliasBase sqlAliasBase,
			BiFunction<String, TableGroup, TableReferenceJoin> tableReferenceJoinCreator,
			SessionFactoryImplementor sessionFactory) {
		this.path = path;
		this.producer = producer;
		this.lockMode = lockMode;
		this.sqlAliasBase = sqlAliasBase;
		this.tableReferenceJoinCreator = tableReferenceJoinCreator;
		this.sessionFactory = sessionFactory;
	}

	public NavigablePath getPath() {
		return path;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public SqlAliasBase getSqlAliasBase() {
		return sqlAliasBase;
	}

	@Override
	public void applyPrimaryJoinProducer(BiFunction<TableReference, TableReference, TableReferenceJoin> primaryJoinProducer) {
		this.primaryJoinProducer = primaryJoinProducer;
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
				tableJoins,
				sqlAliasBase,
				tableReferenceJoinCreator,
				sessionFactory
		);
	}

	@Override
	public void applyPrimaryReference(TableReference tableReference) {
		if ( primaryTableReference != null ) {
			assert primaryJoinProducer != null;
			addTableReferenceJoin( primaryJoinProducer.apply( primaryTableReference, tableReference ) );
		}
		else {
			primaryTableReference = tableReference;
		}

		secondaryTableLhs = tableReference;
	}

	@Override
	public void applySecondaryTableReferences(
			TableReference tableReference,
			SqlAstJoinType tableReferenceSqlAstJoinType,
			TableReferenceJoinPredicateProducer predicateProducer) {
		if ( primaryTableReference == null ) {
			primaryTableReference = tableReference;
			secondaryTableLhs = primaryTableReference;
		}
		else {
			addTableReferenceJoin(
					new TableReferenceJoin(
							tableReferenceSqlAstJoinType,
							tableReference,
							predicateProducer.producePredicate(
									secondaryTableLhs,
									tableReference,
									tableReferenceSqlAstJoinType
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
