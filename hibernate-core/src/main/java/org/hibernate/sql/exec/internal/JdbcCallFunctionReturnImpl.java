/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.query.OutputableType;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public class JdbcCallFunctionReturnImpl extends JdbcCallParameterRegistrationImpl implements JdbcCallFunctionReturn {
	public JdbcCallFunctionReturnImpl(
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
}
