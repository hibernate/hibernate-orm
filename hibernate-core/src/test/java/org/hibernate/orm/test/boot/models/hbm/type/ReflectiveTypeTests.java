/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.type;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.ClobJavaType;
import org.hibernate.type.descriptor.java.DoubleJavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.ShortJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.java.UrlJavaType;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @implNote Limited to H2 simply because some Dialects will map some of these to
 * minor differences in SQL/JDBC types, and largely such differences are unimportant here
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect( H2Dialect.class )
public class ReflectiveTypeTests {
	@Test
	@ServiceRegistry(settings = {
			@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"),
			@Setting(name = MappingSettings.PREFERRED_INSTANT_JDBC_TYPE, value = "INSTANT")
	} )
	@DomainModel(xmlMappings = "mappings/models/hbm/type/basics.xml")
	void testBasicsXml(DomainModelScope scope) {
		scope.withHierarchy( EntityOfBasics.class, this::verify );
	}

	@Test
	@ServiceRegistry
	@DomainModel(xmlMappings = "mappings/models/hbm/type/element-collections.xml")
	void testElementCollectionsXml(DomainModelScope scope) {
		scope.withHierarchy( EntityWithElementCollections.class, this::verifyElementCollections );
	}

	private void verify(RootClass rootClass) {
		verify( (BasicType<?>) rootClass.getIdentifier().getType(), IntegerJavaType.class, SqlTypes.INTEGER );
		verify( rootClass, "theBoolean", BooleanJavaType.class, SqlTypes.BOOLEAN );
		verify( rootClass, "theString", StringJavaType.class, SqlTypes.VARCHAR );
		verify( rootClass, "theInt", IntegerJavaType.class, SqlTypes.INTEGER );
		verify( rootClass, "theInteger", IntegerJavaType.class, SqlTypes.INTEGER );
		verify( rootClass, "theShort", ShortJavaType.class, SqlTypes.SMALLINT );
		verify( rootClass, "theDouble", DoubleJavaType.class, SqlTypes.DOUBLE );
		verify( rootClass, "theUrl", UrlJavaType.class, SqlTypes.VARCHAR );
		verify( rootClass, "theClob", ClobJavaType.class, SqlTypes.CLOB );
		verify( rootClass, "theInstant", InstantJavaType.class, SqlTypes.INSTANT );
		verify( rootClass, "theDate", JdbcDateJavaType.class, SqlTypes.DATE );
		verify( rootClass, "theTime", JdbcTimeJavaType.class, SqlTypes.TIME );
		verify( rootClass, "theTimestamp", JdbcTimestampJavaType.class, SqlTypes.TIMESTAMP );
	}

	private static void verify(RootClass rootClass, String attributeName, Class<? extends BasicJavaType<?>> expectedJavaType, int expectedJdbcTypeCode) {
		final Property attribute = rootClass.getProperty( attributeName );
		assertThat( attribute.getType() ).isInstanceOf( BasicType.class );
		verify( (BasicType<?>) attribute.getType(), expectedJavaType, expectedJdbcTypeCode );
	}

	private void verifyElementCollections(RootClass rootClass) {
		verifyElementCollection( rootClass, "listOfStrings", StringJavaType.class, SqlTypes.VARCHAR );
		verifyElementCollection( rootClass, "listOfIntegers", IntegerJavaType.class, SqlTypes.INTEGER );
		verifyElementCollection( rootClass, "listOfDoubles", DoubleJavaType.class, SqlTypes.DOUBLE );
		verifyElementCollection( rootClass, "listOfUrls", UrlJavaType.class, SqlTypes.VARCHAR );
		verifyElementCollection( rootClass, "listOfUuids", UUIDJavaType.class, SqlTypes.OTHER );
	}

	private static void verifyElementCollection(RootClass rootClass, String name, Class<? extends BasicJavaType<?>> expectedJavaType, int expectedJdbcTypeCode) {
		final Property property = rootClass.getProperty( name );
		final Collection propertyValue = (Collection) property.getValue();
		verify( (BasicType<?>) propertyValue.getElement().getType(), expectedJavaType, expectedJdbcTypeCode );
	}

	private static void verify(BasicType<?> type, Class<? extends BasicJavaType<?>> expectedJavaType, int expectedJdbcTypeCode) {
		assertThat( type.getJavaTypeDescriptor().getClass() ).isEqualTo( expectedJavaType );
		assertThat( type.getJdbcType().getJdbcTypeCode() ).isEqualTo( expectedJdbcTypeCode );
	}

}
