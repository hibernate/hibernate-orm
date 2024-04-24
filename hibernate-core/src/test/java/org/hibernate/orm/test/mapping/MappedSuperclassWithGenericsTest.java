/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@TestForIssue(jiraKey = "HHH-14499")
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
	public void testIt() {

	}

	@Test
	void testSelectCriteriaGenericId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery( Long.class );
			Root<SimpleEntity> root = criteriaQuery.from( SimpleEntity.class );
			Path<Long> idPath = root.get( "id" );
			criteriaQuery.select( idPath );
			assertThat( session.createQuery( criteriaQuery ).getResultList() ).isEmpty();
		} );
	}

	@Test
	void testSelectGenericId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery( "select e.id from SimpleEntity e", Long.class ).getResultList() ).isEmpty();
		} );
	}

	@MappedSuperclass
	public static abstract class AbstractGenericMappedSuperType<T> {

		private T whateverType;

	}

	@MappedSuperclass
	@IdClass(PK.class)
	public static abstract class IntermediateAbstractMapped<T> extends AbstractGenericMappedSuperType<T> {

		@Id
		private String keyOne;
		@Id
		private String keyTwo;
		@Id
		private String keyThree;
	}

	@SuppressWarnings("UnusedDeclaration")
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

	@Entity(name = "BaseEntity")
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

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity extends GenericIdBaseEntity<Long> {

		@Column
		String string;

		public SimpleEntity() {
			super( null );
		}

		protected SimpleEntity(Long id) {
			super( id );
		}
	}

}
