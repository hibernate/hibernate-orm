/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.util.List;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.orm.test.envers.integration.inheritance.joined.ChildEntity;
import org.hibernate.orm.test.envers.integration.inheritance.joined.ParentEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A join-inheritance test using a custom revision entity where the revision number is a long, mapped in the database
 * as an int.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@DomainModel(annotatedClasses = {ChildEntity.class, ParentEntity.class, LongRevNumberRevEntity.class})
@SessionFactory
public class LongRevEntityInheritanceChildAuditing {
	@Test
	public void testChildRevColumnType(DomainModelScope scope) {
		// Hibernate now sorts columns that are part of the key and therefore this test needs to test
		// for the existence of the specific key column rather than the expectation that is exists at
		// a specific order.
		List<Selectable> childEntityKeyColumns = scope.getDomainModel()
				.getEntityBinding( ChildEntity.class.getName() + "_AUD" )
				.getKey()
				.getSelectables();

		final String revisionColumnName = "REV"; // default revision field name
		Column column = getColumnFromIteratorByName( childEntityKeyColumns, revisionColumnName );
		assertNotNull( column );
		assertEquals( "int", column.getSqlType() );
	}

	private Column getColumnFromIteratorByName(List<Selectable> selectables, String columnName) {
		for ( Selectable selectable : selectables ) {
			if ( selectable instanceof Column ) {
				Column column = (Column) selectable;
				if ( columnName.equals( column.getName() ) ) {
					return column;
				}
			}
		}
		return null;
	}
}
