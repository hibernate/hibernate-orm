/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics.sql;

import java.sql.Clob;
import java.sql.Types;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = JdbcTypeTests.SimpleEntity.class
)
public class JdbcTypeTests {

	@Test
	public void verifyResolutions(DomainModelScope scope) {
		final MetadataImplementor domainModel = scope.getDomainModel();
		final Dialect dialect = domainModel.getDatabase().getDialect();
		final NationalizationSupport nationalizationSupport = dialect.getNationalizationSupport();
		final JdbcTypeRegistry jdbcTypeRegistry = domainModel.getTypeConfiguration()
				.getJdbcTypeRegistry();
		final PersistentClass entityBinding = domainModel.getEntityBinding( SimpleEntity.class.getName() );

		verifyJdbcTypeCode(
				entityBinding.getProperty( "materializedClob" ),
				jdbcTypeRegistry.getDescriptor( Types.CLOB ).getJdbcTypeCode()
		);
		verifyJdbcTypeCode(
				entityBinding.getProperty( "materializedNClob" ),
				jdbcTypeRegistry.getDescriptor( nationalizationSupport.getClobVariantCode() )
						.getJdbcTypeCode()
		);
		verifyJdbcTypeCode(
				entityBinding.getProperty( "jpaMaterializedClob" ),
				jdbcTypeRegistry.getDescriptor( Types.CLOB ).getJdbcTypeCode()
		);
		verifyJdbcTypeCode(
				entityBinding.getProperty( "jpaMaterializedNClob" ),
				jdbcTypeRegistry.getDescriptor( nationalizationSupport.getClobVariantCode() )
						.getJdbcTypeCode()
		);

		verifyJdbcTypeCode(
				entityBinding.getProperty( "nationalizedString" ),
				jdbcTypeRegistry.getDescriptor( nationalizationSupport.getVarcharVariantCode() )
						.getJdbcTypeCode()
		);
		verifyJdbcTypeCode(
				entityBinding.getProperty( "nationalizedClob" ),
				jdbcTypeRegistry.getDescriptor( nationalizationSupport.getClobVariantCode() )
						.getJdbcTypeCode()
		);

		verifyResolution( entityBinding.getProperty( "customType" ), CustomJdbcType.class );
		verifyResolution( entityBinding.getProperty( "customTypeRegistration" ), RegisteredCustomJdbcType.class );
	}

	private void verifyResolution(Property property, Class<? extends JdbcType> expectedSqlTypeDescriptor) {
		verifyResolution(
				property,
				sqlTypeDescriptor -> {
					assertThat( "For property `" + property.getName() + "`", sqlTypeDescriptor, instanceOf( expectedSqlTypeDescriptor ) );
				}
		);
	}

	private void verifyJdbcTypeCode(Property property, int typeCode) {
		verifyJdbcTypeResolution(
				property,
				(p, jdbcType) -> assertThat(
						"JDBC type code mismatch for `" + property.getName() + "`",
						jdbcType.getJdbcTypeCode(),
						equalTo( typeCode )
				)
		);
	}

	private void verifyJdbcTypeResolution(
			Property property,
			BiConsumer<Property, JdbcType> verifier) {
		final Value value = property.getValue();
		assertThat( value, instanceOf( BasicValue.class ) );
		final BasicValue basicValue = (BasicValue) value;
		final BasicValue.Resolution<?> resolution = basicValue.resolve();

		verifier.accept( property, resolution.getJdbcType() );
	}


	private void verifyResolution(
			Property property,
			Consumer<JdbcType> stdVerifier) {
		final Value value = property.getValue();
		assertThat( value, instanceOf( BasicValue.class ) );
		final BasicValue basicValue = (BasicValue) value;
		final BasicValue.Resolution<?> resolution = basicValue.resolve();

		stdVerifier.accept( resolution.getJdbcType() );
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "simple_entity" )
	@JdbcTypeRegistration( value = RegisteredCustomJdbcType.class, registrationCode = Integer.MAX_VALUE - 1 )
	@SuppressWarnings("unused")
	public static class SimpleEntity {
		@Id
		private Integer id;

		@JdbcTypeCode( Types.CLOB )
		private String materializedClob;

		@JdbcTypeCode( Types.NCLOB )
		private String materializedNClob;

		@Lob
		private String jpaMaterializedClob;

		@Lob
		@Nationalized
		private String jpaMaterializedNClob;

		@Nationalized
		private String nationalizedString;

		@Nationalized
		private Clob nationalizedClob;

		@org.hibernate.annotations.JdbcType( CustomJdbcType.class )
		private Integer customType;

		@JdbcTypeCode( Integer.MAX_VALUE - 1 )
		private Integer customTypeRegistration;
	}

	public static class CustomJdbcType implements JdbcType {
		@Override
		public int getJdbcTypeCode() {
			return Types.TINYINT;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
			return TinyIntJdbcType.INSTANCE.getBinder( javaType );
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return TinyIntJdbcType.INSTANCE.getExtractor( javaType );
		}
	}

	public static class RegisteredCustomJdbcType extends CustomJdbcType {
	}
}
