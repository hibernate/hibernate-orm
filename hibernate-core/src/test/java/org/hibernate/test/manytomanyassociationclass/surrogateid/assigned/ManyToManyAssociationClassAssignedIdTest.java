/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
