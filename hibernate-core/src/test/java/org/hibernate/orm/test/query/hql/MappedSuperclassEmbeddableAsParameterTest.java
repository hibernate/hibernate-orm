/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		MappedSuperclassEmbeddableAsParameterTest.ConcreteEntity.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20339")
public class MappedSuperclassEmbeddableAsParameterTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var entity = new ConcreteEntity();
			entity.setId( 1L );
			entity.setIdent( new MyEmbeddable( "code_1", "desc_1" ) );
			entity.setName( "first" );
			session.persist( entity );

			var entity2 = new ConcreteEntity();
			entity2.setId( 2L );
			entity2.setIdent( new MyEmbeddable( "code_2", "desc_2" ) );
			entity2.setName( "second" );
			session.persist( entity2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testEmbeddableParameterOnConcreteEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var results = session.createQuery(
					"select e from ConcreteEntity e where e.ident = :ident",
					ConcreteEntity.class
			).setParameter( "ident", new MyEmbeddable( "code_1", "desc_1" ) ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).isEqualTo( "first" );
		} );
	}

	@Test
	public void testEmbeddableParameterOnMappedSuperclass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var results = session.createQuery(
					"select e from " + MySuperclass.class.getName() + " e where e.ident = :ident",
					MySuperclass.class
			).setParameter( "ident", new MyEmbeddable( "code_1", "desc_1" ) ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( ( (ConcreteEntity) results.get( 0 ) ).getName() ).isEqualTo( "first" );
		} );
	}

	@Embeddable
	public static class MyEmbeddable {
		private String code;
		private String description;

		public MyEmbeddable() {
		}

		public MyEmbeddable(String code, String description) {
			this.code = code;
			this.description = description;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	@MappedSuperclass
	public static abstract class MySuperclass {
		@Embedded
		private MyEmbeddable ident;

		public MyEmbeddable getIdent() {
			return ident;
		}

		public void setIdent(MyEmbeddable ident) {
			this.ident = ident;
		}
	}

	@Entity(name = "ConcreteEntity")
	public static class ConcreteEntity extends MySuperclass {
		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
