/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.ast.from.TableBinding;
import org.hibernate.sql.ast.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public class CompositeColumnBindingSource implements ColumnBindingSource {
	private final List<ColumnBindingSource> components;

	public CompositeColumnBindingSource(ColumnBindingSource... components) {
		final List<ColumnBindingSource> componentList = new ArrayList<>();
		for ( ColumnBindingSource component : components ) {
			if ( component != null ) {
				componentList.add( component );
			}
		}
		this.components = componentList;
	}

	@Override
	public TableGroup getTableGroup() {
		return (TableGroup) components.get( components.size() - 1 );
	}

	@Override
	public TableBinding locateTableBinding(Table table) {
		for ( ColumnBindingSource component : components ) {
			final TableBinding tableBinding = component.locateTableBinding( table );
			if ( tableBinding != null ) {
				return tableBinding;
			}
		}

		throw new IllegalStateException( "Could not locate TableBinding for Table : " + table );
	}

	@Override
	public ColumnBinding resolveColumnBinding(Column column) {
		for ( ColumnBindingSource component : components ) {
			final ColumnBinding columnBinding = component.resolveColumnBinding( column );
			if ( columnBinding != null ) {
				return columnBinding;
			}
		}

		throw new IllegalStateException( "Could not locate ColumnBinding for Column : " + column.toLoggableString() );
	}
}
