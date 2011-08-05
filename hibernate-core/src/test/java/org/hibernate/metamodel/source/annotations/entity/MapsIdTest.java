package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;

import org.junit.Test;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.service.ServiceRegistryBuilder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class MapsIdTest extends BaseAnnotationBindingTestCase {
	@Entity
	public class Employee {
		@Id
		long empId;
		String name;
	}

	@Embeddable
	public class DependentId {
		String name;
		long empid; // corresponds to PK type of Employee
	}

	@Entity
	public class Dependent {
		@Id
		// should be @EmbeddedId, but embedded id are not working atm
				DependentId id;

		@MapsId("empid")
		@OneToMany
		Employee emp; // maps the empid attribute of embedded id @ManyToOne Employee emp;
	}

	@Test
	@Resources(annotatedClasses = DependentId.class)
	public void testMapsIsOnOneToManyThrowsException() {
		try {
			sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
			sources.addAnnotatedClass( DependentId.class );
			sources.addAnnotatedClass( Dependent.class );
			sources.addAnnotatedClass( Employee.class );
			sources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
			assertTrue(
					e.getMessage()
							.startsWith( "@MapsId can only be specified on a many-to-one or one-to-one associations" )
			);
			assertEquals(
					"Wrong error origin",
					"org.hibernate.metamodel.source.annotations.entity.MapsIdTest$Dependent",
					e.getOrigin().getName()
			);
		}
	}
}


