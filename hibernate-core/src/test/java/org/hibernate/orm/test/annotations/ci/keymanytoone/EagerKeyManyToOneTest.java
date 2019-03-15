/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.ci.keymanytoone;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Disabled("We sql generated select for the Card entity is wrong ")
public class EagerKeyManyToOneTest extends SessionFactoryBasedFunctionalTest {
	public static final String CARD_ID = "cardId";
	public static final String KEY_ID = "keyId";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Card.class, CardField.class, Key.class, PrimaryKey.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4147")
	public void testLoadEntityWithEagerFetchingToKeyManyToOneReferenceBackToSelf() {
		// based on the core testsuite test of same name in org.hibernate.test.keymanytoone.bidir.component.EagerKeyManyToOneTest
		// meant to test against regression relating to http://opensource.atlassian.com/projects/hibernate/browse/HHH-2277
		// and http://opensource.atlassian.com/projects/hibernate/browse/HHH-4147

		inTransaction(
				session -> {
					Card card = new Card( CARD_ID );
					Key key = new Key( KEY_ID );
					card.addField( card, key );
					session.persist( key );
					session.persist( card );
				}
		);

		inTransaction(
				session -> {
					try {
						Card card = session.get( Card.class, CARD_ID );
						assertEquals( 1, card.getFields().size() );
						CardField cf = card.getFields().iterator().next();
						assertSame( card, cf.getPrimaryKey().getCard() );
					}
					catch (StackOverflowError soe) {
						fail( "eager + key-many-to-one caused stack-overflow in annotations" );
					}
				}
		);

		inTransaction(
				session -> {
					Card card = session.get( Card.class, CARD_ID );
					Key key = session.get( Key.class, KEY_ID );
					session.delete( card );
					session.delete( key );
				}
		);

	}
}
