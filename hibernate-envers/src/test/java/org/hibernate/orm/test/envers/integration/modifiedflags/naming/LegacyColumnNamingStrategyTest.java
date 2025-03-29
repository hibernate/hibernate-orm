/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags.naming;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
public class LegacyColumnNamingStrategyTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class, OtherEntity.class, SingleIdEntity.class };
	}

	@Test
	public void testModifiedColumns() {
		final Table table1 = metadata().getEntityBinding( TestEntity.class.getName() + "_AUD" ).getTable();
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

		final Table table2 = metadata().getEntityBinding( OtherEntity.class.getName() + "_AUD" ).getTable();
		assertNotNull( table2.getColumn( new Column( "data_MOD" ) ) );
	}

}
