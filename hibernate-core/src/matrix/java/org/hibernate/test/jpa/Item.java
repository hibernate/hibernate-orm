package org.hibernate.test.jpa;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class Item {
	private Long id;
	private String name;
	private long version;
	private Set parts = new HashSet();

	public Item() {
	}

	public Item(String name) {
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

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Set getParts() {
		return parts;
	}

	public void setParts(Set parts) {
		this.parts = parts;
	}
}
