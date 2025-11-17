/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = InstantMappingTests.EntityWithInstant.class)
@SessionFactory
public class InstantMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithInstant.class);

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("instant");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Instant.class));
		assertThat(
				jdbcMapping.getJdbcType(),
				equalTo(
						mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry()
								.getDescriptor( SqlTypes.TIMESTAMP_UTC )
				)
		);

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithInstant(1, Instant.now()));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithInstant.class, 1)
		);
	}

	@Entity(name = "EntityWithInstant")
	@Table(name = "EntityWithInstant")
	public static class EntityWithInstant {
		@Id
		private Integer id;

		//tag::basic-instant-example[]
		// mapped as TIMESTAMP
		private Instant instant;
		//end::basic-instant-example[]

		public EntityWithInstant() {
		}

		public EntityWithInstant(Integer id, Instant instant) {
			this.id = id;
			this.instant = instant;
		}
	}
}
