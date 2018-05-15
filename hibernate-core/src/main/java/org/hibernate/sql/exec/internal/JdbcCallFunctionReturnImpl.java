/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import javax.persistence.ParameterMode;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;

/**
 * @author Steve Ebersole
 */
public class JdbcCallFunctionReturnImpl extends JdbcCallParameterRegistrationImpl implements JdbcCallFunctionReturn {
	public JdbcCallFunctionReturnImpl(
			int jdbcTypeCode,
			AllowableParameterType ormType,
			JdbcCallParameterExtractorImpl parameterExtractor,
			JdbcCallRefCursorExtractorImpl refCursorExtractor) {
		super(
				null,
				0,
				ParameterMode.OUT,
				jdbcTypeCode,
				ormType,
				null,
				parameterExtractor,
				refCursorExtractor
		);
	}
}
