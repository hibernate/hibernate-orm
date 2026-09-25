package org.hibernate.sql.exec.spi;

import java.sql.CallableStatement;
import java.sql.ResultSet;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
@Incubating(since = "6.0", group = "sql-execution")
public interface JdbcCallRefCursorExtractor {
	ResultSet extractResultSet(CallableStatement callableStatement, SharedSessionContractImplementor session);
}
