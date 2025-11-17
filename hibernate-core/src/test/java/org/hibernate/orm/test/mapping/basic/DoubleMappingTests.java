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
import static org.hamcrest.Matchers.isOneOf;

/**
 * Tests for mapping `double` values
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = DoubleMappingTests.EntityOfDoubles.class)
@SessionFactory
public class DoubleMappingTests {

	@Test
	public void testMappings(SessionFactoryScope scope) {
		// first, verify the type selections...
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor(EntityOfDoubles.class);

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapper");
			assertThat( attribute.getJavaType().getJavaTypeClass(), equalTo( Double.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Double.class));
			assertThat(
					jdbcMapping.getJdbcType().getJdbcTypeCode(),
					isOneOf(Types.DOUBLE, Types.FLOAT, Types.REAL, Types.NUMERIC)
			);
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitive");
			assertThat( attribute.getJavaType().getJavaTypeClass(), equalTo( Double.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Double.class));
			assertThat(
					jdbcMapping.getJdbcType().getJdbcTypeCode(),
					isOneOf(Types.DOUBLE, Types.FLOAT, Types.REAL, Types.NUMERIC)
			);
		}


		// and try to use the mapping
		scope.inTransaction(
				(session) -> session.persist(new EntityOfDoubles(1, 3.0, 5.0))
		);
		scope.inTransaction(
				(session) -> session.get(EntityOfDoubles.class, 1)
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EntityOfDoubles")
	@Table(name = "EntityOfDoubles")
	public static class EntityOfDoubles {
		@Id
		Integer id;

		//tag::basic-double-example-implicit[]
		// these will be mapped using DOUBLE, FLOAT, REAL or NUMERIC
		// depending on the capabilities of the database
		Double wrapper;
		double primitive;
		//end::basic-double-example-implicit[]

		public EntityOfDoubles() {
		}

		public EntityOfDoubles(Integer id, Double wrapper, double primitive) {
			this.id = id;
			this.wrapper = wrapper;
			this.primitive = primitive;
		}
	}
}
