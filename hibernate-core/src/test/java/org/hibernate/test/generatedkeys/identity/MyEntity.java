package org.hibernate.test.generatedkeys.identity;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class MyEntity {
	private Long id;
	private String name;
	private MySibling sibling;
	private Set nonInverseChildren = new HashSet();
	private Set inverseChildren = new HashSet();

	public MyEntity() {
	}

	public MyEntity(String name) {
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

	public MySibling getSibling() {
		return sibling;
	}

	public void setSibling(MySibling sibling) {
		this.sibling = sibling;
	}

	public Set getNonInverseChildren() {
		return nonInverseChildren;
	}

	public void setNonInverseChildren(Set nonInverseChildren) {
		this.nonInverseChildren = nonInverseChildren;
	}

	public Set getInverseChildren() {
		return inverseChildren;
	}

	public void setInverseChildren(Set inverseChildren) {
		this.inverseChildren = inverseChildren;
	}
}
