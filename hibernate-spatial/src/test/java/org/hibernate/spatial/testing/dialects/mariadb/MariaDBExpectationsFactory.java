/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.mariadb;

import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.dialects.mysql.MySQL8ExpectationsFactory;

//for now, create the same expectations as for MySQL8
public class MariaDBExpectationsFactory extends MySQL8ExpectationsFactory {
	public MariaDBExpectationsFactory(DataSourceUtils dataSourceUtils) {
		super( dataSourceUtils );
	}
}
