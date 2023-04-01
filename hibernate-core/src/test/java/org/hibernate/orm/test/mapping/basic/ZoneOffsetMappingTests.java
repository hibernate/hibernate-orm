/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.time.ZoneOffset;
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
@DomainModel(annotatedClasses = ZoneOffsetMappingTests.EntityWithZoneOffset.class)
@SessionFactory
public class ZoneOffsetMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final JdbcTypeRegistry jdbcRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithZoneOffset.class);

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("zoneOffset");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(ZoneOffset.class));
		assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR)));

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithZoneOffset(1, ZoneOffset.from(ZoneOffset.MIN)));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithZoneOffset.class, 1)
		);
	}

	@Entity(name = "EntityWithZoneOffset")
	@Table(name = "EntityWithZoneOffset")
	public static class EntityWithZoneOffset {
		@Id
		private Integer id;

		//tag::basic-ZoneOffset-example[]
		// mapped as VARCHAR
		private ZoneOffset zoneOffset;
		//end::basic-ZoneOffset-example[]

		public EntityWithZoneOffset() {
		}

		public EntityWithZoneOffset(Integer id, ZoneOffset zoneOffset) {
			this.id = id;
			this.zoneOffset = zoneOffset;
		}
	}
}
