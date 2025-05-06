/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.time.OffsetTime;
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
@DomainModel(annotatedClasses = OffsetTimeMappingTests.EntityWithOffsetTime.class)
@SessionFactory
public class OffsetTimeMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithOffsetTime.class);

		final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("offsetTime");
		final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(OffsetTime.class));
		assertThat(
				jdbcMapping.getJdbcType().getJdbcTypeCode(),
				isOneOf( Types.TIME, Types.TIMESTAMP_WITH_TIMEZONE, Types.TIME_WITH_TIMEZONE )
		);

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithOffsetTime(1, OffsetTime.now()));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithOffsetTime.class, 1)
		);
	}

	@Entity(name = "EntityWithOffsetTime")
	@Table(name = "EntityWithOffsetTime")
	public static class EntityWithOffsetTime {
		@Id
		private Integer id;

		//tag::basic-offsetTime-example[]
		// mapped as TIME or TIME_WITH_TIMEZONE
		private OffsetTime offsetTime;
		//end::basic-offsetTime-example[]

		public EntityWithOffsetTime() {
		}

		public EntityWithOffsetTime(Integer id, OffsetTime offsetTime) {
			this.id = id;
			this.offsetTime = offsetTime;
		}
	}
}
