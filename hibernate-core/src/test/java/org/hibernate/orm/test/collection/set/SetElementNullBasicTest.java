/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.set;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-11881")
public class SetElementNullBasicTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Test
	public void testPersistNullValue() {
		int entityId = doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( null );
					session.persist( e );
					return e.id;
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					session.delete( e );
				}
		);
	}

	@Test
	public void addNullValue() {
		int entityId = doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = new AnEntity();
					session.persist( e );
					return e.id;
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					e.aCollection.add( null );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					session.delete( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNull() {
		int entityId = doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( "def" );
					session.persist( e );
					return e.id;
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId ) );
					e.aCollection.remove( "def" );
					e.aCollection.add( null );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					session.delete( e );
				}
		);
	}

	private int getCollectionElementRows(int id) {
		return doInHibernate(
				this::sessionFactory, session -> {
					// todo (6.0) : use native query when native queries will be implemented
					return session.doReturningWork(
							work -> {
								PreparedStatement statement = null;
								ResultSet resultSet = null;

								try {
									statement = work.prepareStatement(
											"SELECT count(aCollection) as numberOfRows FROM AnEntity_aCollection where AnEntity_id = " + id );
									statement.execute();
									resultSet = statement.getResultSet();
									if ( resultSet.next() ) {
										return resultSet.getInt( "numberOfRows" );
									}
									return 0;
								}
								finally {
									if ( resultSet != null && !resultSet.isClosed() ) {
										resultSet.close();
									}
									if ( statement != null && !statement.isClosed() ) {
										statement.close();
									}
								}
							}
					);
				}
		);
	}

	@Entity
	@Table(name = "AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;

		@ElementCollection
		@CollectionTable(name = "AnEntity_aCollection", joinColumns = { @JoinColumn(name = "AnEntity_id") })
		private Set<String> aCollection = new HashSet<>();
	}
}
