/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class Hive {
	private long id;
	private Location location;
	private List members = new ArrayList();
	public List getMembers() {
		return members;
	}
	public void setMembers(List hives) {
		this.members = hives;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location;
	}
}
