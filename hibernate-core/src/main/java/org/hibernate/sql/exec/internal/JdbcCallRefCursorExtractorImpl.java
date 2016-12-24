/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;

/**
 * @author Steve Ebersole
 */
public class JdbcCallRefCursorExtractorImpl implements JdbcCallRefCursorExtractor {
	@Override
	public ResultSet extractResultSet(
			CallableStatement callableStatement, SharedSessionContractImplementor session) {
		return null;
	}
}
