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
