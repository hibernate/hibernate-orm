/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany.mapkey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-4235")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
@DomainModel(
		annotatedClasses = {
				ManyToManyWithMaykeyAndSchemaDefinitionTest.EntityA.class,
				ManyToManyWithMaykeyAndSchemaDefinitionTest.EntityB.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.HBM2DDL_CREATE_SCHEMAS, value = "true")
)
public class ManyToManyWithMaykeyAndSchemaDefinitionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = new EntityA();
					entityA.setId( 1L );

					EntityB entityB = new EntityB();
					entityB.setId( 1L );
					entityA.setEntityBs( "B", entityB );
					session.persist( entityB );
					session.persist( entityA );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testRetrievingTheMapGeneratesACorrectlyQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.get( EntityA.class, 1L );
					Collection<EntityB> values = entityA.getEntityBMap().values();
					assertThat( values.size(), is( 1 ) );
				}
		);

	}


	@Entity(name = "EntityA")
	@Table(name = "entitya", schema = "myschema")
	public static class EntityA {

		@Id
		private Long id;

		private String name;

		@ManyToMany
		@MapKey(name = "id")
		@JoinTable(name = "entitya_entityb", schema = "myschema", joinColumns = @JoinColumn(name = "entitya_pk"), inverseJoinColumns = @JoinColumn(name = "entityb_pk"))
		private Map<String, EntityB> entityBMap = new HashMap<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setEntityBs(String key, EntityB entityB) {
			this.entityBMap.put( key, entityB );
		}

		public Map<String, EntityB> getEntityBMap() {
			return entityBMap;
		}
	}

	@Entity(name = "EntityB")
	@Table(name = "entityb", schema = "myschema")
	public static class EntityB {
		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}
}
