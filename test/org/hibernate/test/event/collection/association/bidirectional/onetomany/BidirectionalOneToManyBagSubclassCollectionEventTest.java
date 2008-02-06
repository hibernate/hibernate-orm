//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution statements
 * applied by the authors.
 *
 * All third-party contributions are distributed under license by Red Hat
 * Middleware LLC.  This copyrighted material is made available to anyone
 * wishing to use, modify, copy, or redistribute it subject to the terms
 * and conditions of the GNU Lesser General Public License, as published by
 * the Free Software Foundation.  This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details.  You should
 * have received a copy of the GNU Lesser General Public License along with
 * this distribution; if not, write to: Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor Boston, MA  02110-1301  USA
 */
package org.hibernate.test.event.collection.association.bidirectional.onetomany;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;

import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.event.collection.Child;
import org.hibernate.test.event.collection.ParentWithCollection;
import org.hibernate.test.event.collection.association.AbstractAssociationCollectionEventTest;
import org.hibernate.test.event.collection.association.bidirectional.onetomany.ChildWithManyToOne;
import org.hibernate.test.event.collection.association.bidirectional.onetomany.ParentWithBidirectionalOneToMany;

/**
 *
 * @author Gail Badner
 */
public class BidirectionalOneToManyBagSubclassCollectionEventTest extends BidirectionalOneToManyBagCollectionEventTest {

	public BidirectionalOneToManyBagSubclassCollectionEventTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( BidirectionalOneToManyBagSubclassCollectionEventTest.class );
	}

	public String[] getMappings() {
		return new String[] { "event/collection/association/bidirectional/onetomany/BidirectionalOneToManyBagSubclassMapping.hbm.xml" };
	}

	public ParentWithCollection createParent(String name) {
		return new ParentWithBidirectionalOneToManySubclass( name );
	}
}