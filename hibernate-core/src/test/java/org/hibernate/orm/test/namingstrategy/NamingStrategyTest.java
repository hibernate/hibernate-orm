/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak
 */
public class NamingStrategyTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class
		};
	}

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] {
				"org/hibernate/orm/test/namingstrategy/Customers.hbm.xml"
		};
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		builder.applySetting( AvailableSettings.IMPLICIT_NAMING_STRATEGY, TestNamingStrategy.class.getName() );
		builder.applySetting( AvailableSettings.PHYSICAL_NAMING_STRATEGY, TestNamingStrategy.class.getName() );
	}


	@Test
	public void testDatabaseColumnNames() {
		PersistentClass classMapping = getMetadata().getEntityBinding( Customers.class.getName() );
		Column stateColumn = (Column) classMapping.getProperty( "specified_column" ).getSelectables().get( 0 );
		assertEquals( "CN_specified_column", stateColumn.getName() );
	}

	@Test
	@JiraKey(value = "HHH-5848")
	public void testDatabaseTableNames() {
		PersistentClass classMapping = getMetadata().getEntityBinding( Item.class.getName() );
		Column secTabColumn = (Column) classMapping.getProperty( "specialPrice" ).getSelectables().get( 0 );
		assertEquals( "TAB_ITEMS_SEC", secTabColumn.getValue().getTable().getName() );
		Column tabColumn = (Column) classMapping.getProperty( "price" ).getSelectables().get( 0 );
		assertEquals( "TAB_ITEMS", tabColumn.getValue().getTable().getName() );
	}
}
