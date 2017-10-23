/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomanyassociationclass.surrogateid.assigned;

import org.hibernate.test.manytomanyassociationclass.AbstractManyToManyAssociationClassTest;
import org.hibernate.test.manytomanyassociationclass.Membership;

/**
 * Tests on many-to-many association using an association class with a surrogate ID that is assigned.
 *
 * @author Gail Badner
 */
public class ManyToManyAssociationClassAssignedIdTest extends AbstractManyToManyAssociationClassTest {
	@Override
	public String[] getMappings() {
		return new String[] { "manytomanyassociationclass/surrogateid/assigned/Mappings.hbm.xml" };
	}

	@Override
	public Membership createMembership(String name) {
		return new Membership( Long.valueOf( 1000 ), name );
	}
}
