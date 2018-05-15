/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.set;
import java.util.HashSet;
import java.util.Set;

/**
 * Container implementation
 *
 * @author Steve Ebersole
 */
public class Container {
	private Long id;
	private String name;
	private Set contents = new HashSet();

	public Container() {
	}

	public Container(String name) {
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

	public Set getContents() {
		return contents;
	}

	public void setContents(Set contents) {
		this.contents = contents;
	}

	public static class Content {
		private String name;

		public Content() {
		}

		public Content(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
