/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.override;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@DomainModel(
		annotatedClasses = {
				GenericGeneratorOverrideTest.Entity1.class,
				GenericGeneratorOverrideTest.Entity2.class,
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-4309")
public class GenericGeneratorOverrideTest {

	@Test
	public void test(SessionFactoryScope scope) {
		SessionFactoryImplementor sf = scope.getSessionFactory();
		EntityPersister p1 = sf.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Entity1.class.getName() );
		EntityPersister p2 = sf.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Entity2.class.getName() );

		Generator generator1 = p1.getGenerator();
		Generator generator2 = p2.getGenerator();
		assertThat( generator1 ).isInstanceOf( UUIDGenerator.class );
		assertThat( generator2 ).isInstanceOf( UUIDHexGenerator.class );
	}

	@MappedSuperclass
	@GenericGenerator(name = "my-generator", strategy = "uuid2")
	public static abstract class BaseEntity {
		@Id
		@GeneratedValue(generator = "my-generator")
		private String id;

		public String getId() {
			return id;
		}

		public void setId(final String id) {
			this.id = id;
		}
	}

	@jakarta.persistence.Entity(name = "Entity1")
	public static class Entity1 extends BaseEntity {
		public Entity1() {
		}
	}

	@jakarta.persistence.Entity(name = "Entity2")
	@GenericGenerator(name = "my-generator", strategy = "uuid.hex")
	public static class Entity2 extends BaseEntity {
		public Entity2() {
		}
	}

}
