/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetomany;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.orm.test.mapping.onetomany.OneToManyTest.Card;
import org.hibernate.orm.test.mapping.onetomany.OneToManyTest.CardField;
import org.hibernate.orm.test.mapping.onetomany.OneToManyTest.Key;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				Card.class,
				CardField.class,
				Key.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class OneToManyTest {
	public static final String CARD_ID = "cardId";
	public static final String CARD_FIELD_ID = "cardFieldId";
	public static final String KEY_ID = "keyId";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Card card = new Card( CARD_ID );
					Key key = new Key( KEY_ID );
					card.addField( CARD_FIELD_ID, card, key );
					session.persist( key );
					session.persist( card );
				}
		);
	}

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					try {
						Card card = session.find( Card.class, CARD_ID );
						assertEquals( 1, card.getFields().size() );

						CardField cf = card.getFields().iterator().next();
						assertSame( card, cf.getCard() );

					}
					catch (StackOverflowError soe) {
						fail( "eager + key-many-to-one caused stack-overflow in annotations" );
					}
				}
		);
	}

	@Entity(name = "Card")
	public static class Card implements Serializable {
		@Id
		private String id;

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		private Set<CardField> fields;

		Card() {
			fields = new HashSet<>();
		}

		public Card(String id) {
			this();
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void addField(String cardFieldId, Card card, Key key) {
			fields.add( new CardField( cardFieldId, card, key ) );
		}

		public Set<CardField> getFields() {
			return fields;
		}

		public void setFields(Set<CardField> fields) {
			this.fields = fields;
		}
	}

	@Entity(name = "CardField")
	public static class CardField implements Serializable {

		@Id
		private String id;

		@ManyToOne(optional = false)
		private Card card;

		@ManyToOne(optional = false)
		private Key key;

		CardField() {

		}

		public CardField(String id, Card card, Key key) {
			this.id = id;
			this.card = card;
			this.key = key;
		}

		public Card getCard() {
			return card;
		}
	}

	@Entity(name = "Key")
	@Table(name = "`key`")
	public static class Key implements Serializable {
		@Id
		private String id;

		public Key(String id) {
			this.id = id;
		}

		Key() {
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}


}
