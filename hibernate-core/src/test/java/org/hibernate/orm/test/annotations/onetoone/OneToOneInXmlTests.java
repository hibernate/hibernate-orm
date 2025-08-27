/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import java.util.Iterator;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class OneToOneInXmlTests {

	@Test
	@JiraKey( "HHH-4606" )
	@DomainModel( xmlMappings = "org/hibernate/orm/test/annotations/onetoone/orm.xml" )
	@SuppressWarnings("JUnitMalformedDeclaration")
	public void testJoinColumnConfiguredInXml(DomainModelScope scope) {
		final PersistentClass pc = scope.getDomainModel().getEntityBinding( Son.class.getName() );
		Table table = pc.getJoins().get( 0 ).getTable();
		Iterator<Column> columnItr = table.getColumns().iterator();
		boolean fooFound = false;
		boolean barFound = false;
		while ( columnItr.hasNext() ) {
			Column column = columnItr.next();
			if ( column.getName().equals( "foo" ) ) {
				fooFound = true;
			}
			if ( column.getName().equals( "bar" ) ) {
				barFound = true;
			}
		}

		if ( !(fooFound && barFound) ) {
			fail( "The mapping defines join columns which could not be found in the metadata." );
		}
	}
}
