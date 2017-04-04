package org.hibernate.test.annotations.embeddables.collection.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <code>ContactInformation</code> -
 *
 * @author Vlad Mihalcea
 */
public class ContactInformation implements Serializable {

	private String name;

	private List<ContactType> contactType = new ArrayList<>();

	public List<ContactType> getContactType() {
		return contactType;
	}

	public void setContactType(final List<ContactType> contactType) {
		this.contactType = contactType;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ContactInformation that = (ContactInformation) o;
		return Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}
}
