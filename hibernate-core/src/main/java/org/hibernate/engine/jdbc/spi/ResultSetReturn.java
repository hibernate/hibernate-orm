/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

/**
 * Contract for extracting ResultSets from Statements, executing Statements,
 * managing Statement/ResultSet resources, and logging statement calls.
 * 
 * TODO: This could eventually utilize the new Return interface.  It would be
 * great to have a common API shared.
 *
 * Generally the methods here dealing with CallableStatement are extremely limited, relying on the legacy
 *
 * 
 * @author Brett Meyer
 * @author Steve Ebersole
 *
 * @deprecated (since 6.0) Use {@link JdbcStatementSupport} instead.  See
 * {@link JdbcCoordinator#getResultSetReturn()}
 */
@Deprecated
public interface ResultSetReturn extends JdbcStatementSupport {
}
