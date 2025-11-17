/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = LocalDateTimeMappingTests.EntityWithLocalDateTime.class)
@SessionFactory
public class LocalDateTimeMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithLocalDateTime.class);

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("localDateTime");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(LocalDateTime.class));
		assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), equalTo( Types.TIMESTAMP));

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithLocalDateTime(1, LocalDateTime.now()));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithLocalDateTime.class, 1)
		);
	}

	@Entity(name = "EntityWithLocalDateTime")
	@Table(name = "EntityWithLocalDateTime")
	public static class EntityWithLocalDateTime {
		@Id
		private Integer id;

		//tag::basic-localDateTime-example[]
		// mapped as TIMESTAMP
		private LocalDateTime localDateTime;
		//end::basic-localDateTime-example[]

		public EntityWithLocalDateTime() {
		}

		public EntityWithLocalDateTime(Integer id, LocalDateTime localDateTime) {
			this.id = id;
			this.localDateTime = localDateTime;
		}
	}
}
