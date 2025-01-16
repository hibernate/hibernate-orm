/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.access;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Member;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Scott Marlow
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		PropertyAccessMemberTest.PropertyEntity.class,
		PropertyAccessMemberTest.FieldEntity.class
})
@BytecodeEnhanced
public class PropertyAccessMemberTest {
	@Test
	public void testPropertyAccessMember(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Metamodel metaModel = entityManager.getMetamodel();
			final ManagedType<PropertyEntity> managedType = metaModel.managedType( PropertyEntity.class );
			final Attribute<PropertyEntity, ?> attribute = managedType.getDeclaredAttribute( "total" );
			final Member member = attribute.getJavaMember();
			assertEquals( "getTotal", member.getName() );
		} );
	}

	@Test
	public void testFieldAccessMember(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Metamodel metaModel = entityManager.getMetamodel();
			final ManagedType<FieldEntity> managedType = metaModel.managedType( FieldEntity.class );
			final Attribute<FieldEntity, ?> attribute = managedType.getDeclaredAttribute( "total" );
			final Member member = attribute.getJavaMember();
			assertEquals( "total", member.getName() );
		} );
	}

	@Entity(name = "PropertyEntity")
	static class PropertyEntity {
		private int id;
		private int total;

		@Id
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getTotal() {
			return total;
		}

		public void setTotal(int total) {
			this.total = total;
		}
	}

	@Entity(name = "FieldEntity")
	static class FieldEntity {
		@Id
		private int id;
		private int total;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getTotal() {
			return total;
		}

		public void setTotal(int total) {
			this.total = total;
		}
	}
}
