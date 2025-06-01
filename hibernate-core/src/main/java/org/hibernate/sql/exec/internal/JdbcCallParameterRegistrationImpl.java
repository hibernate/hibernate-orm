/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.OutputableType;
import org.hibernate.sql.exec.spi.JdbcCallParameterExtractor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public class JdbcCallParameterRegistrationImpl implements JdbcCallParameterRegistration {
	private final String name;
	private final int jdbcParameterPositionStart;
	private final ParameterMode parameterMode;
	private final OutputableType<?> ormType;
	private final JdbcParameterBinder parameterBinder;
	private final JdbcCallParameterExtractorImpl<?> parameterExtractor;
	private final JdbcCallRefCursorExtractorImpl refCursorExtractor;

	public JdbcCallParameterRegistrationImpl(
			String name,
			int jdbcParameterPositionStart,
			ParameterMode parameterMode,
			OutputableType<?> ormType,
			JdbcParameterBinder parameterBinder,
			JdbcCallParameterExtractorImpl<?> parameterExtractor,
			JdbcCallRefCursorExtractorImpl refCursorExtractor) {
		this.name = name;
		this.jdbcParameterPositionStart = jdbcParameterPositionStart;
		this.parameterMode = parameterMode;
		this.ormType = ormType;
		this.parameterBinder = parameterBinder;
		this.parameterExtractor = parameterExtractor;
		this.refCursorExtractor = refCursorExtractor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public JdbcParameterBinder getParameterBinder() {
		return parameterBinder;
	}

	@Override
	public JdbcCallParameterExtractor<?> getParameterExtractor() {
		return parameterExtractor;
	}

	@Override
	public JdbcCallRefCursorExtractorImpl getRefCursorExtractor() {
		return refCursorExtractor;
	}

	@Override
	public ParameterMode getParameterMode() {
		return parameterMode;
	}

	@Override
	public OutputableType<?> getParameterType() {
		return ormType;
	}

	@Override
	public void registerParameter(
			CallableStatement callableStatement, SharedSessionContractImplementor session) {
		switch ( parameterMode ) {
			case REF_CURSOR: {
				registerRefCursorParameter( callableStatement, session );
				break;
			}
			case IN: {
				// nothing to prepare
				break;
			}
			default: {
				// OUT and INOUT
				registerOutputParameter( callableStatement, session );
				break;
			}
		}
	}

	private void registerRefCursorParameter(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session) {
		session.getFactory().getServiceRegistry()
				.requireService( RefCursorSupport.class )
				.registerRefCursorParameter( callableStatement, jdbcParameterPositionStart );

	}

	private void registerOutputParameter(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session) {
		final JdbcType sqlTypeDescriptor = ormType.getJdbcType();
		try {
			sqlTypeDescriptor.registerOutParameter( callableStatement, jdbcParameterPositionStart );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to register CallableStatement out parameter"
			);
		}
	}
}
