/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.time.ZonedDateTime;
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
import static org.hamcrest.Matchers.isOneOf;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = ZonedDateTimeMappingTests.EntityWithZonedDateTime.class)
@SessionFactory
public class ZonedDateTimeMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithZonedDateTime.class);

		final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("zonedDateTime");
		final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(ZonedDateTime.class));
		assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), isOneOf( Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE));

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithZonedDateTime(1, ZonedDateTime.now()));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithZonedDateTime.class, 1)
		);
	}

	@Entity(name = "EntityWithZonedDateTime")
	@Table(name = "EntityWithZonedDateTime")
	public static class EntityWithZonedDateTime {
		@Id
		private Integer id;

		//tag::basic-ZonedDateTime-example[]
		// mapped as TIMESTAMP or TIMESTAMP_WITH_TIMEZONE
		private ZonedDateTime zonedDateTime;
		//end::basic-ZonedDateTime-example[]

		public EntityWithZonedDateTime() {
		}

		public EntityWithZonedDateTime(Integer id, ZonedDateTime zonedDateTime) {
			this.id = id;
			this.zonedDateTime = zonedDateTime;
		}
	}
}
