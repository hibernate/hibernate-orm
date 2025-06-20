/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.util.TimeZone;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = TimeZoneMappingTests.EntityWithTimeZone.class)
@SessionFactory
public class TimeZoneMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final JdbcTypeRegistry jdbcRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithTimeZone.class);

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("timeZone");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(TimeZone.class));
		assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR)));

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithTimeZone(1, TimeZone.getDefault()));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithTimeZone.class, 1)
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17726" )
	public void testUpdateQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new EntityWithTimeZone( 1, TimeZone.getDefault() ) ) );
		scope.inTransaction( session -> {
			final TimeZone timeZone = TimeZone.getTimeZone( "UTC" );
			session.createMutationQuery( "update EntityWithTimeZone e set e.timeZone = :timeZone" )
					.setParameter( "timeZone", timeZone )
					.executeUpdate();
			final EntityWithTimeZone entity = session.find( EntityWithTimeZone.class, 1 );
			assertThat( entity.timeZone, equalTo( timeZone ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EntityWithTimeZone")
	@Table(name = "EntityWithTimeZone")
	public static class EntityWithTimeZone {
		@Id
		private Integer id;

		//tag::basic-timeZone-example[]
		// mapped as VARCHAR
		private TimeZone timeZone;
		//end::basic-timeZone-example[]

		public EntityWithTimeZone() {
		}

		public EntityWithTimeZone(Integer id, TimeZone timeZone) {
			this.id = id;
			this.timeZone = timeZone;
		}
	}
}
