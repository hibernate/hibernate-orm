/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.List;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.spi.MappingException;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.junit.Test;

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
		// should be @EmbeddedId, but embedded id are not working atm
		@EmbeddedId
		DependentId id;

		@MapsId("empid")
		@OneToMany
		List<Employee> emp; // maps the empid attribute of embedded id @ManyToOne Employee emp;
	}

	@Test
	public void testMapsIsOnOneToManyThrowsException() {
		try {
			sources = new MetadataSources( new StandardServiceRegistryBuilder().build() );
			sources.addAnnotatedClass( DependentId.class );
			sources.addAnnotatedClass( Dependent.class );
			sources.addAnnotatedClass( Employee.class );
			sources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
			assertTrue(
					e.getMessage().contains(
							"@MapsId can only be specified on a many-to-one or one-to-one associations"
					)
			);
			assertEquals(
					"Wrong error origin",
					MapsIdTest.Dependent.class.getName(),
					e.getOrigin().getName()
			);
		}
	}
}


