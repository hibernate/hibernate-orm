/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ql;

import java.util.Set;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = {
				JoinTableOptimizationTest.Document.class,
				JoinTableOptimizationTest.Person.class,
				JoinTableOptimizationTest.File.class,
				JoinTableOptimizationTest.Picture.class
		})
@SessionFactory(useCollectingStatementInspector = true)
public class JoinTableOptimizationTest {

	@Test
	@JiraKey("HHH-16691")
	public void testOnlyCollectionTableJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.id from Document d left join d.people p where p.id is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert only the collection table is joined
					statementInspector.assertNumberOfJoins( 0, 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-16691")
	public void testInnerJoin(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.name from Document d join d.people p" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_1.name " +
									"from Document d1_0 " +
									"join people p1_0 on d1_0.id=p1_0.Document_id " +
									"join Person p1_1 on p1_1.id=p1_0.people_id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-16691")
	public void testLeftJoin(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.name from Document d left join d.people p" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_1.name " +
									"from Document d1_0 " +
									"left join people p1_0 on d1_0.id=p1_0.Document_id " +
									"left join Person p1_1 on p1_1.id=p1_0.people_id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-16691")
	public void testInnerJoinCustomOnClause(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.name from Document d join d.people p on p.id > 1" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_1.name " +
									"from Document d1_0 " +
									"join people p1_0 on d1_0.id=p1_0.Document_id and p1_0.people_id>1 " +
									"join Person p1_1 on p1_1.id=p1_0.people_id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-16691")
	public void testLeftJoinCustomOnClause(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.name from Document d left join d.people p on p.id > 1" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_1.name " +
									"from Document d1_0 " +
									"left join (people p1_0 " +
									"join Person p1_1 on p1_1.id=p1_0.people_id) on d1_0.id=p1_0.Document_id and p1_0.people_id>1",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was wrongly optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17830")
	public void testElementCollectionJoinCustomOnClause(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.text from Document d join d.pages p on p.text is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_0.text " +
									"from Document d1_0 " +
									"join document_pages p1_0 on d1_0.id=p1_0.Document_id and p1_0.text is not null",
							statementInspector.getSqlQueries().get( 0 ),
							"Join condition was wrongly removed"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17646")
	public void testLeftJoinFetch(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select d from Document d left join fetch d.people" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select d1_0.id,d1_0.name,p1_0.Document_id,p1_1.id,p1_1.name " +
									"from Document d1_0 " +
									"left join people p1_0 on d1_0.id=p1_0.Document_id " +
									"left join Person p1_1 on p1_1.id=p1_0.people_id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17646")
	public void testJoinTablePolymorphicJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select f from Document d join d.manyFiles f" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select mf1_1.id,case when mf1_2.id is not null then 1 when mf1_1.id is not null then 0 end,mf1_1.document_id,mf1_1.name,mf1_2.extension " +
									"from Document d1_0 " +
									"join many_files mf1_0 on d1_0.id=mf1_0.Document_id " +
									"join file_tbl mf1_1 on mf1_1.id=mf1_0.manyFiles_id " +
									"left join Picture mf1_2 on mf1_1.id=mf1_2.id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17646")
	public void testJoinTablePolymorphicLeftJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select f from Document d left join d.manyFiles f" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select mf1_1.id,case when mf1_2.id is not null then 1 when mf1_1.id is not null then 0 end,mf1_1.document_id,mf1_1.name,mf1_2.extension " +
									"from Document d1_0 " +
									"left join many_files mf1_0 on d1_0.id=mf1_0.Document_id " +
									"left join file_tbl mf1_1 on mf1_1.id=mf1_0.manyFiles_id " +
									"left join Picture mf1_2 on mf1_1.id=mf1_2.id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17646")
	public void testJoinPolymorphicJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select f from Document d join d.files f" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select f1_0.id,case when f1_1.id is not null then 1 when f1_0.id is not null then 0 end,d1_0.id,d1_0.name,f1_0.name,f1_1.extension " +
									"from Document d1_0 " +
									"join file_tbl f1_0 on d1_0.id=f1_0.document_id " +
									"left join Picture f1_1 on f1_0.id=f1_1.id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17646")
	public void testJoinPolymorphicLeftJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select f from Document d left join d.files f" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select f1_0.id,case when f1_1.id is not null then 1 when f1_0.id is not null then 0 end,d1_0.id,d1_0.name,f1_0.name,f1_1.extension " +
									"from Document d1_0 " +
									"left join file_tbl f1_0 on d1_0.id=f1_0.document_id " +
									"left join Picture f1_1 on f1_0.id=f1_1.id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17646")
	public void testJoinTablePolymorphicSubtypeJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select f from Document d join d.manyPictures f" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select mp1_1.id,mp1_2.document_id,mp1_2.name,mp1_1.extension " +
									"from Document d1_0 " +
									"join many_pictures mp1_0 on d1_0.id=mp1_0.Document_id " +
									"join Picture mp1_1 on mp1_1.id=mp1_0.manyPictures_id " +
									"join file_tbl mp1_2 on mp1_1.id=mp1_2.id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17646")
	public void testJoinTablePolymorphicSubtypeLeftJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select f from Document d left join d.manyPictures f" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select mp1_1.id,mp1_2.document_id,mp1_2.name,mp1_1.extension " +
									"from Document d1_0 " +
									"left join many_pictures mp1_0 on d1_0.id=mp1_0.Document_id " +
									"left join Picture mp1_1 on mp1_1.id=mp1_0.manyPictures_id " +
									"left join file_tbl mp1_2 on mp1_1.id=mp1_2.id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17646")
	public void testJoinPolymorphicSubtypeJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select f from Document d join d.pictures f" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_0.id,p1_1.document_id,p1_1.name,p1_0.extension " +
									"from Document d1_0 " +
									"join file_tbl p1_1 on d1_0.id=p1_1.document_id " +
									"join Picture p1_0 on p1_0.id=p1_1.id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17646")
	public void testJoinPolymorphicSubtypeLeftJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select f from Document d left join d.pictures f" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_0.id,p1_1.document_id,p1_1.name,p1_0.extension " +
									"from Document d1_0 " +
									"left join file_tbl p1_1 on d1_0.id=p1_1.document_id " +
									"left join Picture p1_0 on p1_0.id=p1_1.id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Entity(name = "Document")
	public static class Document {
		@Id
		Long id;
		String name;
		@OneToMany
		@JoinTable(name = "people")
		Set<Person> people;
		@ElementCollection
		@CollectionTable(name = "document_pages")
		Set<Page> pages;
		@OneToMany(mappedBy = "document")
		Set<File> files;
		@ManyToMany
		@JoinTable(name = "many_files")
		Set<File> manyFiles;
		@OneToMany(mappedBy = "document")
		Set<Picture> pictures;
		@ManyToMany
		@JoinTable(name = "many_pictures")
		Set<Picture> manyPictures;
	}
	@Entity(name = "Person")
	public static class Person {
		@Id
		Long id;
		String name;
	}
	@Entity(name = "File")
	@Table(name = "file_tbl")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class File {
		@Id
		Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		Document document;
	}
	@Entity(name = "Picture")
	public static class Picture extends File {
		String extension;
	}
	@Embeddable
	public static class Page {
		String text;
	}
}
