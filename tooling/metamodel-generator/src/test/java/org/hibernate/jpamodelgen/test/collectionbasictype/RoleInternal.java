/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.collectionbasictype;

import javax.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class RoleInternal implements Role {
	public static String TYPE = "Internal";

	@Override
	public String getType() {
		return TYPE;
	}
}
