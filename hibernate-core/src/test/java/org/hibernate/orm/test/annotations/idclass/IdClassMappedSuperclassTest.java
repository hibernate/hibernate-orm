/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idclass;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-9114")
@DomainModel(annotatedClasses = IdClassMappedSuperclassTest.Simple.class)
@SessionFactory
public class IdClassMappedSuperclassTest {

	@Test
	public void testIdClassWithMappedSuperclassAndId(SessionFactoryScope scope) {
		Simple sim = scope.fromTransaction(
				session -> {
					// Persist the entity
					Simple simple = new Simple();
					simple.setSimpleId( "1" );
					simple.setCategoryId( "2" );
					session.persist( simple );

					session.getTransaction().commit();

					session.clear();

					session.getTransaction().begin();

					// Query the entity.
					simple = session.createQuery( "FROM Simple", Simple.class ).getSingleResult();
					return simple;
				}
		);

		assertNotNull( sim );
		assertEquals( "1", sim.getSimpleId() );
		assertEquals( "2", sim.getCategoryId() );

	}

	@MappedSuperclass
	public abstract static class AbstractMappedSuperclass {
		@Id
		private String categoryId;

		public String getCategoryId() {
			return categoryId;
		}

		public void setCategoryId(String categoryId) {
			this.categoryId = categoryId;
		}
	}

	@Entity(name = "Simple")
	@IdClass(SimpleId.class)
	public static class Simple extends AbstractMappedSuperclass {
		@Id
		private String simpleId;
		private String data;

		public String getSimpleId() {
			return simpleId;
		}

		public void setSimpleId(String simpleId) {
			this.simpleId = simpleId;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	public static class SimpleId implements Serializable {
		private String simpleId;
		private String categoryId;

		public String getSimpleId() {
			return simpleId;
		}

		public void setSimpleId(String simpleId) {
			this.simpleId = simpleId;
		}

		public String getCategoryId() {
			return categoryId;
		}

		public void setCategoryId(String categoryId) {
			this.categoryId = categoryId;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			SimpleId simpleId1 = (SimpleId) o;

			if ( getSimpleId() != null ?
					!getSimpleId().equals( simpleId1.getSimpleId() ) :
					simpleId1.getSimpleId() != null ) {
				return false;
			}
			return getCategoryId() != null ?
					getCategoryId().equals( simpleId1.getCategoryId() ) :
					simpleId1.getCategoryId() == null;
		}

		@Override
		public int hashCode() {
			int result = getSimpleId() != null ? getSimpleId().hashCode() : 0;
			result = 31 * result + ( getCategoryId() != null ? getCategoryId().hashCode() : 0 );
			return result;
		}
	}
}
