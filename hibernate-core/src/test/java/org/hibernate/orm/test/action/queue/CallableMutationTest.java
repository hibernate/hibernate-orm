/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.event.spi.EventType;
import org.hibernate.exception.DataException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.jdbc.Expectation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		CallableMutationTest.Item.class, CallableMutationTest.Parent.class, CallableMutationTest.InvalidResultItem.class
})
@SessionFactory(generateStatistics = true)
@RequiresDialect(H2Dialect.class)
public class CallableMutationTest {

	@BeforeAll
	void createFunctions(SessionFactoryScope scope) {
		scope.getSessionFactory().getEventListenerRegistry().appendListeners(
				EventType.POST_COLLECTION_RECREATE, event -> ((Item) event.getAffectedOwnerOrNull()).collectionCreates++ );
		scope.inTransaction( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				for ( var method : new String[] { "insertItem", "updateItem", "deleteItem", "insertTag", "updateInvalidResult" } ) {
					statement.execute( "create alias " + method + " for \"" + getClass().getName() + "." + method + "\"" );
				}
			}
		} ) );
	}

	@AfterAll
	void dropFunctions(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				for ( var method : new String[] { "insertItem", "updateItem", "deleteItem", "insertTag", "updateInvalidResult" } ) {
					statement.execute( "drop alias if exists " + method );
				}
			}
		} ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 5 })
	void mutationsAndCallbacks(int batchSize, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setJdbcBatchSize( batchSize );
			final var parent = new Parent();
			session.persist( parent );
			final var item = new Item();
			item.parent = parent;
			item.tags.addAll( Set.of( "one", "two", "three" ) );
			session.persist( item );
			session.flush();
			assertEquals( 1, item.inserts );
			assertEquals( 1, item.collectionCreates );
			assertEquals( Set.of( "one", "two", "three" ),
					new HashSet<>( session.createNativeQuery( "select tag from callable_tags", String.class ).getResultList() ) );

			item.name = "changed";
			session.flush();
			assertEquals( 1, item.updates );
			assertEquals( 1, item.collectionCreates );
			assertEquals( 1, item.version );
			assertEquals( "changed", session.createNativeQuery( "select name from callable_item", String.class ).getSingleResult() );

			session.remove( item );
			session.flush();
			assertEquals( 1, item.deletes );
			session.clear();
			assertNull( session.find( Item.class, item.id ) );
		} );
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 5 })
	void invalidOutputCount(int batchSize, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new InvalidResultItem() ) );
		scope.inSession( session -> {
			final var transaction = session.beginTransaction();
			try {
				session.setJdbcBatchSize( batchSize );
				final var item = session.find( InvalidResultItem.class, 1L );
				item.name = "changed";
				final var exception = assertThrows( GenericJDBCException.class, session::flush );
				assertEquals( "Could not extract row count from CallableStatement", exception.getErrorMessage() );
				assertEquals( "22018", exception.getSQLException().getSQLState() );
				assertEquals( 0, item.updates );
			}
			finally {
				transaction.rollback();
			}
		} );
		scope.inTransaction( session -> assertEquals( "initial", session.find( InvalidResultItem.class, 1L ).name ) );
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 5 })
	void failedCollectionInsertSuppressesCompletion(int batchSize, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var transaction = session.beginTransaction();
			try {
				session.setJdbcBatchSize( batchSize );
				final var parent = new Parent();
				session.persist( parent );
				final var item = new Item();
				item.parent = parent;
				item.tags.add( "first" );
				item.tags.add( "longer than the tag column" );
				session.persist( item );
				assertThrows( DataException.class, session::flush );
				assertEquals( 1, item.inserts );
				assertEquals( 0, item.collectionCreates );
				session.doWork( connection -> {
					try ( var statement = connection.createStatement();
							var rows = statement.executeQuery( "select tag from callable_tags" ) ) {
						assertTrue( rows.next() );
						assertEquals( "first", rows.getString( 1 ) );
						assertFalse( rows.next() );
					}
				} );
			}
			finally {
				transaction.rollback();
			}
		} );
		scope.inTransaction( session -> {
			assertNull( session.find( Item.class, 1L ) );
			assertNull( session.find( Parent.class, 1L ) );
			assertEquals( 0L, session.createNativeQuery( "select count(*) from callable_tags", Long.class ).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 5 })
	void staleUpdate(int batchSize, SessionFactoryScope scope) {
		staleMutation( batchSize, false, scope );
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 5 })
	void staleDelete(int batchSize, SessionFactoryScope scope) {
		staleMutation( batchSize, true, scope );
	}

	private void staleMutation(int batchSize, boolean delete, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parent = new Parent();
			session.persist( parent );
			final var item = new Item();
			item.parent = parent;
			session.persist( item );
		} );
		scope.inSession( session -> {
			final var transaction = session.beginTransaction();
			try {
				session.setJdbcBatchSize( batchSize );
				final var item = session.find( Item.class, 1L );
				session.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						statement.executeUpdate( "update callable_item set version=version+1 where id=1" );
					}
				} );
				final var statistics = scope.getSessionFactory().getStatistics();
				statistics.clear();
				if ( delete ) {
					session.remove( item );
				}
				else {
					item.name = "stale";
				}
				final var exception = assertThrows( OptimisticLockException.class, session::flush );
				final var stale = assertInstanceOf( StaleObjectStateException.class, exception.getCause() );
				assertEquals( Item.class.getName(), stale.getEntityName() );
				assertEquals( 1L, stale.getIdentifier() );
				assertEquals( 0, item.updates );
				assertEquals( 0, item.deletes );
				assertEquals( 1, statistics.getOptimisticFailureCount() );
				assertEquals( 1, statistics.getEntityStatistics( Item.class.getName() ).getOptimisticFailureCount() );
			}
			finally {
				transaction.rollback();
			}
		} );
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1L );
			assertEquals( "initial", item.name );
			assertEquals( 0, item.version );
		} );
	}

	public static int insertItem(Connection connection, String name, long parent, int version, long id) throws SQLException {
		return execute( connection, "insert into callable_item(name,parent_id,version,id) values (?,?,?,?)", name, parent, version, id );
	}

	public static int updateItem(Connection connection, String name, long parent, int version, long id, int oldVersion) throws SQLException {
		return execute( connection, "update callable_item set name=?,parent_id=?,version=? where id=? and version=?", name, parent, version, id, oldVersion );
	}

	public static int deleteItem(Connection connection, long id, int version) throws SQLException {
		return execute( connection, "delete from callable_item where id=? and version=?", id, version );
	}

	public static int insertTag(Connection connection, long id, String tag) throws SQLException {
		return execute( connection, "insert into callable_tags(item_id,tag) values (?,?)", id, tag );
	}

	public static String updateInvalidResult(Connection connection, String name, long id) throws SQLException {
		execute( connection, "update callable_invalid_result set name=? where id=?", name, id );
		return "not a row count";
	}

	private static int execute(Connection connection, String sql, Object... parameters) throws SQLException {
		try ( var statement = connection.prepareStatement( sql ) ) {
			for ( int i = 0; i < parameters.length; i++ ) {
				statement.setObject( i + 1, parameters[i] );
			}
			return statement.executeUpdate();
		}
	}

	@Entity(name = "CallableParent")
	@Table(name = "callable_parent")
	static class Parent {
		@Id Long id = 1L;
	}

	@Entity(name = "CallableItem")
	@Table(name = "callable_item")
	@SQLInsert(sql = "{?=call insertItem(?,?,?,?)}", callable = true, verify = Expectation.OutParameter.class)
	@SQLUpdate(sql = "{?=call updateItem(?,?,?,?,?)}", callable = true, verify = Expectation.OutParameter.class)
	@SQLDelete(sql = "{?=call deleteItem(?,?)}", callable = true, verify = Expectation.OutParameter.class)
	static class Item {
		@Id Long id = 1L;
		String name = "initial";
		@ManyToOne(optional = false) @JoinColumn(name = "parent_id") Parent parent;
		@Version int version;
		@Transient int inserts;
		@Transient int updates;
		@Transient int deletes;
		@Transient int collectionCreates;

		@ElementCollection
		@CollectionTable(name = "callable_tags", joinColumns = @JoinColumn(name = "item_id"))
		@Column(name = "tag", length = 10)
		@SQLInsert(sql = "{?=call insertTag(?,?)}", callable = true, verify = Expectation.OutParameter.class)
		Set<String> tags = new LinkedHashSet<>();

		@PostPersist void inserted() { inserts++; }
		@PostUpdate void updated() { updates++; }
		@PostRemove void deleted() { deletes++; }
	}

	@Entity(name = "CallableInvalidResultItem")
	@Table(name = "callable_invalid_result")
	@SQLUpdate(sql = "{?=call updateInvalidResult(?,?)}", callable = true, verify = Expectation.OutParameter.class)
	static class InvalidResultItem {
		@Id Long id = 1L;
		String name = "initial";
		@Transient int updates;

		@PostUpdate void updated() { updates++; }
	}
}
