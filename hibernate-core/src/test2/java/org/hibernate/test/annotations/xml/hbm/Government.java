/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.xml.hbm;


/**
 * @author Emmanuel Bernard
 */
public class Government {
	private Integer id;
	private String name;
	private PrimeMinister primeMinister;

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

	public PrimeMinister getPrimeMinister() {
		return primeMinister;
	}

	public void setPrimeMinister(PrimeMinister primeMinister) {
		this.primeMinister = primeMinister;
	}
}
