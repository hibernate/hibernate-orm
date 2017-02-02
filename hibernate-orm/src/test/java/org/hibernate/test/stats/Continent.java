/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Continent.java 6736 2005-05-09 16:09:38Z epbernard $
package org.hibernate.test.stats;
import java.util.Set;

/**
 * @author Emmanuel Bernard
 */
public class Continent {
	private Integer id;
	private String name;
	private Set countries;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set getCountries() {
		return countries;
	}

	public void setCountries(Set countries) {
		this.countries = countries;
	}

}
