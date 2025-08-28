/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;
import java.io.Serializable;

/**
 * Implementation of Silly.
 *
 * @author Steve Ebersole
 */
public class Silly implements Serializable {
	private Long id;
	private String name;
	private Other other;

	public Silly() {
	}

	public Silly(String name) {
		this.name = name;
	}

	public Silly(String name, Other other) {
		this.name = name;
		this.other = other;
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

	public Other getOther() {
		return other;
	}

	public void setOther(Other other) {
		this.other = other;
	}
}
