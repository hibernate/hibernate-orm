//$Id: $
package org.hibernate.test.sql.hand;


/**
 * @author Emmanuel Bernard
 */
public class Speech {
	private Integer id;
	private String name;
	private Double length;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Double getLength() {
		return length;
	}

	public void setLength(Double length) {
		this.length = length;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
