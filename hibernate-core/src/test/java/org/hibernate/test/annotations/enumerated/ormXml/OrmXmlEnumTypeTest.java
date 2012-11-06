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
package org.hibernate.test.annotations.enumerated.ormXml;

import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.Type;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7645" )
public class OrmXmlEnumTypeTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String[] getXmlFiles() {
		return new String[] { "org/hibernate/test/annotations/enumerated/ormXml/orm.xml" };
	}

	@Test
	public void testOrmXmlDefinedEnumType() {
		Type bindingPropertyType = configuration().getClassMapping( BookWithOrmEnum.class.getName() )
				.getProperty( "bindingStringEnum" )
				.getType();
		CustomType customType = ExtraAssertions.assertTyping( CustomType.class, bindingPropertyType );
		EnumType enumType = ExtraAssertions.assertTyping( EnumType.class, customType.getUserType() );
		assertFalse( enumType.isOrdinal() );
	}
}
