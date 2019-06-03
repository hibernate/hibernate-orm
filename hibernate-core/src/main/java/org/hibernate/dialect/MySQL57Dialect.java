/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.query.spi.QueryEngine;

/**
 * @author Gail Badner
 */
public class MySQL57Dialect extends MySQL55Dialect {

	@Override
	int getVersion() {
		return 570;
	}
}
