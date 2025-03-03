/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.nulliteral;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;
import java.util.List;

import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * @author Andrea Boriero
 */
@Jpa(
		annotatedClasses = {CriteriaLiteralInSelectExpressionTest.MyEntity.class}
)
public class CriteriaLiteralInSelectExpressionTest {

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> entityManager.persist( new MyEntity( "Fab", "A" ) )
		);
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete from MyEntity" ).executeUpdate()
		);
	}

	@Test
	@JiraKey(value = "HHH-10729")
	public void testBooleanLiteral(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<MyEntityDTO> query = criteriaBuilder.createQuery( MyEntityDTO.class );
					final Root<MyEntity> entity = query.from( MyEntity.class );

					query.multiselect( criteriaBuilder.literal( false ), entity.get( "name" ) );

					final List<MyEntityDTO> dtos = entityManager.createQuery( query ).getResultList();

					assertThat( dtos.size(), is( 1 ) );
					assertThat( dtos.get( 0 ).active, is( false ) );
					assertThat( dtos.get( 0 ).name, is( "Fab" ) );
					assertThat( dtos.get( 0 ).surname, nullValue() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10861")
	public void testNullLiteral(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<MyEntityDTO> query = criteriaBuilder.createQuery( MyEntityDTO.class );
					final Root<MyEntity> entity = query.from( MyEntity.class );

					query.multiselect( criteriaBuilder.literal( false ), criteriaBuilder.nullLiteral( String.class ) );

					final List<MyEntityDTO> dtos = entityManager.createQuery( query ).getResultList();

					assertThat( dtos.size(), is( 1 ) );
					assertThat( dtos.get( 0 ).active, is( false ) );
					assertThat( dtos.get( 0 ).name, nullValue() );
					assertThat( dtos.get( 0 ).surname, nullValue() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10861")
	public void testNullLiteralFirst(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<MyEntityDTO> query = criteriaBuilder.createQuery( MyEntityDTO.class );
					final Root<MyEntity> entity = query.from( MyEntity.class );

					query.multiselect( criteriaBuilder.nullLiteral( String.class ), entity.get( "surname" ) );

					final List<MyEntityDTO> dtos = entityManager.createQuery( query ).getResultList();

					assertThat( dtos.size(), is( 1 ) );
					assertThat( dtos.get( 0 ).name, nullValue() );
					assertThat( dtos.get( 0 ).surname, is( "A" ) );
					assertThat( dtos.get( 0 ).active, is( false ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10729")
	public void testStringLiteral(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<MyEntityDTO> query = criteriaBuilder.createQuery( MyEntityDTO.class );
					final Root<MyEntity> entity = query.from( MyEntity.class );

					query.multiselect( criteriaBuilder.literal( "Leo" ), entity.get( "surname" ) );

					final List<MyEntityDTO> dtos = entityManager.createQuery( query ).getResultList();

					assertThat( dtos.size(), is( 1 ) );
					assertThat( dtos.get( 0 ).name, is( "Leo" ) );
					assertThat( dtos.get( 0 ).surname, is( "A" ) );
					assertThat( dtos.get( 0 ).active, is( false ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9021")
	@SkipForDialect( dialectClass = OracleDialect.class)
	@SkipForDialect( dialectClass = DB2Dialect.class)
	@SkipForDialect( dialectClass = SQLServerDialect.class)
	@SkipForDialect( dialectClass = SybaseDialect.class)
	@SkipForDialect( dialectClass = HANADialect.class)
	public void testStringLiteral2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Tuple> criteriaQuery = builder.createQuery( Tuple.class );
					criteriaQuery.from( MyEntity.class );
					criteriaQuery.multiselect( builder.equal( builder.literal( 1 ), builder.literal( 2 ) ) );

					final TypedQuery<Tuple> typedQuery = entityManager.createQuery( criteriaQuery );

					final List<Tuple> results = typedQuery.getResultList();

					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ).getElements().size(), is( 1 ) );
					assertThat( results.get( 0 ).get( 0 ), is( false ) );
				}
		);
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity implements Serializable {
		@Id
		@Column(name = "id")
		@GeneratedValue
		private Integer id;
		private String name;
		private String surname;

		public MyEntity() {
		}

		public MyEntity(String name, String surname) {
			this.name = name;
			this.surname = surname;
		}

		public MyEntity(String name, boolean surname) {
			this.name = name;
		}
	}

	public static class MyEntityDTO {
		private String name;
		private String surname;
		private boolean active;

		public MyEntityDTO() {
		}

		public MyEntityDTO(String name, String surname) {
			this.name = name;
			this.surname = surname;
		}

		public MyEntityDTO(boolean active, String name) {
			this.name = name;
			this.active = active;
		}
	}
}
