/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.time.Instant;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.TemporalJavaType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = DatePrecisionTests.EntityOfDates.class)
@SessionFactory
public class DatePrecisionTests {
	@Test
	public void verifyMapping(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityOfDates.class);

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("dateAsTimestamp");
			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			final TemporalJavaType jtd = (TemporalJavaType) jdbcMapping.getJavaTypeDescriptor();
			assertThat(jtd, is(attribute.getJavaType()));
			assertThat(jtd.getJavaTypeClass(), equalTo(Date.class));
			assertThat(jtd.getPrecision(), equalTo(TemporalType.TIMESTAMP));
			assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), equalTo( Types.TIMESTAMP));
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("dateAsDate");
			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			final TemporalJavaType jtd = (TemporalJavaType) jdbcMapping.getJavaTypeDescriptor();
			assertThat(jtd, is(attribute.getJavaType()));
			assertThat(jtd.getJavaTypeClass(), equalTo(Date.class));
			assertThat(jtd.getPrecision(), equalTo(TemporalType.DATE));
			assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), equalTo( Types.DATE));
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("dateAsTime");
			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			final TemporalJavaType jtd = (TemporalJavaType) jdbcMapping.getJavaTypeDescriptor();
			assertThat(jtd, is(attribute.getJavaType()));
			assertThat(jtd.getJavaTypeClass(), equalTo(Date.class));
			assertThat(jtd.getPrecision(), equalTo(TemporalType.TIME));
			assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), equalTo( Types.TIME));
		}

		// check persistence operations

		scope.inTransaction(
				(session) -> {
					session.persist(
							new EntityOfDates(
									1,
									Date.from(Instant.now()),
									Date.from(Instant.now()),
									Date.from(Instant.now())
							)
					);
				}
		);
		scope.inTransaction(
				(session) -> {
					session.find(EntityOfDates.class, 1);
				}
		);
	}

	@Entity(name = "EntityWithTimestamp")
	public static class EntityOfDates {
		@Id
		private Integer id;

		//tag::basic-temporal-example[]
		// mapped as TIMESTAMP by default
		Date dateAsTimestamp;

		// explicitly mapped as DATE
		@Temporal(TemporalType.DATE)
		Date dateAsDate;

		// explicitly mapped as TIME
		@Temporal(TemporalType.TIME)
		Date dateAsTime;
		//end::basic-temporal-example[]


		public EntityOfDates() {
		}

		public EntityOfDates(Integer id, Date dateAsTimestamp, Date dateAsDate, Date dateAsTime) {
			this.id = id;
			this.dateAsTimestamp = dateAsTimestamp;
			this.dateAsDate = dateAsDate;
			this.dateAsTime = dateAsTime;
		}
	}
}
