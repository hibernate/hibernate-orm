/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.ArrayList;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableReference;

/**
 * An array list for {@link ColumnValueParameter} that implements {@link SelectableConsumer} to add new parameters.
 */
@Internal
public class ColumnValueParameterList extends ArrayList<ColumnValueParameter> implements SelectableConsumer {

	private final TableReference tableReference;
	private final ParameterUsage parameterUsage;

	public ColumnValueParameterList(TableReference tableReference, ParameterUsage parameterUsage, int jdbcTypeCount) {
		super( jdbcTypeCount );
		this.tableReference = tableReference;
		this.parameterUsage = parameterUsage;
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void accept(int selectionIndex, SelectableMapping selectableMapping) {
		addColumValueParameter( selectableMapping );
	}

	public ColumnValueParameter addColumValueParameter(SelectableMapping selectableMapping) {
		final ColumnValueParameter columnValueParameter =
				new ColumnValueParameter( new ColumnReference( tableReference, selectableMapping ), parameterUsage );
		add( columnValueParameter );
		return columnValueParameter;
	}

	public void apply(Object parameterRef) {
		if ( parameterRef instanceof ColumnValueParameterList columnValueParameters ) {
			addAll( columnValueParameters );
		}
		else {
			add( (ColumnValueParameter) parameterRef );
		}
	}
}
