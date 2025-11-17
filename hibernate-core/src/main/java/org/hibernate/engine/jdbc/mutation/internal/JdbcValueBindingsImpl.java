/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.UnknownParameterException;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * @author Steve Ebersole
 */
public class JdbcValueBindingsImpl implements JdbcValueBindings {
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
		return bindingGroupMap.get( tableName );
	}

	@Override
	public void bindValue(
			Object value,
			String tableName,
			String columnName,
			ParameterUsage usage) {
		final JdbcValueDescriptor jdbcValueDescriptor = jdbcValueDescriptorAccess.resolveValueDescriptor( tableName, columnName, usage );
		if ( jdbcValueDescriptor == null ) {
			throw new UnknownParameterException( mutationType, mutationTarget, tableName, columnName, usage );
		}

		resolveBindingGroup( jdbcValueDescriptorAccess.resolvePhysicalTableName( tableName ) ).bindValue( columnName, value, jdbcValueDescriptor );
	}

	private BindingGroup resolveBindingGroup(String tableName) {
		final BindingGroup existing = bindingGroupMap.get( tableName );
		if ( existing != null ) {
			assert tableName.equals( existing.getTableName() );
			return existing;
		}

		final BindingGroup created = new BindingGroup( tableName );
		bindingGroupMap.put( tableName, created );
		return created;
	}

	@Override
	public void beforeStatement(PreparedStatementDetails statementDetails) {
		final BindingGroup bindingGroup = bindingGroupMap.get( statementDetails.getMutatingTableDetails().getTableName() );
		if ( bindingGroup == null ) {
			statementDetails.resolveStatement();
		}
		else {
			bindingGroup.forEachBinding( (binding) -> {
				try {
					binding.getValueBinder().bind(
							statementDetails.resolveStatement(),
							binding.getValue(),
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
		final BindingGroup bindingGroup = bindingGroupMap.remove( mutatingTable.getTableName() );
		if ( bindingGroup == null ) {
			return;
		}

		bindingGroup.clear();
	}

	/**
	 * Access to {@link JdbcValueDescriptor} values
	 */
	public interface JdbcValueDescriptorAccess {

		default String resolvePhysicalTableName(String tableName) {
			return tableName;
		}

		JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage);
	}
}
