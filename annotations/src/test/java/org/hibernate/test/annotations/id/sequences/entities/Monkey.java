//$
package org.hibernate.test.annotations.id.sequences.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Paul Cowan
 */
@Entity
public class Monkey {
	private String id;

	@Id
	@GeneratedValue(generator = "system-uuid-2")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
