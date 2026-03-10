/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueBindingsImplementor;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueDescriptorAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.sql.model.TableMapping;

import java.sql.SQLException;
import java.util.Locale;

import static org.hibernate.action.queue.Helper.normalizeTableName;

/**
 * @author Steve Ebersole
 */
public class NormalizedJdbcValueBindings implements JdbcValueBindingsImplementor {
	private final String tableName;
	private final BindingGroup bindingGroup;
	private final JdbcValueDescriptorAccess jdbcValueDescriptorAccess;
	private final SharedSessionContractImplementor session;

	public NormalizedJdbcValueBindings(
			TableMapping tableMapping,
			JdbcValueDescriptorAccess jdbcValueDescriptorAccess,
			SharedSessionContractImplementor session) {
		this.tableName = normalizeTableName( tableMapping.getTableName() );
		this.jdbcValueDescriptorAccess = jdbcValueDescriptorAccess;
		this.session = session;
		this.bindingGroup = new BindingGroup( tableName );
	}

	@Override
	public BindingGroup getBindingGroup(String tableName) {
		assert normalizeTableName( tableName ).equals( this.tableName );
		return bindingGroup;
	}

	@Override
	public void bindValue(Object value, String tableName, String columnName, ParameterUsage usage) {
		assert normalizeTableName( tableName ).equals( this.tableName );
	}

	@Override
	public void beforeStatement(PreparedStatementDetails statementDetails) {
		bindingGroup.forEachBinding( (binding) -> {
			try {
				// Unwrap MutableObject handles used by cycle breaking
				Object valueToBindObject = binding.getValue();
				if ( valueToBindObject instanceof MutableObject ) {
					valueToBindObject = ( (MutableObject<?>) valueToBindObject ).get();
				}

				binding.getValueBinder().bind(
						statementDetails.resolveStatement(),
						valueToBindObject,
						binding.getPosition(),
						session
				);
			}
			catch (SQLException e) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert(
						e,
						String.format(
								Locale.ROOT,
								"Unable to bind parameter #%s - %s",
								binding.getPosition(),
								binding.getValue()
						)
				);
			}
		} );
	}

	@Override
	public void afterStatement(TableMapping mutatingTable) {
		assert normalizeTableName( mutatingTable.getTableName() ).equals( this.tableName );

	}

	@Override
	public @Nullable Object getBoundValue(String tableName, String columnName, ParameterUsage usage) {
		assert normalizeTableName( tableName ).equals( this.tableName );
		return null;
	}

	@Override
	public void replaceValue(String tableName, String columnName, ParameterUsage usage, Object newValue) {
		assert normalizeTableName( tableName ).equals( this.tableName );

	}
}
