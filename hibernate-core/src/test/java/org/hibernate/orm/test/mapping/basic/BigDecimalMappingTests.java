/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.math.BigDecimal;
import java.sql.Types;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for mapping `BigInteger` values
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = BigDecimalMappingTests.EntityOfBigDecimals.class)
@SessionFactory
public class BigDecimalMappingTests {

	@Test
	public void testMappings(SessionFactoryScope scope) {
		// first, verify the type selections...
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( EntityOfBigDecimals.class );
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapper");
			assertThat( attribute.getJavaType().getJavaTypeClass(), equalTo( BigDecimal.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(BigDecimal.class));
			assertThat( jdbcMapping.getJdbcType(), is( jdbcTypeRegistry.getDescriptor( Types.NUMERIC)));
		}


		// and try to use the mapping
		scope.inTransaction(
				(session) -> session.persist(new EntityOfBigDecimals(1, BigDecimal.TEN))
		);
		scope.inTransaction(
				(session) -> session.get(EntityOfBigDecimals.class, 1)
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EntityOfBigDecimals")
	@Table(name = "EntityOfBigDecimals")
	public static class EntityOfBigDecimals {
		@Id
		Integer id;

		//tag::basic-bigdecimal-example-implicit[]
		// will be mapped using NUMERIC
		BigDecimal wrapper;
		//end::basic-bigdecimal-example-implicit[]

		public EntityOfBigDecimals() {
		}

		public EntityOfBigDecimals(Integer id, BigDecimal wrapper) {
			this.id = id;
			this.wrapper = wrapper;
		}
	}
}
