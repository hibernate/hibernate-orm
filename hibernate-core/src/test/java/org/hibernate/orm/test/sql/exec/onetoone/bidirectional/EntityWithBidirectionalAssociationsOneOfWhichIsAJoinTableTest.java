/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec.onetoone.bidirectional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalAssociationsOneOfWhichIsAJoinTableTest.Female;
import static org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalAssociationsOneOfWhichIsAJoinTableTest.Male;
import static org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalAssociationsOneOfWhichIsAJoinTableTest.Parent;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				Parent.class,
				Male.class,
				Female.class,
		}
)
@ServiceRegistry
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class EntityWithBidirectionalAssociationsOneOfWhichIsAJoinTableTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( 1, "Hibernate" );

					Male son = new Male( 2, parent );
					son.setName( "Luigi" );

					Female daughter = new Female( 3, parent );
					daughter.setName( "Fab" );

					session.save( parent );
					session.save( son );
					session.save( daughter );
				} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Parent" ).executeUpdate();
					session.createQuery( "delete from Male" ).executeUpdate();
					session.createQuery( "delete from Female" ).executeUpdate();
				} );
	}

	@Test
	public void testGetParent(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final Parent parent = session.get( Parent.class, 1 );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 6 );
					Male son = parent.getSon();
					assertThat( son, CoreMatchers.notNullValue() );
					assertTrue(
							Hibernate.isInitialized( son ),
							"The son eager OneToOne association is not initialized"
					);
					assertThat( son.getName(), equalTo( "Luigi" ) );
					assertSame( son.getParent(), parent );

					Female daughter = parent.getDaughter();
					assertThat( daughter, CoreMatchers.notNullValue() );
					assertTrue(
							Hibernate.isInitialized( daughter ),
							"The daughter eager OneToOne association is not initialized"
					);
					assertThat( daughter.getName(), equalTo( "Fab" ) );
					assertSame( daughter.getParent(), parent );
					statementInspector.assertExecutedCount( 1 );
				} );
	}

	@Test
	public void testGetChild(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Male son = session.get( Male.class, 2 );
			statementInspector.assertExecutedCount( 1 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 6 );
			Parent parent = son.getParent();
			assertThat( parent, CoreMatchers.notNullValue() );
			assertTrue(
					Hibernate.isInitialized( parent ),
					"The parent eager OneToOne association is not initialized"
			);
			assertThat( parent.getDescription(), CoreMatchers.notNullValue() );

			assertTrue(
					Hibernate.isInitialized( parent.getSon() ),
					"The son eager OneToOne association is not initialized"
			);
			assertSame( parent.getSon(), son );

			Female daughter = parent.getDaughter();
			assertThat( daughter, CoreMatchers.notNullValue() );
			assertTrue(
					Hibernate.isInitialized( daughter ),
					"The child2 eager OneToOne association is not initialized"

			);
			assertThat( daughter.getParent(), CoreMatchers.notNullValue() );
			statementInspector.assertExecutedCount( 1 );
		} );
	}

	@Test
	public void testHqlSelectSon(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final String queryString = "SELECT m FROM Male m JOIN m.parent d WHERE d.id = :id";
					final Male son = session.createQuery( queryString, Male.class )
							.setParameter( "id", 1 )
							.getSingleResult();

					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 6 );
					assertThat( son.getParent(), CoreMatchers.notNullValue() );

					String description = son.getParent().getDescription();
					assertThat( description, CoreMatchers.notNullValue() );
				}
		);
	}

	@Test
	public void testHqlSelectParent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
					statementInspector.clear();
					final Parent parent = session.createQuery(
							"SELECT p FROM Parent p JOIN p.son WHERE p.id = :id",
							Parent.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 6 );
					Male son = parent.getSon();
					assertThat( son, CoreMatchers.notNullValue() );
					assertTrue(
							Hibernate.isInitialized( son ),
							"the son have to be initialized"
					);
					String name = son.getName();
					assertThat( name, CoreMatchers.notNullValue() );
				}

		);

		scope.inTransaction(
				session -> {
					final Parent parent = session.createQuery(
							"SELECT p FROM Parent p JOIN p.son WHERE p.id = :id",
							Parent.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();

					Male son = parent.getSon();
					assertThat( son, CoreMatchers.notNullValue() );
					assertTrue(
							Hibernate.isInitialized( son ),
							"The son have to be initialized"
					);
					String name = son.getName();
					assertThat( name, CoreMatchers.notNullValue() );
				}

		);
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	public static class Parent {
		private Integer id;

		private String description;
		private Male son;
		private Female daughter;

		Parent() {
		}

		public Parent(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@OneToOne
		@JoinTable(name = "PARENT_SON", inverseJoinColumns = @JoinColumn(name = "son_id"), joinColumns = @JoinColumn(name = "parent_id"))
		public Male getSon() {
			return son;
		}

		public void setSon(Male son) {
			this.son = son;
		}

		@OneToOne
		public Female getDaughter() {
			return daughter;
		}

		public void setDaughter(Female daughter) {
			this.daughter = daughter;
		}
	}

	@Entity(name = "Male")
	@Table(name = "MALE")
	public static class Male {
		private Integer id;

		private String name;
		private Parent parent;

		Male() {
		}

		Male(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
			this.parent.setSon( this );
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToOne(mappedBy = "son")
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Female")
	@Table(name = "FEMALE")
	public static class Female {
		private Integer id;

		private String name;
		private Parent parent;

		Female() {
		}

		Female(Integer id, Parent child) {
			this.id = id;
			this.parent = child;
			this.parent.setDaughter( this );
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToOne(mappedBy = "daughter")
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
