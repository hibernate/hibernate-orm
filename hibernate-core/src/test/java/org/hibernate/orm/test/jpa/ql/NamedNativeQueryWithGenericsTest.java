/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ql;

import java.util.List;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		NamedNativeQueryWithGenericsTest.MyEntity.class,
		NamedNativeQueryWithGenericsTest.EntityType.class
})
@SessionFactory
@JiraKey("HHH-16023")
@RequiresDialect(H2Dialect.class)
public class NamedNativeQueryWithGenericsTest {
	@Test
	public void testNamedNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List myEntities = session.getNamedNativeQuery( "MyEntity.findMyEntity" )
					.addEntity( "ame", MyEntity.class )
					.getResultList();
			assertNotNull( myEntities );
		} );
	}

	@NamedNativeQueries(value = {
			@NamedNativeQuery(
					name = "MyEntity.findMyEntity",
					query = "WITH all_my_entities AS " +
							"(SELECT me.* FROM my_entity me) " +
							"SELECT {ame.*} FROM all_my_entities ame")
	})
	@Entity(name = "my_entity")
	@Table(name = "my_entity")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "entity_type")
	public static class MyEntity<E extends MyEntity> {
		@Id
		@GeneratedValue
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

	@Entity
	@DiscriminatorValue("SUPPORTING_ASSET_TYPE")
	public static class EntityType extends MyEntity<EntityType> {
		private String additionalProp;

		public String getAdditionalProp() {
			return additionalProp;
		}

		public void setAdditionalProp(String additionalProp) {
			this.additionalProp = additionalProp;
		}
	}
}
