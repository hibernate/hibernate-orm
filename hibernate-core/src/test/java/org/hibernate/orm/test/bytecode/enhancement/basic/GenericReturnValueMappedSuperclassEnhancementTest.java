/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;


import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

@BytecodeEnhanced
public class GenericReturnValueMappedSuperclassEnhancementTest {

	@Test
	@JiraKey("HHH-12579")
	public void enhanceClassWithGenericReturnValueOnMappedSuperclass() {
		SimpleEntity implementation = new SimpleEntity();

		implementation.setEntity( SimpleEntity.Type.ONE );

		assertEquals( SimpleEntity.Type.ONE, implementation.getEntity() );
	}

	@MappedSuperclass
	@Cache(usage = CacheConcurrencyStrategy.NONE)
	public static class AbstractMappedSuperclassWithGenericReturnValue<T extends Marker> {

		@Id
		@GeneratedValue
		public int id;

		@Access(AccessType.PROPERTY)
		private T entity;

		public T getEntity() {
			return entity;
		}

		public void setEntity(T entity) {
			this.entity = entity;
		}
	}

	public interface Marker {
	}

	@Entity
	@Cache(usage = CacheConcurrencyStrategy.NONE)
	public static class SimpleEntity extends AbstractMappedSuperclassWithGenericReturnValue<SimpleEntity.Type> {

		public enum Type implements Marker {
			ONE
		}
	}
}
