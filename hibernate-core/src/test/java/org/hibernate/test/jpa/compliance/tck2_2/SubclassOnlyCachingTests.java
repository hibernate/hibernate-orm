/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class SubclassOnlyCachingTests extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Person.class );
		sources.addAnnotatedClass( Employee.class );
		sources.addAnnotatedClass( Customer.class );
	}

	@Test
	public void testOnlySubclassIsCached() {

	}

	@Entity( name = "Person" )
	@Table( name = "persons" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static class Person {
		@Id
		public Integer id;
		public String name;
	}

	@Entity
	public static class Employee extends Person {
		public String employeeCode;
		public String costCenter;
	}

	@Entity
	@Cacheable()
	public static class Customer extends Person {
		public String erpCode;
	}

}
