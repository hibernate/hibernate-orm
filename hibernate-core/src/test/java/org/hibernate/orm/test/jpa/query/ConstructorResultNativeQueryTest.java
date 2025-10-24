/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(dialectClass = OracleDialect.class, matchSubTypes = true, reason = "https://hibernate.atlassian.net/browse/HHH-10323")
@Jpa(annotatedClasses = {ConstructorResultNativeQueryTest.Person.class})
public class ConstructorResultNativeQueryTest {
	@Entity( name = "Person" )
	@SqlResultSetMappings(
			value = {
					@SqlResultSetMapping(
							name = "person-id-and-name",
							classes = {
									@ConstructorResult(
											targetClass = Person.class,
											columns = {
													@ColumnResult( name = "id" ),
													@ColumnResult( name = "p_name" )
											}
									)
							}
					),
					@SqlResultSetMapping(
							name = "person-id-and-name2",
							classes = {
									@ConstructorResult(
											targetClass = Person.class,
											columns = {
													@ColumnResult( name = "id" ),
													@ColumnResult( name = "p_name" )
											}
									),
									@ConstructorResult(
											targetClass = Person.class,
											columns = {
													@ColumnResult( name = "id2" ),
													@ColumnResult( name = "p_name2" )
											}
									)
							}
					),
					@SqlResultSetMapping(
							name = "person-id-and-name-and-weight",
							classes = {
									@ConstructorResult(
											targetClass = Person.class,
											columns = {
													@ColumnResult( name = "id" ),
													@ColumnResult( name = "p_name" ),
													@ColumnResult( name = "p_weight", type=String.class )
											}
									)
							}
					)
			}
	)
	@NamedNativeQueries(
			value = {
				@NamedNativeQuery(
						name = "person-id-and-name",
						query = "select p.id, p.p_name from person p order by p.p_name",
						resultSetMapping = "person-id-and-name"
				),
				@NamedNativeQuery(
						name = "person-id-and-name2",
						query = "select p.id, p.p_name, p.id as id2, p.p_name as p_name2 from person p order by p.p_name",
						resultSetMapping = "person-id-and-name2"
				),
				@NamedNativeQuery(
						name = "person-id-and-name-and-weight",
						query = "select p.id, p.p_name, p.p_weight from person p order by p.p_name",
						resultSetMapping = "person-id-and-name-and-weight"
				)
			}
	)
	@Table(name = "person")
	public static class Person {
		@Id
		@Column(name = "id")
		private Integer id;
		@Column( name = "p_name" )
		private String name;
		@Temporal( TemporalType.TIMESTAMP )
		private Date birthDate;
		@Column( name = "p_weight" )
		private int weight;

		public Person() {
		}

		public Person(Integer id, String name, Date birthDate) {
			this.id = id;
			this.name = name;
			this.birthDate = birthDate;
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Person(Integer id, String name, String weight) {
			this.id = id;
			this.name = name;
			this.weight = Integer.valueOf(weight);
		}
	}

	@AfterEach
	public void cleanup(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testConstructorResultNativeQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Person( 1, "John", new Date() ) );
		} );

		scope.inTransaction( em -> {
			List results = em.createNativeQuery(
					"select p.id, p.p_name from person p order by p.p_name",
					"person-id-and-name"
			).getResultList();
			assertEquals( 1, results.size() );
			assertTyping( Person.class, results.get( 0 ) );
		} );
	}


	@Test
	public void testMultipleConstructorResultNativeQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Person( 1, "John", new Date() ) );
		} );

		scope.inTransaction( em -> {
			List results = em.createNamedQuery( "person-id-and-name2" ).getResultList();
			assertEquals( 1, results.size() );
			Object[] result = assertTyping( Object[].class, results.get( 0 ) );
			assertEquals( 2, result.length );
			assertTyping( Person.class, result[0] );
			assertTyping( Person.class, result[1] );
		} );
	}

	@Test
	public void testConstructorResultNativeQuerySpecifyingType(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Person( 1, "John", "85" ) );
		} );

		scope.inTransaction( em -> {
			List results = em.createNamedQuery( "person-id-and-name-and-weight" ).getResultList();
			assertEquals( 1, results.size() );
			assertTyping( Person.class, results.get( 0 ) );
		} );
	}
}
