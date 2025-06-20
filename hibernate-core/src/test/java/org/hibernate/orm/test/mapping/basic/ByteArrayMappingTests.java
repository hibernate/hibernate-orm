/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.annotations.JavaType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.ByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = ByteArrayMappingTests.EntityOfByteArrays.class)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.WRAPPER_ARRAY_HANDLING, value = "ALLOW")
)
public class ByteArrayMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor(EntityOfByteArrays.class);

		{
			final BasicAttributeMapping primitive = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitive");
			final JdbcMapping jdbcMapping = primitive.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(byte[].class));
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcTypeRegistry.getDescriptor( Types.VARBINARY ) ) );
		}

		{
			final BasicAttributeMapping primitive = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapper");
			final JdbcMapping jdbcMapping = primitive.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Byte[].class));
			if ( dialect.supportsStandardArrays() ) {
				assertThat( jdbcMapping.getJdbcType(), instanceOf( ArrayJdbcType.class ) );
				assertThat(
						( (ArrayJdbcType) jdbcMapping.getJdbcType() ).getElementJdbcType(),
						is( jdbcTypeRegistry.getDescriptor( Types.TINYINT ) )
				);
			}
			else {
				assertThat(
						jdbcMapping.getJdbcType().getDdlTypeCode(),
						isOneOf( SqlTypes.ARRAY, SqlTypes.JSON, SqlTypes.SQLXML, SqlTypes.VARBINARY, SqlTypes.LONG32VARCHAR )
				);
			}
		}

		{
			final BasicAttributeMapping primitive = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapperOld");
			final JdbcMapping jdbcMapping = primitive.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Byte[].class));
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcTypeRegistry.getDescriptor( Types.VARBINARY ) ) );
		}

		{
			final BasicAttributeMapping primitive = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitiveLob");
			final JdbcMapping jdbcMapping = primitive.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(byte[].class));
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcTypeRegistry.getDescriptor( Types.BLOB ) ) );
		}

		{
			final BasicAttributeMapping primitive = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapperLob");
			final JdbcMapping jdbcMapping = primitive.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Byte[].class));
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcTypeRegistry.getDescriptor( Types.BLOB ) ) );
		}

		scope.inTransaction(
				(session) -> {
					session.persist(
							new EntityOfByteArrays(1, "abc".getBytes(), new Byte[] { (byte) 1 })
					);
				}
		);

		scope.inTransaction(
				(session) -> session.get(EntityOfByteArrays.class, 1)
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}


	@Entity(name = "EntityOfByteArrays")
	@Table(name = "EntityOfByteArrays")
	public static class EntityOfByteArrays {
		@Id
		public Integer id;

		//tag::basic-bytearray-example[]
		// mapped as VARBINARY
		private byte[] primitive;
		private Byte[] wrapper;
		@JavaType( ByteArrayJavaType.class )
		private Byte[] wrapperOld;

		// mapped as (materialized) BLOB
		@Lob
		private byte[] primitiveLob;
		@Lob
		private Byte[] wrapperLob;
		//end::basic-bytearray-example[]

		public EntityOfByteArrays() {
		}

		public EntityOfByteArrays(Integer id, byte[] primitive, Byte[] wrapper) {
			this.id = id;
			this.primitive = primitive;
			this.wrapper = wrapper;
			this.primitiveLob = primitive;
			this.wrapperLob = wrapper;
		}

		public EntityOfByteArrays(Integer id, byte[] primitive, Byte[] wrapper, byte[] primitiveLob, Byte[] wrapperLob) {
			this.id = id;
			this.primitive = primitive;
			this.wrapper = wrapper;
			this.primitiveLob = primitiveLob;
			this.wrapperLob = wrapperLob;
		}
	}
}
