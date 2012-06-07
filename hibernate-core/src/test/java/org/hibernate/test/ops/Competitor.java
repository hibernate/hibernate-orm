//$Id: $
package org.hibernate.test.ops;


/**
 * @author Emmanuel Bernard
 */
public class Competitor {
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
