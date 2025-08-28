/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class ComponentOwner {
	private Long id;
	private String name;
	private Component component;

	public ComponentOwner() {
	}

	public ComponentOwner(String name) {
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

	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}

	public static class Component {
		private int generated;

		public int getGenerated() {
			return generated;
		}

		public void setGenerated(int generated) {
			this.generated = generated;
		}
	}
}
