/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import static org.junit.Assert.fail;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

@JiraKey(value = "HHH-12666")
@RequiresDialect(H2Dialect.class)
public class IdentifierGenerationExceptionHandlingTest extends BaseExceptionHandlingTest {

	public IdentifierGenerationExceptionHandlingTest(
			BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionExpectations );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Owner.class,
				OwnerAddress.class
		};
	}

	@Test
	public void testIdentifierGeneratorException() {
		OwnerAddress address = new OwnerAddress();
		address.owner = null;

		Session s = openSession();
		s.beginTransaction();
		try {
			s.persist( address );
			s.flush();
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onIdentifierGeneratorFailure( expected );
		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	@Entity(name = "OwnerAddress")
	public static class OwnerAddress {
		@Id
		@GeneratedValue(generator = "fk_1")
		@GenericGenerator(strategy = "foreign", name = "fk_1", parameters = @Parameter(name = "property", value = "owner"))
		private Integer id;

		@OneToOne(mappedBy = "address")
		private Owner owner;
	}

	@Entity(name = "Owner")
	public static class Owner {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToOne(cascade = CascadeType.ALL)
		@PrimaryKeyJoinColumn
		private OwnerAddress address;
	}

}
