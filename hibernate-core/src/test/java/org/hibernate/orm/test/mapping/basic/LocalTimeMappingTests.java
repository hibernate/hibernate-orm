/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.time.LocalTime;
import jakarta.persistence.Column;
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
@DomainModel(annotatedClasses = LocalTimeMappingTests.EntityWithLocalTime.class)
@SessionFactory
public class LocalTimeMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithLocalTime.class);

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("localTime");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(LocalTime.class));
		assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), equalTo( Types.TIME));

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithLocalTime(1, LocalTime.now()));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithLocalTime.class, 1)
		);
	}

	@Entity(name = "EntityWithLocalTime")
	@Table(name = "EntityWithLocalTime")
	public static class EntityWithLocalTime {
		@Id
		private Integer id;

		@Column(name = "`localTime`")
		//tag::basic-localTime-example[]
		// mapped as TIME
		private LocalTime localTime;
		//end::basic-localTime-example[]

		public EntityWithLocalTime() {
		}

		public EntityWithLocalTime(Integer id, LocalTime localTime) {
			this.id = id;
			this.localTime = localTime;
		}
	}
}
