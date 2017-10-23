/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.generated;


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
