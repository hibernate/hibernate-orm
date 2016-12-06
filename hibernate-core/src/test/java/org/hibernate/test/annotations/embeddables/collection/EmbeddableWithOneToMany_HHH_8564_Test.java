/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables.collection;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.AnnotationException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-8564")
public class EmbeddableWithOneToMany_HHH_8564_Test
		extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				User.class,
		};
	}

	protected void buildSessionFactory() {
		try {
			super.buildSessionFactory();
			fail( "Should throw AnnotationException!" );
		}
		catch ( AnnotationException expected ) {
			assertTrue( expected.getMessage().startsWith(
					"@OneToMany, @ManyToMany or @ElementCollection cannot be used inside an @Embeddable that is also contained within an @ElementCollection"
			) );
		}
	}

	@Test
	public void test() {
	}

	@Embeddable
	public static class Address {

		@ElementCollection(fetch = FetchType.EAGER)
		@Enumerated(EnumType.STRING)
		private Set<AddressType> type;

		@NotNull
		@Size(min = 3, max = 200)
		private String street;

		@NotNull
		@Pattern(regexp = "[0-9]{5}")
		private String zipcode;

		@NotNull
		@Size(min = 3, max = 60)
		private String city;

		@NotNull
		@Size(min = 3, max = 60)
		private String state;

	}

	public static enum AddressType {

		OFFICE, HOME, BILLING

	}

	@Entity
	@Table(name = "users")
	@SuppressWarnings("serial")
	public static class User implements Serializable {

		@Id
		@NotNull
		private String email;

		@NotNull
		private String password;

		@NotNull
		private String name;

		@NotNull
		private String surname;

		@ElementCollection(fetch = FetchType.EAGER)
		private Set<Address> addresses;

		@Version
		private long version;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( email == null ) ?
					0 :
					email.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			User other = (User) obj;
			if ( email == null ) {
				if ( other.email != null ) {
					return false;
				}
			}
			else if ( !email.equals( other.email ) ) {
				return false;
			}
			return true;
		}

	}
}
