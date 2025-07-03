/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;

import org.hibernate.annotations.Nationalized;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for mapping wrapper values
 */
@DomainModel(annotatedClasses = {
		WrapperArrayHandlingLegacyTests.EntityOfByteArrays.class,
		WrapperArrayHandlingLegacyTests.EntityWithCharArrays.class,
})
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.WRAPPER_ARRAY_HANDLING, value = "LEGACY")
)
public class WrapperArrayHandlingLegacyTests {

	@Test
	@SkipForDialect(dialectClass = GaussDBDialect.class, reason = "GaussDB does not support byte array operations through lob type")
	public void verifyByteArrayMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( WrapperArrayHandlingLegacyTests.EntityOfByteArrays.class);

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
							new EntityOfByteArrays( 1, "abc".getBytes(), new Byte[] { (byte) 1 })
					);
				}
		);

		scope.inTransaction(
				(session) -> session.get( EntityOfByteArrays.class, 1)
		);
	}

	@Test
	public void verifyCharacterArrayMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final JdbcTypeRegistry jdbcRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( WrapperArrayHandlingLegacyTests.EntityWithCharArrays.class);

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitive");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR)));
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapper");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR)));
		}


		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitiveClob");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.CLOB)));
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapperClob");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.CLOB)));
		}
	}

	@Test
	public void verifyCharacterArrayNationalizedMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor(
				WrapperArrayHandlingLegacyTests.EntityWithCharArrays.class);
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final NationalizationSupport nationalizationSupport = dialect.getNationalizationSupport();

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitiveNVarchar");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), is( jdbcTypeRegistry.getDescriptor( nationalizationSupport.getVarcharVariantCode())));
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapperNVarchar");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), is( jdbcTypeRegistry.getDescriptor( nationalizationSupport.getVarcharVariantCode())));
		}


		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitiveNClob");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), is( jdbcTypeRegistry.getDescriptor( nationalizationSupport.getClobVariantCode())));
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapperNClob");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), is( jdbcTypeRegistry.getDescriptor( nationalizationSupport.getClobVariantCode())));
		}
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
	@Entity(name = "EntityWithCharArrays")
	@Table(name = "EntityWithCharArrays")
	public static class EntityWithCharArrays {
		@Id
		public Integer id;

		// mapped as VARCHAR
		char[] primitive;
		Character[] wrapper;

		// mapped as CLOB
		@Lob
		char[] primitiveClob;
		@Lob
		Character[] wrapperClob;

		// mapped as NVARCHAR
		@Nationalized
		char[] primitiveNVarchar;
		@Nationalized
		Character[] wrapperNVarchar;

		// mapped as NCLOB
		@Lob
		@Nationalized
		char[] primitiveNClob;
		@Lob
		@Nationalized
		Character[] wrapperNClob;
	}
}
