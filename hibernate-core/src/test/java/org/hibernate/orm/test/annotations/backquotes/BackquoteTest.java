/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.backquotes;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jboss.logging.Logger;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Testcase for ANN-718 - @JoinTable / @JoinColumn fail when using backquotes in PK field name.
 *
 * @author Hardy Ferentschik
 */
public class BackquoteTest {

	private ServiceRegistry serviceRegistry;
	private SessionFactory sessionFactory;
	private final Logger log = Logger.getLogger( getClass() );

	@BeforeEach
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@AfterEach
	public void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@JiraKey(value = "ANN-718")
	public void testBackquotes() {
		try {
			Configuration config = new Configuration();
			config.addAnnotatedClass( Bug.class );
			config.addAnnotatedClass( Category.class );
			sessionFactory = config.buildSessionFactory( serviceRegistry );
		}
		catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace( new PrintWriter( writer ) );
			log.debug( writer.toString() );
			fail( e.getMessage() );
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
				sessionFactory = null;
			}
		}
	}

	/**
	 * HHH-4647 : Problems with @JoinColumn referencedColumnName and quoted column and table names
	 * <p>
	 * An invalid referencedColumnName to an entity having a quoted table name results in an
	 * infinite loop in o.h.c.Configuration$MappingsImpl#getPhysicalColumnName().
	 * The same issue exists with getLogicalColumnName()
	 */
	@Test
	@JiraKey(value = "HHH-4647")
	public void testInvalidReferenceToQuotedTableName() {
		try (BootstrapServiceRegistry serviceRegistry = new BootstrapServiceRegistryBuilder().build()) {
			Configuration config = new Configuration( serviceRegistry );
			config.addAnnotatedClass( Printer.class );
			config.addAnnotatedClass( PrinterCable.class );
			sessionFactory = config.buildSessionFactory( this.serviceRegistry );
			fail( "expected MappingException to be thrown" );
		}
		//we WANT MappingException to be thrown
		catch (MappingException e) {
			assertTrue( true, "MappingException was thrown");
		}
		catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace( new PrintWriter( writer ) );
			log.debug( writer.toString() );
			fail( e.getMessage() );
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
				sessionFactory = null;
			}
		}
	}
}
