/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.exceptionhandling;

import static org.junit.Assert.fail;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-12666")
@RequiresDialect(H2Dialect.class)
public class IdentifierGenerationExceptionHandlingTest extends BaseExceptionHandlingTest {

	public IdentifierGenerationExceptionHandlingTest(
			BootstrapMethod bootstrapMethod,
			ExceptionHandlingSetting exceptionHandlingSetting,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionHandlingSetting, exceptionExpectations );
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
