//$Id: $
package org.hibernate.ejb.test.pack.defaultpar;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Money {
	private Integer id;

	@Id @GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}
