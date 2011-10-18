package org.hibernate.test.annotations.fetchprofile;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="Order_Country")
public class Country {
	@Id @GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	private Integer id;

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;
}
