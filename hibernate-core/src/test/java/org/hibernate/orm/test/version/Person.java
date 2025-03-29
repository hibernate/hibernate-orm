/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;
import java.util.ArrayList;
import java.util.List;

public class Person {
	private String name;
	private List things;
	private List tasks;
	private int version;

	Person() {}
	public Person(String name) {
		this.name = name;
		this.things = new ArrayList();
		this.tasks = new ArrayList();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List getThings() {
		return things;
	}
	public void setThings(List things) {
		this.things = things;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public List getTasks() {
		return tasks;
	}
	public void setTasks(List tasks) {
		this.tasks = tasks;
	}
}
