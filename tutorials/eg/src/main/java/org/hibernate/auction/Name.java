//$Id: Name.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.auction;

/**
 * @author Gavin King
 */
public class Name {
	private String firstName;
	private String lastName;
	private Character initial;
	private Name() {}
	public Name(String first, Character middle, String last) {
		firstName = first;
		initial = middle;
		lastName = last;
	}
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public Character getInitial() {
		return initial;
	}

	public void setInitial(Character initial) {
		this.initial = initial;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer()
			.append(firstName)
			.append(' ');
		if (initial!=null) buf.append(initial)
			.append(' ');
		return buf.append(lastName)
			.toString();
	}

}
