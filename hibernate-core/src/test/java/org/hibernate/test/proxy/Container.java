package org.hibernate.test.proxy;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class Container implements Serializable {
	private Long id;
	private String name;
	private Owner owner;
	private Info info;
	private Set dataPoints = new HashSet();

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

	public Owner getOwner() {
		return owner;
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	public Info getInfo() {
		return info;
	}

	public void setInfo(Info info) {
		this.info = info;
	}

	public Set getDataPoints() {
		return dataPoints;
	}

	public void setDataPoints(Set dataPoints) {
		this.dataPoints = dataPoints;
	}
}
