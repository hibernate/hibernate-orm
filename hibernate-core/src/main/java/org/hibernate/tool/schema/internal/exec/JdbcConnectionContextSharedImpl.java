/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;

/**
 * @author Steve Ebersole
 */
public class JdbcConnectionContextSharedImpl extends AbstractJdbcConnectionContextImpl {
	public JdbcConnectionContextSharedImpl(
			JdbcConnectionAccess jdbcConnectionAccess,
			SqlStatementLogger sqlStatementLogger,
			boolean needsAutoCommit) {
		super( jdbcConnectionAccess, sqlStatementLogger, needsAutoCommit );
	}

	@Override
	public void release() {
		// for a shared JdbcConnectionContext do not release it as part of the normal
		// source/target cleanup call stacks.  The creator will explicitly close the
		// shared JdbcConnectionContext via #reallyRelease
	}

	@Override
	public void reallyRelease() {
		super.reallyRelease();
	}
}
