/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				MappedSuperclassWithGenericsTest.IntermediateAbstractMapped.class,
				MappedSuperclassWithGenericsTest.BaseEntity.class,
				MappedSuperclassWithGenericsTest.AbstractGenericMappedSuperType.class,
				MappedSuperclassWithGenericsTest.SimpleEntity.class,
				MappedSuperclassWithGenericsTest.GenericIdBaseEntity.class
		}
)
@SessionFactory
public class MappedSuperclassWithGenericsTest {
	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-14499" )
	public void testIt() {

	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18007" )
	void testSelectCriteriaGenericId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery( Long.class );
			final Root<SimpleEntity> root = criteriaQuery.from( SimpleEntity.class );
			final Path<Long> idPath = root.get( "id" );
			criteriaQuery.select( idPath );
			final List<Long> resultList = session.createQuery( criteriaQuery ).getResultList();
			assertThat( resultList ).hasSize( 1 ).containsOnly( 1L );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18007" )
	void testSelectGenericId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Long> resultList = session.createQuery(
					"select e.id from SimpleEntity e",
					Long.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 ).containsOnly( 1L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new SimpleEntity( 1L, "simple_1" ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from SimpleEntity" ).executeUpdate() );
	}

	@MappedSuperclass
	public static abstract class AbstractGenericMappedSuperType<T> {
		private T whateverType;
	}

	@MappedSuperclass
	@IdClass( PK.class )
	public static abstract class IntermediateAbstractMapped<T> extends AbstractGenericMappedSuperType<T> {
		@Id
		private String keyOne;
		@Id
		private String keyTwo;
		@Id
		private String keyThree;
	}

	@SuppressWarnings("unused")
	public static class PK implements Serializable {
		private String keyOne;
		private String keyTwo;
		private String keyThree;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			PK pk = (PK) o;
			return Objects.equals( keyOne, pk.keyOne ) &&
					Objects.equals( keyTwo, pk.keyTwo ) &&
					Objects.equals( keyThree, pk.keyThree );
		}

		@Override
		public int hashCode() {
			return Objects.hash( keyOne, keyTwo, keyThree );
		}
	}

	@Entity( name = "BaseEntity" )
	public static class BaseEntity<T> extends IntermediateAbstractMapped<byte[]> {
		String aString;
	}

	@MappedSuperclass
	public static class GenericIdBaseEntity<T extends Serializable> {
		@Id
		private T id;

		protected GenericIdBaseEntity(T id) {
			this.id = id;
		}

		public T getId() {
			return id;
		}
	}

	@Entity( name = "SimpleEntity" )
	public static class SimpleEntity extends GenericIdBaseEntity<Long> {
		@Column
		private String string;

		public SimpleEntity() {
			super( null );
		}

		protected SimpleEntity(Long id, String string) {
			super( id );
			this.string = string;
		}
	}
}
