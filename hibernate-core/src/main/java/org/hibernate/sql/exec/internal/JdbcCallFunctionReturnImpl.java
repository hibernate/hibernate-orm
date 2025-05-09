/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.type.OutputableType;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public abstract class JdbcCallFunctionReturnImpl
		extends JdbcCallParameterRegistrationImpl
		implements JdbcCallFunctionReturn {

	protected JdbcCallFunctionReturnImpl(
			OutputableType<?> ormType,
			JdbcCallParameterExtractorImpl<?> parameterExtractor,
			JdbcCallRefCursorExtractorImpl refCursorExtractor) {
		super(
				null,
				1,
				refCursorExtractor == null ? ParameterMode.OUT : ParameterMode.REF_CURSOR,
				ormType,
				null,
				parameterExtractor,
				refCursorExtractor
		);
	}

	public static class RefCurserJdbcCallFunctionReturnImpl extends JdbcCallFunctionReturnImpl {
		public RefCurserJdbcCallFunctionReturnImpl(JdbcCallRefCursorExtractorImpl refCursorExtractor) {
			super( null, null, refCursorExtractor );
		}
	}

	public static class RegularJdbcCallFunctionReturnImpl extends JdbcCallFunctionReturnImpl {
		public <T> RegularJdbcCallFunctionReturnImpl(
				OutputableType<T> ormType, JdbcCallParameterExtractorImpl<T> parameterExtractor) {
			super( ormType, parameterExtractor, null );
		}
	}
}
