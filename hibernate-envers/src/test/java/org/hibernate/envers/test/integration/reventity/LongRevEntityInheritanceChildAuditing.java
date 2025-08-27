/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.reventity;

import java.util.List;

import org.hibernate.mapping.Selectable;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.integration.inheritance.joined.ChildEntity;
import org.hibernate.orm.test.envers.integration.inheritance.joined.ParentEntity;
import org.hibernate.mapping.Column;
import org.hibernate.orm.test.envers.integration.reventity.LongRevNumberRevEntity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
		// Hibernate now sorts columns that are part of the key and therefore this test needs to test
		// for the existence of the specific key column rather than the expectation that is exists at
		// a specific order.
		List<Selectable> childEntityKeyColumns = metadata()
				.getEntityBinding( ChildEntity.class.getName() + "_AUD" )
				.getKey()
				.getSelectables();

		final String revisionColumnName = getConfiguration().getRevisionFieldName();
		Column column = getColumnFromIteratorByName( childEntityKeyColumns, revisionColumnName );
		assertNotNull( column );
		assertEquals( column.getSqlType(), "int" );
	}
}
