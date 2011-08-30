//$Id$
package org.hibernate.test.annotations.manytoone;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Color showing a surrogate key and a unique constraint to ensure business rule
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Color {
	private Integer id;
	private String name;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(unique = true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
