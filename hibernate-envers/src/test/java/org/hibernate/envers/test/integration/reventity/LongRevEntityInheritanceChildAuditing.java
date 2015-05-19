/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.reventity;

import java.util.Iterator;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.integration.inheritance.joined.ChildEntity;
import org.hibernate.envers.test.integration.inheritance.joined.ParentEntity;
import org.hibernate.mapping.Column;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A join-inheritance test using a custom revision entity where the revision number is a long, mapped in the database
 * as an int.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class LongRevEntityInheritanceChildAuditing extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ChildEntity.class, ParentEntity.class, LongRevNumberRevEntity.class};
	}

	@Test
	public void testChildRevColumnType() {
		// We need the second column
		Iterator childEntityKeyColumnsIterator = metadata()
				.getEntityBinding("org.hibernate.envers.test.integration.inheritance.joined.ChildEntity_AUD" )
				.getKey()
				.getColumnIterator();
		childEntityKeyColumnsIterator.next();
		Column second = (Column) childEntityKeyColumnsIterator.next();

		assertEquals( second.getSqlType(), "int" );
	}
}