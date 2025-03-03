/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-16606")
public class SelectOnlyArrayPropertyTest extends BaseEntityManagerFunctionalTestCase {


	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithIdAndIntegerArray.class
		};
	}

	@Test
	@JiraKey("HHH-16606")
	public void criteriaSelectOnlyIntArray() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final byte[] result = "Hello, World!".getBytes();

			EntityWithIdAndIntegerArray myEntity = new EntityWithIdAndIntegerArray( 1, result );
			entityManager.persist( myEntity );

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<byte[]> cq = cb.createQuery( byte[].class );

			Root<EntityWithIdAndIntegerArray> root = cq.from( EntityWithIdAndIntegerArray.class );

			cq.select( root.get( "bytes" ) )
					.where( cb.equal( root.get( "id" ), 1 ) );

			TypedQuery<byte[]> q = entityManager.createQuery( cq );

			byte[] bytes = q.getSingleResult();

			assertArrayEquals( result, bytes );
		} );
	}

	@Test
	public void criteriaSelectWrappedIntArray() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final byte[] result = "Hi there!".getBytes();

			EntityWithIdAndIntegerArray myEntity = new EntityWithIdAndIntegerArray( 2, result );
			entityManager.persist( myEntity );

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Object[]> cq = cb.createQuery( Object[].class );

			Root<EntityWithIdAndIntegerArray> root = cq.from( EntityWithIdAndIntegerArray.class );

			cq.select( root.get( "bytes" ) )
					.where( cb.equal( root.get( "id" ), 2 ) );

			TypedQuery<Object[]> q = entityManager.createQuery( cq );

			final Object[] objects = q.getSingleResult();
			assertEquals( 1, objects.length );
			byte[] bytes = (byte[]) objects[0];

			assertArrayEquals( result, bytes );
		} );
	}

	@Entity(name = "EntityWithIdAndIntegerArray")
	public static class EntityWithIdAndIntegerArray {

		@Id
		private Integer id;

		private byte[] bytes;

		public EntityWithIdAndIntegerArray(Integer id, byte[] bytes) {
			this.id = id;
			this.bytes = bytes;
		}

		public EntityWithIdAndIntegerArray() {

		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}
	}
}
