/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = { CompareEntityValuedPathsTest.Person.class }
)
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey(value = "HHH-15349")
public class CompareEntityValuedPathsTest {

	@Test
	public void testCompareOneToManyUK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p where p in (select c from p.childrenUk c)" ).list();

					// Ensure that there are no joins and we compare by UK
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"where p1_0.uk in (" +
									"select cu1_0.child_uk " +
									"from children_uks cu1_0 " +
									"where p1_0.uk=cu1_0.owner_uk" +
									")",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testCompareOneToManyPK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p where p in (select c from p.children c)" ).list();

					// Ensure that there are no joins and we compare by PK
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"where p1_0.id in (" +
									"select c1_0.children_id " +
									"from PERSON_TABLE_PERSON_TABLE c1_0 " +
									"where p1_0.id=c1_0.Person_id" +
									")",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testManyToOneIsNull(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p where p.parent is null" ).list();

					// Comparing a PK-ManyToOne against a UK-CollectionPart will force comparing by PK
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"where p1_0.parent_id is null",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testComparePKWithOneToManyUK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p where p.parent in (select c from p.childrenUk c)" ).list();

					// Comparing a PK-ManyToOne against a UK-CollectionPart will force comparing by PK
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"where p1_0.parent_id in (" +
									"select cu1_1.id " +
									"from children_uks cu1_0 " +
									"join PERSON_TABLE cu1_1 on cu1_1.uk=cu1_0.child_uk " +
									"where p1_0.uk=cu1_0.owner_uk" +
									")",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testCompareUKWithOneToManyPK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p where p.parentUk in (select c from p.children c)" ).list();

					// Comparing a UK-ManyToOne against a PK-CollectionPart will force comparing by PK
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"join PERSON_TABLE pu1_0 on pu1_0.uk=p1_0.parent_uk " +
									"where pu1_0.id in (" +
									"select c1_0.children_id " +
									"from PERSON_TABLE_PERSON_TABLE c1_0 " +
									"where p1_0.id=c1_0.Person_id" +
									")",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testComparePKWithMappedByOneToManyPK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p where p in (select c from p.employees c)" ).list();

					// Comparing a root against a PK-one-to-many CollectionPart allows comparing by FK
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"where p1_0.id in (" +
									"select e1_0.id " +
									"from PERSON_TABLE e1_0 " +
									"where p1_0.id=e1_0.supervisor_id" +
									")",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testComparePKWithMappedByOneToManyUK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p where p in (select c from p.employeesUk c)" ).list();

					// Comparing a root against a UK-one-to-many CollectionPart forces comparing by PK
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"where p1_0.id in (" +
									"select eu1_0.id " +
									"from PERSON_TABLE eu1_0 " +
									"where p1_0.uk=eu1_0.supervisor_uk" +
									")",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testOneToManyUKIsNotNull(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p join p.childrenUk c where c is not null" ).list();

					// Assert that we don't join the childrenUk target table, only the join table
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"join children_uks cu1_0 on p1_0.uk=cu1_0.owner_uk " +
									"where cu1_0.child_uk is not null",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testOneToManyPKIsNotNull(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p join p.children c where c is not null" ).list();

					// Assert that we don't join the children target table, only the join table
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"join PERSON_TABLE_PERSON_TABLE c1_0 on p1_0.id=c1_0.Person_id " +
									"where c1_0.children_id is not null",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testCompareOneToManyUKWithOneToManyPK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p join p.childrenUk c join p.children c2 where c = c2" ).list();

					// Assert that we don't join the childrenUk target table, only the join table
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"join children_uks cu1_0 on p1_0.uk=cu1_0.owner_uk join PERSON_TABLE cu1_1 on cu1_1.uk=cu1_0.child_uk " +
									"join PERSON_TABLE_PERSON_TABLE c1_0 on p1_0.id=c1_0.Person_id " +
									"where cu1_1.id=c1_0.children_id",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testCompareOneToManyPKWithOneToManyUK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p join p.children c join p.childrenUk c2 where c = c2" ).list();

					// Assert that we don't join the children target table, only the join table
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"join PERSON_TABLE_PERSON_TABLE c1_0 on p1_0.id=c1_0.Person_id " +
									"join children_uks cu1_0 on p1_0.uk=cu1_0.owner_uk join PERSON_TABLE cu1_1 on cu1_1.uk=cu1_0.child_uk " +
									"where c1_0.children_id=cu1_1.id",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testCompareOneToManyUKWithSubqueryOneToManyPK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p join p.childrenUk c where c in (select c2 from p.children c2)" ).list();

					// Assert that we don't join the childrenUk target table, only the join table
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"join children_uks cu1_0 on p1_0.uk=cu1_0.owner_uk join PERSON_TABLE cu1_1 on cu1_1.uk=cu1_0.child_uk " +
									"where cu1_1.id in (select c1_0.children_id from PERSON_TABLE_PERSON_TABLE c1_0 where p1_0.id=c1_0.Person_id)",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testCompareOneToManyPKWithSubqueryOneToManyUK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p join p.children c where c in (select c2 from p.childrenUk c2)" ).list();

					// Assert that we don't join the children target table, only the join table
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"join PERSON_TABLE_PERSON_TABLE c1_0 on p1_0.id=c1_0.Person_id " +
									"where c1_0.children_id in (select cu1_1.id from children_uks cu1_0 join PERSON_TABLE cu1_1 on cu1_1.uk=cu1_0.child_uk where p1_0.uk=cu1_0.owner_uk)",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testCompareManyToOneUK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p where p in (select p2.parentUk from Person p2)" ).list();

					// Ensure that there are no joins and we compare by UK
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"where p1_0.uk in (" +
									"select p2_0.parent_uk " +
									"from PERSON_TABLE p2_0" +
									")",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	public void testCompareManyToOnePK(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createQuery( "select 1 from Person p where p in (select p2.parent from Person p2)" ).list();

					// Ensure that there are no joins and we compare by PK
					assertEquals(
							"select " +
									"1 " +
									"from PERSON_TABLE p1_0 " +
									"where p1_0.id in (" +
									"select p2_0.parent_id " +
									"from PERSON_TABLE p2_0" +
									")",
							statementInspector.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		private Long id;

		@Column(unique = true)
		private String uk;
		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		private Person parent;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_uk", referencedColumnName = "uk")
		private Person parentUk;
		@ManyToOne(fetch = FetchType.LAZY)
		private Person supervisor;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "supervisor_uk", referencedColumnName = "uk")
		private Person supervisorUk;
		@OneToMany
		private Set<Person> children;
		@OneToMany
		@JoinTable(name = "children_uks",
				joinColumns = @JoinColumn(name = "owner_uk", referencedColumnName = "uk"),
				inverseJoinColumns = @JoinColumn(name = "child_uk", referencedColumnName = "uk")
		)
		private Set<Person> childrenUk;
		@OneToMany(mappedBy = "supervisor")
		private Set<Person> employees;
		@OneToMany(mappedBy = "supervisorUk")
		private Set<Person> employeesUk;

		public Person() {
		}
	}
}
