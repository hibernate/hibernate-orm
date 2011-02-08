//$Id: Name.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.hql;


/**
 * @author Gavin King
 */
public class Name {
	private String first;
	private Character initial;
	private String last;
	
	protected Name() {}
	
	public Name(String first, Character initial, String last) {
		this.first = first;
		this.initial = initial;
		this.last = last;
	}

	public Name(String first, char initial, String last) {
		this( first, new Character( initial ), last );
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public Character getInitial() {
		return initial;
	}

	public void setInitial(Character initial) {
		this.initial = initial;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}
}
