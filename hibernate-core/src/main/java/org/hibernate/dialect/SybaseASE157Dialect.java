/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.util.Map;

import org.hibernate.JDBCException;
import org.hibernate.LockOptions;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SybaseASE157LimitHandler;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.sql.ForUpdateFragment;

/**
 * An SQL dialect targeting Sybase Adaptive Server Enterprise (ASE) 15.7 and higher.
 *
 * @author Junyan Ren
 *
 * @deprecated use {@code SybaseASEDialect(1570)}
 */
@Deprecated
public class SybaseASE157Dialect extends SybaseASEDialect {

	public SybaseASE157Dialect() {
		super(1570);
	}

}
