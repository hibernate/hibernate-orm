package org.hibernate.metamodel.source.annotations.xml.mocker;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Strong Liu
 */
@MappedSuperclass
@Access(AccessType.PROPERTY)
public class Person {
	private String name;
	private Long id;
	@Embedded
	@Access(AccessType.FIELD)
	private Address address;

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@Id
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
}
