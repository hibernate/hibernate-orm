/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags.naming;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Chris Cranford
 */
@EnversTest
@DomainModel(annotatedClasses = { TestEntity.class, OtherEntity.class, SingleIdEntity.class })
@SessionFactory
public class LegacyColumnNamingStrategyTest {

	@Test
	public void testModifiedColumns(DomainModelScope scope) {
		final Table table1 = scope.getDomainModel()
				.getEntityBinding( TestEntity.class.getName() + "_AUD" )
				.getTable();
		assertNotNull( table1.getColumn( new Column( "data1_MOD") ) );
		assertNotNull( table1.getColumn( new Column( "data2_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "data_3" ) ) );
		assertNotNull( table1.getColumn( new Column( "the_data_mod" ) ) );

		assertNotNull( table1.getColumn( new Column( "embeddable_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "otherEntity_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "singleIdEntity_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "singleIdEntity2_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "clientOption_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "cop_mod" ) ) );

		final Table table2 = scope.getDomainModel()
				.getEntityBinding( OtherEntity.class.getName() + "_AUD" )
				.getTable();
		assertNotNull( table2.getColumn( new Column( "data_MOD" ) ) );
	}
}
