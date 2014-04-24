/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

package org.hibernate.envers.test.integration.reventity;

import java.util.Iterator;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.integration.inheritance.joined.ChildEntity;
import org.hibernate.envers.test.integration.inheritance.joined.ParentEntity;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A join-inheritance test using a custom revision entity where the revision number is a long, mapped in the database
 * as an int.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@FailureExpectedWithNewMetamodel( message = "Entity subclass has wrong ID type" )
public class LongRevEntityInheritanceChildAuditing extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ChildEntity.class, ParentEntity.class, LongRevNumberRevEntity.class};
	}

	@Test
	public void testChildRevColumnType() {
		// We need the second column
		Iterator childEntityKeyColumnsIterator = getMetadata()
				.getEntityBinding( "org.hibernate.envers.test.integration.inheritance.joined.ChildEntity_AUD" )
				.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding().getValues().iterator();
		childEntityKeyColumnsIterator.next();
		Value second = (Value) childEntityKeyColumnsIterator.next();

		assertEquals( second.getJdbcDataType().getTypeName(), "int" );
	}
}