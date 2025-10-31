/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags.naming;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Chris Cranford
 */
@EnversTest
@DomainModel(annotatedClasses = {TestEntity.class, OtherEntity.class, SingleIdEntity.class})
@ServiceRegistry(settings = @Setting(name = EnversSettings.MODIFIED_COLUMN_NAMING_STRATEGY, value = "improved"))
@SessionFactory
public class ImprovedColumnNamingStrategyTest {

	@Test
	public void testModifiedColumns(DomainModelScope scope) {
		final Table table1 = scope.getDomainModel()
				.getEntityBinding( TestEntity.class.getName() + "_AUD" )
				.getTable();
		assertNotNull( table1.getColumn( new Column( "data1_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "mydata_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "data_3" ) ) );
		assertNotNull( table1.getColumn( new Column( "the_data_mod" ) ) );

		assertNotNull( table1.getColumn( new Column( "embeddable_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "otherEntity_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "single_id_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "singleIdEntity2_id_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "client_option_MOD" ) ) );
		assertNotNull( table1.getColumn( new Column( "cop_mod" ) ) );

		final Table table2 = scope.getDomainModel()
				.getEntityBinding( OtherEntity.class.getName() + "_AUD" )
				.getTable();
		assertNotNull( table2.getColumn( new Column( "d_MOD" ) ) );
	}
}
