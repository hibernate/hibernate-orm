/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.enums;

import java.util.Properties;

import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.EnumType;
import org.hibernate.usertype.DynamicParameterizedType;

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
		{
			// test ordinal mapping
			EnumType enumType = new EnumType();
			Properties properties = new Properties();
			properties.put( EnumType.ENUM, UnspecifiedEnumTypeEntity.E1.class.getName() );
			enumType.setParameterValues( properties );
			assertTrue( enumType.isOrdinal() );
			SerializationHelper.clone( enumType );
		}

		{
			// test named mapping
			EnumType enumType = new EnumType();
			Properties properties = new Properties();
			properties.put( EnumType.ENUM, UnspecifiedEnumTypeEntity.E1.class.getName() );
			properties.put( EnumType.NAMED, "true" );
			enumType.setParameterValues( properties );
			assertFalse( enumType.isOrdinal() );
			SerializationHelper.clone( enumType );
		}
	}
}
