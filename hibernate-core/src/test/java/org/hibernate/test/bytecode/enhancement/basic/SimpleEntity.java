/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.basic;

import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Steve Ebersole
 */
@Entity
public class SimpleEntity {

	@Id
	private Long id;

	private String name;

	private boolean active;

	private long someNumber;

	Object anUnspecifiedObject;

	private List<String> someStrings;

	@OneToMany
	private Set<Integer> someInts;

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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public long getSomeNumber() {
		return someNumber;
	}

	public void setSomeNumber(long someNumber) {
		this.someNumber = someNumber;
	}

	public Object getAnObject() {
		return anUnspecifiedObject;
	}

	public void setAnObject(Object providedObject) {
		this.anUnspecifiedObject = providedObject;
	}

	public List<String> getSomeStrings() {
		return someStrings;
	}

	public void setSomeStrings(List<String> someStrings) {
		this.someStrings = someStrings;
	}

	public Set<Integer> getSomeInts() {
		return someInts;
	}

	public void setSomeInts(Set<Integer> someInts) {
		this.someInts = someInts;
	}
}
