/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.manytomanyassociationclass.compositeid;

import junit.framework.Test;

import org.hibernate.test.manytomanyassociationclass.AbstractManyToManyAssociationClassTest;
import org.hibernate.test.manytomanyassociationclass.Membership;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * Tests on many-to-many association using an association class with a composite ID containing
 * the IDs from the associated entities.
 *
 * @author Gail Badner
 */
public class ManyToManyAssociationClassCompositeIdTest extends AbstractManyToManyAssociationClassTest {
	public ManyToManyAssociationClassCompositeIdTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "manytomanyassociationclass/compositeid/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ManyToManyAssociationClassCompositeIdTest.class );
	}

	public Membership createMembership( String name ) {
		return new MembershipWithCompositeId( name );
	}
}
