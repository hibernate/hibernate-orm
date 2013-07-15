package org.hibernate.test.readonly;
import java.io.Serializable;

/**
 * @author Steve Ebersole, Gail Badner (adapted this from "proxy" tests version)
 */
public class Owner implements Serializable {
	private Long id;
	private String name;

	public Owner() {
	}

	public Owner(String name) {
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
}
