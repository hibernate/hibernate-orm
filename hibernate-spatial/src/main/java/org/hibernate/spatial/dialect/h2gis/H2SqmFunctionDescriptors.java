/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.h2gis;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.spatial.BaseSqmFunctionDescriptors;

public class H2SqmFunctionDescriptors extends BaseSqmFunctionDescriptors {
	public H2SqmFunctionDescriptors(FunctionContributions contributions) {
		super( contributions );
	}
}
