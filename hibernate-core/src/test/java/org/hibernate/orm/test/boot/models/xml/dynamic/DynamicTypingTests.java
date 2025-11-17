/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.ClobJavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.java.UrlJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class DynamicTypingTests {
	@ServiceRegistry(settings = {
			@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"),
			@Setting(name = MappingSettings.PREFERRED_INSTANT_JDBC_TYPE, value = "INSTANT")
	} )
	@DomainModel(xmlMappings = "mappings/models/dynamic/dynamic-typing.xml")
	@Test
	void testDynamicModelBasicTyping(DomainModelScope modelScope) {
		final MetadataImplementor domainModel = modelScope.getDomainModel();
		final JdbcType uuidJdbcType = domainModel
				.getTypeConfiguration()
				.getJdbcTypeRegistry()
				.getDescriptor( SqlTypes.UUID );
		final JdbcType booleanJdbcType = domainModel
				.getTypeConfiguration()
				.getJdbcTypeRegistry()
				.getDescriptor( SqlTypes.BOOLEAN );

		final RootClass entityBinding = (RootClass) domainModel.getEntityBinding( "TheEntity" );
		assertThat( entityBinding ).isNotNull();

		verifyBasicAttribute( entityBinding, "theBoolean", BooleanJavaType.class, booleanJdbcType.getJdbcTypeCode() );
		verifyBasicAttribute( entityBinding, "theString", StringJavaType.class, SqlTypes.VARCHAR, /*HANA Cloud uses UTF8 by default*/ SqlTypes.NVARCHAR );
		verifyBasicAttribute( entityBinding, "theInt", IntegerJavaType.class, SqlTypes.INTEGER );
		verifyBasicAttribute( entityBinding, "theInteger", IntegerJavaType.class, SqlTypes.INTEGER );
		verifyBasicAttribute( entityBinding, "theUrl", UrlJavaType.class, SqlTypes.VARCHAR, /*HANA Cloud uses UTF8 by default*/ SqlTypes.NVARCHAR );
		verifyBasicAttribute( entityBinding, "theClob", ClobJavaType.class, SqlTypes.CLOB, /*CockroachDB doesn't support CLOBs*/ SqlTypes.VARCHAR );
		verifyBasicAttribute( entityBinding, "theInstant", InstantJavaType.class, SqlTypes.INSTANT );
		verifyBasicAttribute( entityBinding, "theDate", JdbcDateJavaType.class, SqlTypes.DATE );
		verifyBasicAttribute( entityBinding, "theTime", JdbcTimeJavaType.class, SqlTypes.TIME );
		verifyBasicAttribute( entityBinding, "theTimestamp", JdbcTimestampJavaType.class, SqlTypes.TIMESTAMP );

		verifyElementCollection( entityBinding, "listOfIntegers", IntegerJavaType.class, SqlTypes.INTEGER );
		verifyElementCollection( entityBinding, "listOfUuids", UUIDJavaType.class, uuidJdbcType.getJdbcTypeCode() );
	}


	private static void verifyBasicAttribute(RootClass rootClass, String attributeName, Class<? extends BasicJavaType<?>> expectedJavaType, int... expectedJdbcTypeCodes) {
		final Property attribute = rootClass.getProperty( attributeName );
		assertThat( attribute.getType() ).isInstanceOf( BasicType.class );
		verifyBasicMapping( (BasicType<?>) attribute.getType(), expectedJavaType, expectedJdbcTypeCodes );
	}

	private static void verifyBasicMapping(BasicType<?> type, Class<? extends BasicJavaType<?>> expectedJavaType, int... expectedJdbcTypeCodes) {
		assertThat( type.getJavaTypeDescriptor().getClass() ).isEqualTo( expectedJavaType );
		assertThat( expectedJdbcTypeCodes ).contains( type.getJdbcType().getJdbcTypeCode() );
	}

	private static void verifyElementCollection(RootClass rootClass, String name, Class<? extends BasicJavaType<?>> expectedJavaType, int expectedJdbcTypeCode) {
		final Property property = rootClass.getProperty( name );
		final Collection propertyValue = (Collection) property.getValue();
		verifyBasicMapping( (BasicType<?>) propertyValue.getElement().getType(), expectedJavaType, expectedJdbcTypeCode );
	}
}
