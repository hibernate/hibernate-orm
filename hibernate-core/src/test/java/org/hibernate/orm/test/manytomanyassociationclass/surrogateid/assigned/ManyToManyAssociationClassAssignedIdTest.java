/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomanyassociationclass.surrogateid.assigned;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.orm.test.manytomanyassociationclass.AbstractManyToManyAssociationClassTest;
import org.hibernate.orm.test.manytomanyassociationclass.Membership;

/**
 * Tests on many-to-many association using an association class with a surrogate ID that is assigned.
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/manytomanyassociationclass/surrogateid/assigned/Mappings.hbm.xml"
)
public class ManyToManyAssociationClassAssignedIdTest extends AbstractManyToManyAssociationClassTest {
	@Override
	public Membership createMembership(String name) {
		return new Membership( Long.valueOf( 1000 ), name );
	}
}
