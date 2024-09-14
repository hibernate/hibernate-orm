/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
