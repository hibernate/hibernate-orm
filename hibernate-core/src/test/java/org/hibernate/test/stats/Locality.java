package org.hibernate.test.stats;


/**
 * @author Steve Ebersole
 */
public class Locality {
	private Long id;
	private String name;
	private Country country;

	public Locality() {
	}

	public Locality(String name, Country country) {
		this.name = name;
		this.country = country;
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

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}
}
