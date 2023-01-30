/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.util.TimeZone;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

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
