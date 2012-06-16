package org.hibernate.test.deletetransient;
import java.util.HashSet;
import java.util.Set;

/**
 * todo: describe Address
 *
 * @author Steve Ebersole
 */
public class Address {
	private Long id;
	private String info;
	private Set suites = new HashSet();

	public Address() {
	}

	public Address(String info) {
		this.info = info;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public Set getSuites() {
		return suites;
	}

	public void setSuites(Set suites) {
		this.suites = suites;
	}
}
