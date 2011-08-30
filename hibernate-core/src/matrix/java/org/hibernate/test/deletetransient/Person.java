package org.hibernate.test.deletetransient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * todo: describe Person
 *
 * @author Steve Ebersole
 */
public class Person {
	private Long id;
	private String name;
	private Set addresses = new HashSet();
	private Collection friends = new ArrayList();

	public Person() {
	}

	public Person(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set getAddresses() {
		return addresses;
	}

	public void setAddresses(Set addresses) {
		this.addresses = addresses;
	}

	public Collection getFriends() {
		return friends;
	}

	public void setFriends(Collection friends) {
		this.friends = friends;
	}
}
