/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.entities;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Comment
 * 
 * @author Brian Stansberry
 */
public class AccountHolder implements Serializable {
	private static final long serialVersionUID = 1L;

	private String lastName;
	private String ssn;
	private transient boolean deserialized;

	public AccountHolder() {
		this("Stansberry", "123-456-7890");
	}

	public AccountHolder(String lastName, String ssn) {
		this.lastName = lastName;
		this.ssn = ssn;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getSsn() {
		return ssn;
	}

	public void setSsn(String ssn) {
		this.ssn = ssn;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof AccountHolder))
			return false;
		AccountHolder pk = (AccountHolder) obj;
		if (!lastName.equals(pk.lastName))
			return false;
		if (!ssn.equals(pk.ssn))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = result * 31 + lastName.hashCode();
		result = result * 31 + ssn.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getName());
		sb.append("[lastName=");
		sb.append(lastName);
		sb.append(",ssn=");
		sb.append(ssn);
		sb.append(",deserialized=");
		sb.append(deserialized);
		sb.append("]");
		return sb.toString();
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		deserialized = true;
	}

}
