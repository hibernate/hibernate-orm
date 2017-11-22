/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.process;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gail Badner
 */
public class EncapsulatedCompositeIdResultSetProcessorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class, CardField.class, Card.class };
	}

	@Test
	public void testSimpleCompositeId() throws Exception {

		// create some test data
		Session session = openSession();
		session.beginTransaction();
		Parent parent = new Parent();
		parent.id = new ParentPK();
		parent.id.firstName = "Joe";
		parent.id.lastName = "Blow";
		session.save( parent );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Parent parentGotten = (Parent) session.get( Parent.class, parent.id );
		assertEquals( parent, parentGotten );
		session.getTransaction().commit();
		session.close();

		final List results = getResults(
				sessionFactory().getEntityPersister( Parent.class.getName() ),
				new Callback() {
					@Override
					public void bind(PreparedStatement ps) throws SQLException {
						ps.setString( 1, "Joe" );
						ps.setString( 2, "Blow" );
					}

					@Override
					public QueryParameters getQueryParameters() {
						return new QueryParameters();
					}

				}
		);
		assertEquals( 1, results.size() );
		Object result = results.get( 0 );
		assertNotNull( result );

		Parent parentWork = ExtraAssertions.assertTyping( Parent.class, result );
		assertEquals( parent, parentWork );

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete Parent" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testCompositeIdWithKeyManyToOne() throws Exception {
		final String cardId = "ace-of-spades";

		// create some test data
		Session session = openSession();
		session.beginTransaction();
		Card card = new Card( cardId );
		final CardField cardField = new CardField( card, 1 );
		session.persist( card );
		session.persist( cardField );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Card cardProxy = (Card) session.load( Card.class, cardId );
		final CardFieldPK cardFieldPK = new CardFieldPK( cardProxy, 1 );
		CardField cardFieldGotten = (CardField) session.get( CardField.class, cardFieldPK );

		//assertEquals( card, cardGotten );
		session.getTransaction().commit();
		session.close();

		final EntityPersister entityPersister = sessionFactory().getEntityPersister( CardField.class.getName() );

		final List results = getResults(
				entityPersister,
				new Callback() {
					@Override
					public void bind(PreparedStatement ps) throws SQLException {
						ps.setString( 1, cardField.primaryKey.card.id );
						ps.setInt( 2, cardField.primaryKey.fieldNumber );
					}

					@Override
					public QueryParameters getQueryParameters() {
						QueryParameters qp = new QueryParameters();
						qp.setPositionalParameterTypes( new Type[] { entityPersister.getIdentifierType() } );
						qp.setPositionalParameterValues( new Object[] { cardFieldPK } );
						qp.setOptionalObject( null );
						qp.setOptionalEntityName( entityPersister.getEntityName() );
						qp.setOptionalId( cardFieldPK );
						qp.setLockOptions( LockOptions.NONE );
						return qp;
					}

				}
		);
		assertEquals( 1, results.size() );
		Object result = results.get( 0 );
		assertNotNull( result );

		CardField cardFieldWork = ExtraAssertions.assertTyping( CardField.class, result );
		assertEquals( cardFieldGotten, cardFieldWork );

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete CardField" ).executeUpdate();
		session.createQuery( "delete Card" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	private List getResults(final EntityPersister entityPersister, final Callback callback) {
		final LoadPlan plan = Helper.INSTANCE.buildLoadPlan( sessionFactory(), entityPersister );

		final LoadQueryDetails queryDetails = Helper.INSTANCE.buildLoadQueryDetails( plan, sessionFactory() );
		final String sql = queryDetails.getSqlStatement();
		final ResultSetProcessor resultSetProcessor = queryDetails.getResultSetProcessor();

		final List results = new ArrayList();

		final Session workSession = openSession();
		workSession.beginTransaction();
		workSession.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement( sql );
						callback.bind( ps );
						ResultSet resultSet = ps.executeQuery();
						//callback.beforeExtractResults( workSession );
						results.addAll(
								resultSetProcessor.extractResults(
										resultSet,
										(SessionImplementor) workSession,
										callback.getQueryParameters(),
										Helper.parameterContext(),
										true,
										false,
										null,
										null
								)
						);
						resultSet.close();
						ps.close();
					}
				}
		);
		workSession.getTransaction().commit();
		workSession.close();

		return results;
	}


	private interface Callback {
		void bind(PreparedStatement ps) throws SQLException;
		QueryParameters getQueryParameters ();
	}

	@Entity ( name = "Parent" )
	public static class Parent {
		@EmbeddedId
		public ParentPK id;

		public boolean equals(Object o) {
			if ( this == o ) return true;
			if ( !( o instanceof Parent ) ) return false;

			final Parent parent = (Parent) o;

			if ( !id.equals( parent.id ) ) return false;

			return true;
		}

		public int hashCode() {
			return id.hashCode();
		}
	}

	@Embeddable
	public static class ParentPK implements Serializable {
		private String firstName;
		private String lastName;

		public boolean equals(Object o) {
			if ( this == o ) return true;
			if ( !( o instanceof ParentPK ) ) return false;

			final ParentPK parentPk = (ParentPK) o;

			if ( !firstName.equals( parentPk.firstName ) ) return false;
			if ( !lastName.equals( parentPk.lastName ) ) return false;

			return true;
		}

		public int hashCode() {
			int result;
			result = firstName.hashCode();
			result = 29 * result + lastName.hashCode();
			return result;
		}
	}

	@Entity ( name = "CardField" )
	public static class CardField implements Serializable {

		@EmbeddedId
		private CardFieldPK primaryKey;

		CardField(Card card, int fieldNumber) {
			this.primaryKey = new CardFieldPK(card, fieldNumber);
		}

		CardField() {
		}

		public CardFieldPK getPrimaryKey() {
			return primaryKey;
		}

		public void setPrimaryKey(CardFieldPK primaryKey) {
			this.primaryKey = primaryKey;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			CardField cardField = (CardField) o;

			if ( primaryKey != null ? !primaryKey.equals( cardField.primaryKey ) : cardField.primaryKey != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			return primaryKey != null ? primaryKey.hashCode() : 0;
		}
	}

	@Embeddable
	public static class CardFieldPK implements Serializable {
		@ManyToOne(optional = false)
		private Card card;

		private int fieldNumber;

		public CardFieldPK(Card card, int fieldNumber) {
			this.card = card;
			this.fieldNumber = fieldNumber;
		}

		CardFieldPK() {
		}

		public Card getCard() {
			return card;
		}

		public void setCard(Card card) {
			this.card = card;
		}

		public int getFieldNumber() {
			return fieldNumber;
		}

		public void setFieldNumber(int fieldNumber) {
			this.fieldNumber = fieldNumber;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			CardFieldPK that = (CardFieldPK) o;

			if ( fieldNumber != that.fieldNumber ) {
				return false;
			}
			if ( card != null ? !card.equals( that.card ) : that.card != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = card != null ? card.hashCode() : 0;
			result = 31 * result + fieldNumber;
			return result;
		}
	}

	@Entity ( name = "Card" )
	public static class Card implements Serializable {
		@Id
		private String id;

		public Card(String id) {
			this();
			this.id = id;
		}

		Card() {
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Card card = (Card) o;

			if ( !id.equals( card.id ) ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}
}
