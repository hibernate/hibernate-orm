/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;

/**
 * Support class for {@link TableReferenceJoinCollector} implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableReferenceCollector implements TableReferenceJoinCollector {
	private TableReference primaryTableReference;
	private List<TableReferenceJoin> tableReferenceJoins;

	public TableReference getPrimaryTableReference() {
		return primaryTableReference;
	}

	public List<TableReferenceJoin> getTableReferenceJoins() {
		return tableReferenceJoins == null ? Collections.emptyList() : tableReferenceJoins;
	}

	@Override
	public void addPrimaryReference(TableReference primaryReference) {
		if ( this.primaryTableReference != null ) {
			throw new IllegalStateException( "Not expecting primary TableReference" );
		}

		this.primaryTableReference = primaryReference;
	}

	@Override
	public void addSecondaryReference(TableReferenceJoin secondaryReference) {
		if ( tableReferenceJoins == null ) {
			tableReferenceJoins = new ArrayList<>();
		}

		tableReferenceJoins.add( secondaryReference );
	}

	public TableReference resolveTableReference(
			Table table,
			Function<Table, TableReference> creator,
			BiFunction<TableReference, Table, TableReferenceJoin> joinCreator) {
		if ( primaryTableReference == null ) {
			primaryTableReference = creator.apply( table );
			return primaryTableReference;
		}
		else if ( table.equals( primaryTableReference.getTable() ) ) {
			// re-use the root reference
			return primaryTableReference;
		}

		if ( tableReferenceJoins == null ) {
			tableReferenceJoins = new ArrayList<>();
			return createJoin( table, joinCreator );
		}
		else {
			TableReferenceJoin existing = null;
			for ( TableReferenceJoin tableReferenceJoin : tableReferenceJoins ) {
				if ( table.equals( tableReferenceJoin.getJoinedTableReference().getTable() ) ) {
					existing = tableReferenceJoin;
					break;
				}
			}

			if ( existing != null ) {
				return existing.getJoinedTableReference();
			}

			return createJoin( table, joinCreator );
		}
	}

	private TableReference createJoin(
			Table table,
			BiFunction<TableReference, Table, TableReferenceJoin> joinCreator) {
		final TableReferenceJoin join = joinCreator.apply( primaryTableReference, table );
		tableReferenceJoins.add( join );
		return join.getJoinedTableReference();
	}
}
