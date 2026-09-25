package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * @author Steve Ebersole
 */
@Incubating(since = "6.0", group = "sql-execution")
public interface JdbcParameterBinding {
	JdbcMapping getBindType();
	Object getBindValue();
}
