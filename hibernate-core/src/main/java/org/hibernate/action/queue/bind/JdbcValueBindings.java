/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.spi.Binding;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueDescriptorAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public class JdbcValueBindings {
	private final TableDescriptor tableDescriptor;
	private final JdbcValueDescriptorAccess jdbcValueDescriptorAccess;
	private final BindingGroup bindingGroup;

	public JdbcValueBindings(TableDescriptor tableDescriptor, JdbcValueDescriptorAccess jdbcValueDescriptorAccess) {
		this.tableDescriptor = tableDescriptor;
		this.jdbcValueDescriptorAccess = jdbcValueDescriptorAccess;
		this.bindingGroup = new BindingGroup( tableDescriptor.normalizedName() );
	}

	public void beforeStatement(PreparedStatement preparedStatement, SharedSessionContractImplementor session) {
		bindingGroup.forEachBinding( (binding) -> {
			try {
				// Unwrap MutableObject handles used by cycle breaking
				Object valueToBindObject = binding.getValue();
				if ( valueToBindObject instanceof MutableObject ) {
					valueToBindObject = ((MutableObject<?>) valueToBindObject).get();
				}

				binding.getValueBinder().bind(
						preparedStatement,
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

	public void bindValue(Object columnValue, String columnName, ParameterUsage parameterUsage) {
		final var jdbcValueDescriptor = jdbcValueDescriptorAccess.resolveValueDescriptor(
				tableDescriptor.normalizedName(),
				columnName,
				parameterUsage
		);
		bindingGroup.bindValue( columnName, columnValue, jdbcValueDescriptor );
	}

	public Object getBoundValue(String columnName, ParameterUsage usage) {
		final Binding binding = bindingGroup.findBinding( columnName, usage );
		return binding == null ? null : binding.getValue();
	}

	public void replaceValue(String columnName, ParameterUsage parameterUsage, Object newValue) {
		final Binding binding = bindingGroup.getBinding( columnName, parameterUsage );
		binding.setValue( newValue );
	}
}
