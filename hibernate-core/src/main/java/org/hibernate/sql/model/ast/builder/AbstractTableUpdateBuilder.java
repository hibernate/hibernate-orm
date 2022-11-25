/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableUpdateBuilder<O extends MutationOperation>
		extends AbstractRestrictedTableMutationBuilder<O, RestrictedTableMutation<O>>
		implements TableUpdateBuilder<O> {
	private final List<ColumnValueBinding> keyBindings = new ArrayList<>();
	private final List<ColumnValueBinding> valueBindings = new ArrayList<>();
	private List<ColumnValueBinding> lobValueBindings;

	public AbstractTableUpdateBuilder(
			MutationTarget mutationTarget,
			TableMapping tableMapping,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.UPDATE, mutationTarget, tableMapping, sessionFactory );
	}

	public AbstractTableUpdateBuilder(
			MutationTarget mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.UPDATE, mutationTarget, tableReference, sessionFactory );
	}

	protected List<ColumnValueBinding> getKeyBindings() {
		return keyBindings;
	}

	protected List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	protected List<ColumnValueBinding> getLobValueBindings() {
		return lobValueBindings;
	}

	@Override
	public void addValueColumn(
			String columnName,
			String columnWriteFragment,
			JdbcMapping jdbcMapping) {
		final ColumnValueBinding valueBinding = createValueBinding( columnName, columnWriteFragment, jdbcMapping );

		if ( jdbcMapping.getJdbcType().isLob() && getJdbcServices().getDialect().forceLobAsLastValue() ) {
			if ( lobValueBindings == null ) {
				lobValueBindings = new ArrayList<>();
				lobValueBindings.add( valueBinding );
			}
		}
		else {
			valueBindings.add( valueBinding );
		}
	}

	@Override
	public void addKeyColumn(
			String columnName,
			String columnWriteFragment,
			JdbcMapping jdbcMapping) {
		addColumn( columnName, columnWriteFragment, jdbcMapping, keyBindings );
	}
}
