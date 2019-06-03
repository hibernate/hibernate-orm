/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Gail Badner
 * @deprecated Use "hibernate.dialect.storage_engine=innodb" environment variable or JVM system property instead.
 */
@Deprecated
public class MySQL57InnoDBDialect extends MySQL5InnoDBDialect {

	@Override
	int getVersion() {
		return 570;
	}
}
