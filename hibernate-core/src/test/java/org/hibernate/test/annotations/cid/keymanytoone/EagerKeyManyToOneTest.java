/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.cid.keymanytoone;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class EagerKeyManyToOneTest extends BaseCoreFunctionalTestCase {
	public static final String CARD_ID = "cardId";
	public static final String KEY_ID = "keyId";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Card.class, CardField.class, Key.class, PrimaryKey.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4147" )
	public void testLoadEntityWithEagerFetchingToKeyManyToOneReferenceBackToSelf() {
		// based on the core testsuite test of same name in org.hibernate.test.keymanytoone.bidir.component.EagerKeyManyToOneTest
		// meant to test against regression relating to http://opensource.atlassian.com/projects/hibernate/browse/HHH-2277
		// and http://opensource.atlassian.com/projects/hibernate/browse/HHH-4147

		{
			Session s = openSession();
			s.beginTransaction();
			Card card = new Card( CARD_ID );
			Key key = new Key( KEY_ID );
			card.addField( card, key );
			s.persist( key );
			s.persist( card );
			s.getTransaction().commit();
			s.close();
		}

		{
			Session s = openSession();
			s.beginTransaction();
			try {
				Card card = (Card) s.get( Card.class, CARD_ID );
				assertEquals( 1, card.getFields().size() );
				CardField cf = card.getFields().iterator().next();
				assertSame( card, cf.getPrimaryKey().getCard() );
			}
			catch ( StackOverflowError soe ) {
				fail( "eager + key-many-to-one caused stack-overflow in annotations" );
			}
			finally {
				s.getTransaction().commit();
				s.close();
			}
		}

		{
			Session s = openSession();
			s.beginTransaction();
			Card card = (Card) s.get( Card.class, CARD_ID );
			Key key = (Key) s.get( Key.class, KEY_ID );
			s.delete( card );
			s.delete( key );
			s.getTransaction().commit();
			s.close();
		}
	}
}
