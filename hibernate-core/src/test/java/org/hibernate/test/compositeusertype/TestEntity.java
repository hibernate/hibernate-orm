package org.hibernate.test.compositeusertype;

import java.util.Set;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class TestEntity {

	private int id;
	private Set<Unit> units;

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setUnits(Set<Unit> units) {
		this.units = units;
	}

	public Set<Unit> getUnits() {
		return units;
	}
}
