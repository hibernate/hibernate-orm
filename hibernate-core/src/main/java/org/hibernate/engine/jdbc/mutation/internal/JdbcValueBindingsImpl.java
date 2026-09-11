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

	private BindingGroup singleBindingGroup;
	private Map<String, BindingGroup> bindingGroupMap;

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
		if ( singleBindingGroup != null ) {
			return tableName.equals( singleBindingGroup.getTableName() ) ? singleBindingGroup : null;
		}
		else if ( bindingGroupMap != null ) {
			return bindingGroupMap.get( tableName );
		}
		else {
			return null;
		}
	}

	@Override
	public void bindValue(
			Object value,
			String tableName,
			String columnName,
			ParameterUsage usage) {
		final var jdbcValueDescriptor =
				jdbcValueDescriptorAccess.resolveValueDescriptor( tableName, columnName, usage );
		if ( jdbcValueDescriptor == null ) {
			throw new UnknownParameterException( mutationType, mutationTarget, tableName, columnName, usage );
		}
		resolveBindingGroup( jdbcValueDescriptorAccess.resolvePhysicalTableName( tableName ) )
				.bindValue( columnName, value, jdbcValueDescriptor );
	}

	private BindingGroup resolveBindingGroup(String tableName) {
		if ( singleBindingGroup != null ) {
			if ( tableName.equals( singleBindingGroup.getTableName() ) ) {
				return singleBindingGroup;
			}
			final var created = new BindingGroup( tableName );
			bindingGroupMap = new HashMap<>();
			bindingGroupMap.put( singleBindingGroup.getTableName(), singleBindingGroup );
			bindingGroupMap.put( tableName, created );
			singleBindingGroup = null;
			return created;
		}
		else if ( bindingGroupMap != null ) {
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
		else {
			final var created = new BindingGroup( tableName );
			singleBindingGroup = created;
			return created;
		}
	}

	@Override
	public void beforeStatement(PreparedStatementDetails statementDetails) {
		final var bindingGroup =
				getBindingGroup( statementDetails.getMutatingTableDetails().getTableName() );
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
		final BindingGroup bindingGroup;
		if ( singleBindingGroup != null ) {
			if ( mutatingTable.getTableName().equals( singleBindingGroup.getTableName() ) ) {
				bindingGroup = singleBindingGroup;
				singleBindingGroup = null;
			}
			else {
				bindingGroup = null;
			}
		}
		else if ( bindingGroupMap != null ) {
			bindingGroup = bindingGroupMap.remove( mutatingTable.getTableName() );
		}
		else {
			bindingGroup = null;
		}
		if ( bindingGroup != null ) {
			bindingGroup.clear();
		}
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
