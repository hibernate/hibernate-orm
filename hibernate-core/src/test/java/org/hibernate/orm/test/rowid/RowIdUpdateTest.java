/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.rowid;

import org.hibernate.annotations.RowId;
import org.hibernate.dialect.Dialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = { RowIdUpdateTest.SimpleEntity.class, RowIdUpdateTest.ParentEntity.class } )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17045" )
public class RowIdUpdateTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SimpleEntity( 1L, "initial_status" ) );
			session.persist( new ParentEntity( 2L, new SimpleEntity( 2L, "initial_status" ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ParentEntity" ).executeUpdate();
			session.createMutationQuery( "delete from SimpleEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testSimpleUpdateSameTransaction(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final SimpleEntity simpleEntity = new SimpleEntity( 3L, "initial_status" );
			session.persist( simpleEntity );
			session.flush();
			simpleEntity.setStatus( "new_status" );
			inspector.clear();
		} );
		// the update should have used the primary key, as the row-id value is not available
		checkUpdateQuery( inspector, true );
		scope.inTransaction( session -> assertThat(
				session.find( SimpleEntity.class, 3L ).getStatus()
		).isEqualTo( "new_status" ) );
	}

	@Test
	public void testRelatedUpdateSameTransaction(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final ParentEntity parent = new ParentEntity(
					4L,
					new SimpleEntity(
							4L,
							"initial_status"
					)
			);
			session.persist( parent );
			session.flush();
			parent.getChild().setStatus( "new_status" );
			inspector.clear();
		} );
		// the update should have used the primary key, as the row-id value is not available
		checkUpdateQuery( inspector, true );
		scope.inTransaction( session -> assertThat(
				session.find( SimpleEntity.class, 4L ).getStatus()
		).isEqualTo( "new_status" ) );
	}

	@Test
	public void testSimpleUpdateDifferentTransaction(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final SimpleEntity simpleEntity = session.find( SimpleEntity.class, 1L );
			simpleEntity.setStatus( "new_status" );
			inspector.clear();
		} );
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		checkUpdateQuery( inspector, dialect.rowId( "" ) == null );
		scope.inTransaction( session -> assertThat(
				session.find( SimpleEntity.class, 1L ).getStatus()
		).isEqualTo( "new_status" ) );
	}

	@Test
	public void testRelatedUpdateRelatedDifferentTransaction(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ParentEntity parent = session.find( ParentEntity.class, 2L );
			parent.getChild().setStatus( "new_status" );
			inspector.clear();
		} );
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		checkUpdateQuery( inspector, dialect.rowId( "" ) == null );
		scope.inTransaction( session -> assertThat(
				session.find( SimpleEntity.class, 2L ).getStatus()
		).isEqualTo( "new_status" ) );
	}

	private void checkUpdateQuery(SQLStatementInspector inspector, boolean shouldUsePrimaryKey) {
		inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "primary_key", shouldUsePrimaryKey ? 1 : 0 );
	}

	@Entity( name = "SimpleEntity" )
	@RowId
	public static class SimpleEntity {
		@Id
		@Column( name = "primary_key" )
		public Long primaryKey;

		public String status;

		public SimpleEntity() {
		}

		public SimpleEntity(Long primaryKey, String status) {
			this.primaryKey = primaryKey;
			this.status = status;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}

	@Entity( name = "ParentEntity" )
	public static class ParentEntity {
		@Id
		public Long id;

		@OneToOne( cascade = CascadeType.ALL )
		@MapsId
		public SimpleEntity child;

		public ParentEntity() {
		}

		public ParentEntity(Long id, SimpleEntity child) {
			this.id = id;
			this.child = child;
		}

		public SimpleEntity getChild() {
			return child;
		}
	}
}
