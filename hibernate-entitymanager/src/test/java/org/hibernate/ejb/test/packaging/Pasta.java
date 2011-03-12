package org.hibernate.ejb.test.packaging;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Pasta {
	@Id @GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id;}
	private Integer id;

	public String getType() { return type; }
	public void setType(String type) { this.type = type;}
	private String type;
}
