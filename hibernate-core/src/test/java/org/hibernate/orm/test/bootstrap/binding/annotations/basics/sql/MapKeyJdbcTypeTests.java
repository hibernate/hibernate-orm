/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics.sql;

import java.sql.Types;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = MapKeyJdbcTypeTests.MyEntity.class )
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "The jTDS driver does support NVARCHAR so we remap it to CLOB")
public class MapKeyJdbcTypeTests {

	@Test
	public void verifyResolutions(DomainModelScope scope) {
		final MetadataImplementor domainModel = scope.getDomainModel();
		final Dialect dialect = domainModel.getDatabase().getDialect();
		final NationalizationSupport nationalizationSupport = dialect.getNationalizationSupport();
		final JdbcTypeRegistry jdbcTypeRegistry = domainModel.getTypeConfiguration()
				.getJdbcTypeRegistry();

		final PersistentClass entityBinding = domainModel.getEntityBinding( MyEntity.class.getName() );

		verifyJdbcTypeCodes(
				entityBinding.getProperty( "baseMap" ),
				jdbcTypeRegistry.getDescriptor( Types.INTEGER ).getJdbcTypeCode(),
				jdbcTypeRegistry.getDescriptor( Types.VARCHAR ).getJdbcTypeCode()
		);

		verifyJdbcTypeCodes(
				entityBinding.getProperty( "sqlTypeCodeMap" ),
				jdbcTypeRegistry.getDescriptor( Types.TINYINT ).getJdbcTypeCode(),
				jdbcTypeRegistry.getDescriptor( nationalizationSupport.getVarcharVariantCode() ).getJdbcTypeCode()
		);

		verifyJdbcTypeCodes(
				entityBinding.getProperty( "sqlTypeMap" ),
				Types.TINYINT,
				jdbcTypeRegistry.getDescriptor( nationalizationSupport.getVarcharVariantCode() ).getJdbcTypeCode()
		);

	}

	private void verifyJdbcTypeCodes(Property property, int keyJdbcTypeCode, int valueJdbcTypeCode) {
		verifyJdbcTypeResolution(
				property,
				(keyJdbcType) -> assertThat(
						"Map key for `" + property.getName() + "`",
						keyJdbcType.getJdbcTypeCode(),
						equalTo( keyJdbcTypeCode )
				),
				(valueJdbcType) -> assertThat(
						"Map value for `" + property.getName() + "`",
						valueJdbcType.getJdbcTypeCode(),
						equalTo( valueJdbcTypeCode )
				)
		);
	}

	private void verifyJdbcTypeResolution(
			Property property,
			Consumer<JdbcType> keyTypeVerifier,
			Consumer<JdbcType> valueTypeVerifier) {
		assertThat( property.getValue(), instanceOf( org.hibernate.mapping.Map.class ) );
		final org.hibernate.mapping.Map mapValue = (org.hibernate.mapping.Map) property.getValue();

		assertThat( mapValue.getIndex(), instanceOf( BasicValue.class ) );
		final BasicValue indexValue = (BasicValue) mapValue.getIndex();
		final BasicValue.Resolution<?> indexResolution = indexValue.resolve();
		keyTypeVerifier.accept( indexResolution.getJdbcType() );

		assertThat( mapValue.getElement(), instanceOf( BasicValue.class ) );
		final BasicValue elementValue = (BasicValue) mapValue.getElement();
		final BasicValue.Resolution<?> elementResolution = elementValue.resolve();
		valueTypeVerifier.accept( elementResolution.getJdbcType() );
	}

	@Entity( name = "MyEntity" )
	@Table( name = "my_entity" )
	public static class MyEntity {
		@Id
		private Integer id;

		@ElementCollection
		private Map<Integer,String> baseMap;

		@ElementCollection
		@JdbcTypeCode( Types.NVARCHAR )
		@MapKeyJdbcTypeCode( Types.TINYINT )
		private Map<Integer,String> sqlTypeCodeMap;

		@ElementCollection
		@JdbcTypeCode( Types.NVARCHAR )
		@MapKeyJdbcType( TinyIntJdbcType.class )
		private Map<Integer,String> sqlTypeMap;
	}
}
