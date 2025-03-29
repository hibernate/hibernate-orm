/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.Audited;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting(
				name = AvailableSettings.JPA_METAMODEL_POPULATION,
				value = "ignoreUnsupported"
		)
)
@DomainModel(
		annotatedClasses = RuntimeModelSmokeTests.SimpleEntity.class
)
@SessionFactory
public class RuntimeModelSmokeTests {
	public static final String FULL_NAME = "org.hibernate.orm.test.envers.RuntimeModelSmokeTests$SimpleEntity_AUD";
	public static final String SIMPLE_NAME = "SimpleEntity_AUD";

	@Test
	public void basicTest(SessionFactoryScope scope) {
		final RuntimeMetamodels runtimeMetamodels = scope.getSessionFactory().getRuntimeMetamodels();

		final EntityPersister mappingType = runtimeMetamodels.getMappingMetamodel().findEntityDescriptor( FULL_NAME );
		assertThat( mappingType, notNullValue() );
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "simple" )
	@Audited
	public static class SimpleEntity {
		@Id
		private Integer id;
		String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		private void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
