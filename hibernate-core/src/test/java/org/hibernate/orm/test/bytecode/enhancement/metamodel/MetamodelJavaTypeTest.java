/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.metamodel;

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
import java.util.Collection;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

	/**
	 * @author Scott Marlow
	 */
	@BytecodeEnhanced
	@SessionFactory
	public class MetamodelJavaTypeTest {

		@Test
		@DomainModel( annotatedClasses = SimpleEntity.class )
		public void basicManagedTest(SessionFactoryScope scope) {
			SimpleEntity entity = new SimpleEntity();
			scope.inTransaction( entityManager -> {
				entityManager.persist( entity );
				Metamodel metaModel = entityManager.getMetamodel();
				ManagedType<SimpleEntity> managedType = metaModel.managedType( SimpleEntity.class );
				Attribute<SimpleEntity,?> attribute = managedType.getDeclaredAttribute( "total" );
				Member member = attribute.getJavaMember();
				assertEquals( "getTotal", member.getName() );
			} );

		}

		// --- //

		@Entity
		private static class SimpleEntity {

			int id;
			int total;

			Collection itemNames = new Vector();

			public SimpleEntity() {
			}

			public SimpleEntity(int total) {
				this.total = total;
			}

			public SimpleEntity(int id, int total) {
				this.total = total;
				this.id = id;
			}

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
	}
