//$Id: $
package org.hibernate.test.nonflushedchanges;

import java.io.Serializable;

/**
 * @author Emmanuel Bernard, Gail Badner (adapted this from "ops" tests version)
 */
public class Competitor implements Serializable {
	public Integer id;
	private String name;


	public Competitor() {
	}

	public Competitor(String name) {
		this.name = name;
	}

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
}
