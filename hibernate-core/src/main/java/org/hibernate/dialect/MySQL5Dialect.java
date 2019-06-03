/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.hint.IndexQueryHintHandler;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;

/**
 * An SQL dialect for MySQL 5.x specific features.
 *
 * @author Steve Ebersole
 */
public class MySQL5Dialect extends MySQLDialect {

	@Override
	int getVersion() {
		return 500;
	}
}
