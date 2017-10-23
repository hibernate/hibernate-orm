/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomanyassociationclass.compositeid;

import org.hibernate.test.manytomanyassociationclass.AbstractManyToManyAssociationClassTest;
import org.hibernate.test.manytomanyassociationclass.Group;
import org.hibernate.test.manytomanyassociationclass.Membership;
import org.hibernate.test.manytomanyassociationclass.User;

/**
 * Tests on many-to-many association using an association class with a composite ID containing
 * the IDs from the associated entities.
 *
 * @author Gail Badner
 */
public class ManyToManyAssociationClassCompositeIdTest extends AbstractManyToManyAssociationClassTest {
	@Override
	public String[] getMappings() {
		return new String[] { "manytomanyassociationclass/compositeid/Mappings.hbm.xml" };
	}

	@Override
	public Membership createMembership( String name ) {
		return new MembershipWithCompositeId( name );
	}
	
	@Override
	public void deleteMembership(User u, Group g, Membership ug) {
		if ( u == null || g == null ) {
			throw new IllegalArgumentException();
		}
		u.getMemberships().remove( ug );
		g.getMemberships().remove( ug );
		ug.setId(null);
	}

}
