/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@BaseUnitTest
public class PersistentClassTest {

	private StandardServiceRegistry serviceRegistry;
	private MetadataBuildingContext metadataBuildingContext;

	@BeforeEach
	public void prepare() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
	}

	@AfterEach
	public void release() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	public void testGetMappedClass() {
		RootClass pc = new RootClass( metadataBuildingContext );
		pc.setClassName( String.class.getName() );
		assertEquals( String.class.getName(), pc.getClassName() );
		assertEquals( String.class, pc.getMappedClass() );
		pc.setClassName( Integer.class.getName() );
		assertEquals( Integer.class, pc.getMappedClass() );
	}

	@Test
	public void testGetProxyInterface() {
		RootClass pc = new RootClass( metadataBuildingContext );
		pc.setProxyInterfaceName( String.class.getName() );
		assertEquals( String.class.getName(), pc.getProxyInterfaceName() );
		assertEquals( String.class, pc.getProxyInterface() );
		pc.setProxyInterfaceName( Integer.class.getName() );
		assertEquals( Integer.class, pc.getProxyInterface() );
	}

	@Test
	public void testGetProperty() {
		RootClass pc = new RootClass( metadataBuildingContext );
		Property p = new Property();
		p.setName( "name" );
		pc.addProperty( p );
		assertEquals( p, pc.getProperty( "name" ) );
		assertEquals( p, pc.getProperty( "name.test" ) );
		try {
			assertNull( pc.getProperty( "test" ) );
			fail( "MappingException expected" );
		}
		catch (MappingException e) {
			// expected
		}
	}

}
