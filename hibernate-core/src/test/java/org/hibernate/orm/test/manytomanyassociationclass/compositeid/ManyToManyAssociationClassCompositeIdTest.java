/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomanyassociationclass.compositeid;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.orm.test.manytomanyassociationclass.AbstractManyToManyAssociationClassTest;
import org.hibernate.orm.test.manytomanyassociationclass.Group;
import org.hibernate.orm.test.manytomanyassociationclass.Membership;
import org.hibernate.orm.test.manytomanyassociationclass.User;

/**
 * Tests on many-to-many association using an association class with a composite ID containing
 * the IDs from the associated entities.
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/manytomanyassociationclass/compositeid/Mappings.hbm.xml"
)
public class ManyToManyAssociationClassCompositeIdTest extends AbstractManyToManyAssociationClassTest {

	@Override
	public Membership createMembership(String name) {
		return new MembershipWithCompositeId( name );
	}

	@Override
	public void deleteMembership(User u, Group g, Membership ug) {
		if ( u == null || g == null ) {
			throw new IllegalArgumentException();
		}
		u.getMemberships().remove( ug );
		g.getMemberships().remove( ug );
		ug.setId( null );
	}

}
