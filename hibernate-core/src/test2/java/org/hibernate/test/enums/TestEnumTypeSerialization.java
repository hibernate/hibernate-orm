/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.enums;

import java.util.Properties;

import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.EnumType;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class TestEnumTypeSerialization extends BaseUnitTestCase {
	@Test
	public void testSerializability() {
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		{
			// test ordinal mapping
			EnumType enumType = new EnumType( );
			enumType.setTypeConfiguration( typeConfiguration );
			Properties properties = new Properties();
			properties.put( EnumType.ENUM, UnspecifiedEnumTypeEntity.E1.class.getName() );
			enumType.setParameterValues( properties );
			assertTrue( enumType.isOrdinal() );
			SerializationHelper.clone( enumType );
		}

		{
			// test named mapping
			EnumType enumType = new EnumType();
			enumType.setTypeConfiguration( typeConfiguration );
			Properties properties = new Properties();
			properties.put( EnumType.ENUM, UnspecifiedEnumTypeEntity.E1.class.getName() );
			properties.put( EnumType.NAMED, "true" );
			enumType.setParameterValues( properties );
			assertFalse( enumType.isOrdinal() );
			SerializationHelper.clone( enumType );
		}
	}
}
