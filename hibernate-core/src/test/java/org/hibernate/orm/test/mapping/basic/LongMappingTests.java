/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for mapping `short` values
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = LongMappingTests.EntityOfLongs.class)
@SessionFactory
public class LongMappingTests {

	@Test
	public void testMappings(SessionFactoryScope scope) {
		// first, verify the type selections...
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityOfLongs.class);

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapper");
			assertThat( attribute.getJavaType().getJavaTypeClass(), equalTo( Long.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Long.class));
			assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), is( Types.BIGINT));
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitive");
			assertThat( attribute.getJavaType().getJavaTypeClass(), equalTo( Long.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Long.class));
			assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), is( Types.BIGINT));
		}


		// and try to use the mapping
		scope.inTransaction(
				(session) -> session.persist(new EntityOfLongs(1, 3L, 5L))
		);
		scope.inTransaction(
				(session) -> session.get(EntityOfLongs.class, 1)
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EntityOfLongs")
	@Table(name = "EntityOfLongs")
	public static class EntityOfLongs {
		@Id
		Integer id;

		//tag::basic-long-example-implicit[]
		// these will both be mapped using BIGINT
		Long wrapper;
		long primitive;
		//end::basic-long-example-implicit[]

		public EntityOfLongs() {
		}

		public EntityOfLongs(Integer id, Long wrapper, long primitive) {
			this.id = id;
			this.wrapper = wrapper;
			this.primitive = primitive;
		}
	}
}
