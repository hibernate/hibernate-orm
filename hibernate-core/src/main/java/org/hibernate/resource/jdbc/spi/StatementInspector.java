/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.spi;

import java.io.Serializable;

/**
 * Implementors may inspect and even process each SQL command issued
 * by a session, before a {@linkplain java.sql.PreparedStatement JDBC
 * statement} is prepared. A {@code StatementInspector} may be either:
 * <ul>
 * <li>shared by all sessions created by a given session factory, in
 *     which case it must be thread-safe, or
 * <li>a dedicated instance {@linkplain
 *     org.hibernate.SessionBuilder#statementInspector registered}
 *     for a certain session.
 * </ul>
 * <p>
 * An implementation may be specified via the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#STATEMENT_INSPECTOR}.
 * An implementation registered this way is shared between sessions.
 *
 * @see org.hibernate.cfg.AvailableSettings#STATEMENT_INSPECTOR
 * @see org.hibernate.cfg.Configuration#setStatementInspector(StatementInspector)
 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatementInspector(StatementInspector)
 * @see org.hibernate.SessionBuilder#statementInspector(StatementInspector)
 *
 * @author Steve Ebersole
 */
public interface StatementInspector extends Serializable {
	/**
	 * Inspect the given SQL command, possibly returning a different
	 * SQL command to be used instead. A {@code null} return value is
	 * interpreted as if the method had returned its argument.
	 *
	 * @param sql The SQL to inspect
	 *
	 * @return The processed SQL to use; may be {@code null}
	 */
	String inspect(String sql);
}
