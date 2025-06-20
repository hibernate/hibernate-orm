/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid.keymanytoone.association;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				Card.class, CardField.class, Key.class
		})
@SessionFactory(useCollectingStatementInspector = true)
public class EagerKeyManyToOneTest {
	public static final String CARD_ID = "cardId";
	public static final String KEY_ID = "keyId";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Card card = new Card( CARD_ID );
					Key key = new Key( KEY_ID );
					CardField field = new CardField( card, key );
					card.setField( field );

					session.persist( key );
					session.persist( card );

					session.persist( field );
				}
		);
	}

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLoadEntityWithEagerFetchingToKeyManyToOneReferenceBackToSelf(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					try {
						Card card = session.get( Card.class, CARD_ID );

						CardField cf = card.getField();
						assertSame( card, cf.getPrimaryKey().getCard() );

						statementInspector.assertExecutedCount( 1 );
						statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
					}
					catch (StackOverflowError soe) {
						fail( "eager + key-many-to-one caused stack-overflow in annotations" );
					}
				}
		);
	}
}
