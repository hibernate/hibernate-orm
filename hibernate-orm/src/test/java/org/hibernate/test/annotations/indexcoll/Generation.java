/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.indexcoll;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Generation {

	private String age;
	private String culture;
	private SubGeneration subGeneration;

	public String getAge() {
		return age;
	}
	public void setAge(String age) {
		this.age = age;
	}
	public String getCulture() {
		return culture;
	}
	public void setCulture(String culture) {
		this.culture = culture;
	}
	public SubGeneration getSubGeneration() {
		return subGeneration;
	}
	public void setSubGeneration(SubGeneration subGeneration) {
		this.subGeneration = subGeneration;
	}

	@Embeddable
	public static class SubGeneration {
		private String description;

		public SubGeneration() {
		}

		public SubGeneration(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
