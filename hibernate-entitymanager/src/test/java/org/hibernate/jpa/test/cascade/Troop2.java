package org.hibernate.jpa.test.cascade;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Troop2 implements Serializable {

	/* FAILS: */
	@Id
	@GeneratedValue
	private Integer id;
	private String name;

	/* WORKS:
	@Id
	@GeneratedValue
	*/
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
