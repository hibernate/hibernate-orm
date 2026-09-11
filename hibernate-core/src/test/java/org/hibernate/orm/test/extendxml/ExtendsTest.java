/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.extendxml;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

/**
 * @author Gavin King
 */
public class ExtendsTest {
	private StandardServiceRegistryImpl serviceRegistry;

	@BeforeEach
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry();
	}

	@AfterEach
	public void tearDown() {
		ServiceRegistryBuilder.destroy( serviceRegistry );
	}

	private String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Test
	public void testAllInOne() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addResource( getBaseForMappings() + "extendxml/allinone.orm.xml" )
				.buildMetadata();

		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Customer" ) );
		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Person" ) );
		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Employee" ) );
	}

	@Test
	public void testOutOfOrder() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addResource( getBaseForMappings() + "extendxml/Customer.orm.xml" )
				.addResource( getBaseForMappings() + "extendxml/Person.orm.xml" )
				.addResource( getBaseForMappings() + "extendxml/Employee.orm.xml" )
				.buildMetadata();

		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Customer" ) );
		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Person" ) );
		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Employee" ) );
	}

	@Test
	public void testNwaitingForSuper() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addResource( getBaseForMappings() + "extendxml/Customer.orm.xml" )
				.addResource( getBaseForMappings() + "extendxml/Employee.orm.xml" )
				.addResource( getBaseForMappings() + "extendxml/Person.orm.xml" )
				.buildMetadata();

		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Customer" ) );
		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Person" ) );
		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Employee" ) );
	}

	@Test
	public void testMissingSuper() {
		try {
			Metadata metadata = new MetadataSources( serviceRegistry )
					.addResource( getBaseForMappings() + "extendxml/Customer.orm.xml" )
					.addResource( getBaseForMappings() + "extendxml/Employee.orm.xml" )
					.buildMetadata();
			fail( "Should not be able to build sessionFactory without a Person" );
		}
		catch ( HibernateException e ) {
		}
	}

	@Test
	public void testAllSeparateInOne() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addResource( getBaseForMappings() + "extendxml/allseparateinone.orm.xml" )
				.buildMetadata();

		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Customer" ) );
		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Person" ) );
		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Employee" ) );
	}

	@Test
	public void testJoinedSubclassAndEntityNamesOnly() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addResource( getBaseForMappings() + "extendxml/entitynames.orm.xml" )
				.buildMetadata();

		assertNotNull( metadata.getEntityBinding( "EntityHasName" ) );
		assertNotNull( metadata.getEntityBinding( "EntityCompany" ) );
	}

	@Test
	public void testEntityNamesWithPackage() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addResource( getBaseForMappings() + "extendxml/packageentitynames.orm.xml" )
				.buildMetadata();

		assertNotNull( metadata.getEntityBinding( "EntityHasName" ) );
		assertNotNull( metadata.getEntityBinding( "EntityCompany" ) );
	}

	@Test
	public void testUnionSubclass() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addResource( getBaseForMappings() + "extendxml/unionsubclass.orm.xml" )
				.buildMetadata();

		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Person" ) );
		assertNotNull( metadata.getEntityBinding( "org.hibernate.orm.test.extendxml.Customer" ) );
	}

}
