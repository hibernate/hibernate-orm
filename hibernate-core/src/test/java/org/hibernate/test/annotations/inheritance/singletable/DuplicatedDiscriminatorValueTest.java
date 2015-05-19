/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.inheritance.singletable;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.SessionFactoryRegistry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

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

		assertFalse( SessionFactoryRegistry.INSTANCE.hasRegistrations() );
	}

	@Test
	public void testDuplicatedDiscriminatorValueDifferentHierarchy() {
		tryBuildingSessionFactory( Building.class, Building1.class, Furniture.class, Chair.class );
	}

	private void tryBuildingSessionFactory(Class... annotatedClasses) {
		final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
		try {
			final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
			for ( Class annotatedClass : annotatedClasses ) {
				metadataSources.addAnnotatedClass( annotatedClass );
			}

			final Metadata metadata = metadataSources.buildMetadata();
			final SessionFactory sessionFactory = metadata.buildSessionFactory();
			sessionFactory.close();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
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
