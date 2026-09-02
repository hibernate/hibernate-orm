/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi.mutation.jdbc;

import java.util.List;
import java.util.Set;

import org.hibernate.SPI;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueDescriptorImpl;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * Supported base for an immutable, single-table JDBC mutation operation.
 * Subclasses select the fixed logical mutation type while this base owns the
 * table, SQL, parameter, callable, and expectation invariants.
 *
 * @author Steve Ebersole
 */
@SPI({ USE, IMPLEMENT })
public abstract class AbstractJdbcMutation implements JdbcMutationOperation {
	private final TableMapping tableDetails;
	private final MutationTarget mutationTarget;
	private final String sql;
	private final boolean callable;
	private final Expectation expectation;

	private final List<JdbcValueDescriptor> jdbcValueDescriptors;
	private final List<? extends JdbcParameterBinder> parameterBinders;

	@SPI(IMPLEMENT)
	protected AbstractJdbcMutation(
			TableMapping tableDetails,
			MutationTarget mutationTarget,
			String sql,
			boolean callable,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders) {
		this.tableDetails = tableDetails;
		this.mutationTarget = mutationTarget;
		this.sql = sql;
		this.callable = callable;
		this.expectation = expectation;
		this.parameterBinders = List.copyOf( parameterBinders );

		this.jdbcValueDescriptors = arrayList( this.parameterBinders.size() );
		for ( int i = 0; i < this.parameterBinders.size(); i++ ) {
			final var parameterDescriptor =
					new JdbcValueDescriptorImpl( this.parameterBinders.get( i ),
							expectation.getNumberOfParametersUsed() + i + 1 );
			this.jdbcValueDescriptors.add( parameterDescriptor );
		}
	}

	@Override
	public final TableMapping getTableDetails() {
		return tableDetails;
	}

	@Override
	public final Set<String> getAffectedTableNames() {
		return Set.of( getTableDetails().getTableName() );
	}

	@Override
	public final MutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public final String getSqlString() {
		return sql;
	}

	@Override
	public final List<? extends JdbcParameterBinder> getParameterBinders() {
		return parameterBinders;
	}

	@Override
	public final JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
		for ( int i = 0; i < jdbcValueDescriptors.size(); i++ ) {
			final var descriptor = jdbcValueDescriptors.get( i );
			if ( descriptor.getColumnName().equals( columnName )
					&& descriptor.getUsage() == usage ) {
				return descriptor;
			}
		}
		return null;
	}
	@Override
	public final boolean isCallable() {
		return callable;
	}

	@Override
	public final Expectation getExpectation() {
		return expectation;
	}

	@Override
	public abstract MutationType getMutationType();
}
