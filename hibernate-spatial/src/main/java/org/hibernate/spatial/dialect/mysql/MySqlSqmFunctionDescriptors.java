/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.mysql;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.spatial.BaseSqmFunctionDescriptors;

public class MySqlSqmFunctionDescriptors extends BaseSqmFunctionDescriptors {
	public MySqlSqmFunctionDescriptors(FunctionContributions functionContributions) {
		super( functionContributions );
	}
}
