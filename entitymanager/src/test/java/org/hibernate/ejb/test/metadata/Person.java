package org.hibernate.ejb.test.metadata;

import java.io.Serializable;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@IdClass(Person.PersonPK.class)
@Entity
public class Person {
	private String firstName;
	private String lastName;
	private Short age;

	@Id
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Id
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Short getAge() {
		return age;
	}

	public void setAge(Short age) {
		this.age = age;
	}

	public static class PersonPK implements Serializable {
		private String firstName;
		private String lastName;

		@Id
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		@Id
		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			PersonPK personPK = ( PersonPK ) o;

			if ( firstName != null ? !firstName.equals( personPK.firstName ) : personPK.firstName != null ) {
				return false;
			}
			if ( lastName != null ? !lastName.equals( personPK.lastName ) : personPK.lastName != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = firstName != null ? firstName.hashCode() : 0;
			result = 31 * result + ( lastName != null ? lastName.hashCode() : 0 );
			return result;
		}
	}
}
