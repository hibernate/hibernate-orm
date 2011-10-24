// $Id$
package org.hibernate.test.annotations.immutable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Immutable;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Foobar {
	@Id
	@GeneratedValue
	private Integer id;
	
	@Immutable
	private String name;

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
