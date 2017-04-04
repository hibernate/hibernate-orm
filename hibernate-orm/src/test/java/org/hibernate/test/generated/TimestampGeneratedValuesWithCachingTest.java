/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.generated;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.RequiresDialect;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(
		value = {
				SybaseDialect.class, Sybase11Dialect.class, SybaseAnywhereDialect.class,
				SybaseASE15Dialect.class, SQLServerDialect.class
		}
)
public class TimestampGeneratedValuesWithCachingTest extends AbstractGeneratedPropertyTest {
	public final String[] getMappings() {
		return new String[] { "generated/MSSQLGeneratedPropertyEntity.hbm.xml" };
	}
}
