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
package org.hibernate.test.manytomanyassociationclass;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.manytomanyassociationclass.compositeid.ManyToManyAssociationClassCompositeIdTest;
import org.hibernate.test.manytomanyassociationclass.surrogateid.assigned.ManyToManyAssociationClassAssignedIdTest;
import org.hibernate.test.manytomanyassociationclass.surrogateid.generated.ManyToManyAssociationClassGeneratedIdTest;

/**
 * Tests on many-to-many association using an association class with a composite ID containing
 * the IDs from the associated entities.
 *
 * @author Gail Badner
 */
public class ManyToManyAssociationClassSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "Many-to-many with associaiton class tests" );
		suite.addTest( ManyToManyAssociationClassCompositeIdTest.suite() );
		suite.addTest( ManyToManyAssociationClassAssignedIdTest.suite() );
		suite.addTest( ManyToManyAssociationClassGeneratedIdTest.suite() );
		return suite;
	}
}
