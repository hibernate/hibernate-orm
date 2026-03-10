/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.UnknownParameterException;
import org.hibernate.engine.jdbc.mutation.spi.Binding;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueBindingsImplementor;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueDescriptorAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hibernate.action.queue.Helper.normalizeColumnName;
import static org.hibernate.action.queue.Helper.normalizeTableName;

/**
 * @author Steve Ebersole
 */
public class JdbcValueBindingsImpl implements JdbcValueBindingsImplementor {
	private final MutationType mutationType;
	private final MutationTarget<?> mutationTarget;
	private final JdbcValueDescriptorAccess jdbcValueDescriptorAccess;
	private final SharedSessionContractImplementor session;

	private final Map<String, BindingGroup> bindingGroupMap = new HashMap<>();

	public JdbcValueBindingsImpl(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			JdbcValueDescriptorAccess jdbcValueDescriptorAccess,
			SharedSessionContractImplementor session) {
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
		this.jdbcValueDescriptorAccess = jdbcValueDescriptorAccess;
		this.session = session;
	}

	@Override
	public BindingGroup getBindingGroup(String tableName) {
		final String normalizedTableName = normalizeTableName( tableName );
		return bindingGroupMap.get( normalizedTableName );
	}

	@Override
	public void bindValue(
			Object value,
			String tableName,
			String columnName,
			ParameterUsage usage) {
		// Normalize column name BEFORE calling resolveValueDescriptor because
		// AbstractJdbcMutation.findValueDescriptor expects normalized names
		final String normalizedColumnName = normalizeColumnName( columnName );
		final var jdbcValueDescriptor =
				jdbcValueDescriptorAccess.resolveValueDescriptor( tableName, normalizedColumnName, usage );
		if ( jdbcValueDescriptor == null ) {
			throw new UnknownParameterException( mutationType, mutationTarget, tableName, columnName, usage );
		}
		// Normalize table name for storage to match cycle breaking lookups
		final String physicalTableName = jdbcValueDescriptorAccess.resolvePhysicalTableName( tableName );
		final String normalizedTableName = normalizeTableName( physicalTableName );
		resolveBindingGroup( normalizedTableName )
				.bindValue( normalizedColumnName, value, jdbcValueDescriptor );
	}

	private BindingGroup resolveBindingGroup(String tableName) {
		final var existing = bindingGroupMap.get( tableName );
		if ( existing != null ) {
			assert tableName.equals( existing.getTableName() );
			return existing;
		}
		else {
			final var created = new BindingGroup( tableName );
			bindingGroupMap.put( tableName, created );
			return created;
		}
	}

	@Override
	public void beforeStatement(PreparedStatementDetails statementDetails) {
		final String normalizedTableName = normalizeTableName(
				statementDetails.getMutatingTableDetails().getTableName() );
		final var bindingGroup = bindingGroupMap.get( normalizedTableName );
		if ( bindingGroup == null ) {
			statementDetails.resolveStatement();
		}
		else {
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
	}

	@Override
	public void afterStatement(TableMapping mutatingTable) {
		final String normalizedTableName = normalizeTableName( mutatingTable.getTableName() );
		final var bindingGroup = bindingGroupMap.remove( normalizedTableName );
		if ( bindingGroup != null ) {
			bindingGroup.clear();
		}
	}

	@Override
	public Object getBoundValue(String tableName, String columnName, ParameterUsage usage) {
		final String normalizedTableName = normalizeTableName( tableName );
		final String normalizedColumnName = normalizeColumnName( columnName );
		final BindingGroup bindingGroup = bindingGroupMap.get( normalizedTableName );
		if ( bindingGroup == null ) {
			return null;
		}
		final Binding binding = bindingGroup.findBinding( normalizedColumnName, usage );
		return binding == null ? null : binding.getValue();
	}

	@Override
	public void replaceValue(String tableName, String columnName, ParameterUsage usage, Object newValue) {
		final String normalizedTableName = normalizeTableName( tableName );
		final String normalizedColumnName = normalizeColumnName( columnName );
		final BindingGroup bindingGroup = bindingGroupMap.get( normalizedTableName );
		final Binding binding = bindingGroup.getBinding( normalizedColumnName, usage );
		binding.setValue( newValue );
	}
}
