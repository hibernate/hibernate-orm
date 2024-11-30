/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.internal.JpaMetamodelImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vincent Bouthinon
 */
@Jpa(
		annotatedClasses = {
				JoinedSubclassEntityPersisterTest.Animal.class,
				JoinedSubclassEntityPersisterTest.Dog.class,
		}
)
@JiraKey("HHH-18703")
class JoinedSubclassEntityPersisterTest {

	@Test
	void the_table_name_must_match_the_attribute_s_column(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					JpaMetamodelImpl metamodel = (JpaMetamodelImpl) entityManager.getMetamodel();
					MappingMetamodel mappingMetamodel = metamodel.getMappingMetamodel();
					EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( Dog.class );
					String table = entityDescriptor.getTableNameForColumn( "name" );
					assertEquals( "TANIMAL", table );
				}
		);
	}

	@Entity
	@Inheritance(strategy = JOINED)
	@Table(name = "TANIMAL")
	public static class Animal {

		@Id
		@GeneratedValue
		public Integer id;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "TDOG")
	public static class Dog extends Animal {

	}
}
