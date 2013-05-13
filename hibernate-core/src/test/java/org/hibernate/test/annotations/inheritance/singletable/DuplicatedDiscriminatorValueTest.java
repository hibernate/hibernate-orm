package org.hibernate.test.annotations.inheritance.singletable;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue( jiraKey = "HHH-7214" )
public class DuplicatedDiscriminatorValueTest extends BaseUnitTestCase {
	private static final String DISCRIMINATOR_VALUE = "D";

	@Test
	public void testDuplicatedDiscriminatorValueSameHierarchy() {
		try {
			tryBuildingSessionFactory( Building.class, Building1.class, Building2.class );
			Assert.fail( MappingException.class.getName() + " expected when two subclasses are mapped with the same discriminator value." );
		}
		catch ( MappingException e ) {
			final String errorMsg = e.getCause().getMessage();
			// Check if error message contains descriptive information.
			Assert.assertTrue( errorMsg.contains( Building1.class.getName() ) );
			Assert.assertTrue( errorMsg.contains( Building2.class.getName() ) );
			Assert.assertTrue( errorMsg.contains( "discriminator value '" + DISCRIMINATOR_VALUE + "'." ) );
		}
	}

	@Test
	public void testDuplicatedDiscriminatorValueDifferentHierarchy() {
		tryBuildingSessionFactory( Building.class, Building1.class, Furniture.class, Chair.class );
	}

	private void tryBuildingSessionFactory(Class... annotatedClasses) {
		Configuration cfg = new Configuration();
		for ( Class annotatedClass : annotatedClasses ) {
			cfg.addAnnotatedClass( annotatedClass );
		}
		ServiceRegistry serviceRegistry = null;
		SessionFactory sessionFactory = null;
		try {
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
	}

	@Entity
	@DiscriminatorValue(DISCRIMINATOR_VALUE) // Duplicated discriminator value in single hierarchy.
	private static class Building1 extends Building {
	}

	@Entity
	@DiscriminatorValue(DISCRIMINATOR_VALUE) // Duplicated discriminator value in single hierarchy.
	private static class Building2 extends Building {
	}

	@Entity
	@DiscriminatorColumn(name = "entity_type")
	@DiscriminatorValue("F")
	private static class Furniture {
		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity
	@DiscriminatorValue(DISCRIMINATOR_VALUE) // Duplicated discriminator value in different hierarchy.
	private static class Chair extends Furniture {
	}
}
