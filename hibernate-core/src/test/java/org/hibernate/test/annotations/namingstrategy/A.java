package org.hibernate.test.annotations.namingstrategy;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "AEC")
public class A implements java.io.Serializable {
	@Id
	protected String id;
	protected String name;
	protected int value;

	@ElementCollection
	protected Set<AddressEntry> address = new HashSet();

	public A() {
	}

	public A(String id, String name, int value) {
		this.id = id;
		this.name = name;
		this.value = value;
	}

	// Default to table A_AddressEntry
	public Set<AddressEntry> getAddress() {
		return address;
	}

	public void setAddress(Set<AddressEntry> addr) {
		this.address = addr;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int val) {
		this.value = val;
	}
}
