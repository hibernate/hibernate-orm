/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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

	private final Map<String, BindingGroup> bindingGroupMap = new HashMap<>();

	public JdbcValueBindingsImpl(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			JdbcValueDescriptorAccess jdbcValueDescriptorAccess) {
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
		this.jdbcValueDescriptorAccess = jdbcValueDescriptorAccess;
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
			ParameterUsage usage,
			SharedSessionContractImplementor session) {
		final JdbcValueDescriptor jdbcValueDescriptor = jdbcValueDescriptorAccess.resolveValueDescriptor( tableName, columnName, usage );
		if ( jdbcValueDescriptor == null ) {
			throw new UnknownParameterException( mutationType, mutationTarget, tableName, columnName, usage );
		}

		resolveBindingGroup( tableName ).bindValue( columnName, value, jdbcValueDescriptor );
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
	public void beforeStatement(
			PreparedStatementDetails statementDetails,
			SharedSessionContractImplementor session) {
		final BindingGroup bindingGroup = bindingGroupMap.get( statementDetails.getMutatingTableDetails().getTableName() );
		if ( bindingGroup == null ) {
			return;
		}

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

	@Override
	public void afterStatement(
			TableMapping mutatingTable,
			SharedSessionContractImplementor session) {
		final BindingGroup bindingGroup = bindingGroupMap.remove( mutatingTable.getTableName() );
		if ( bindingGroup == null ) {
			return;
		}

		bindingGroup.clear();
	}

	/**
	 * Access to {@link JdbcValueDescriptor} values
	 */
	@FunctionalInterface
	public interface JdbcValueDescriptorAccess {
		JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage);
	}
}
