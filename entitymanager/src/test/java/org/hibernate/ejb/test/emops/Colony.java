//$Id: $
package org.hibernate.ejb.test.emops;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Colony {
	@Id
	@GeneratedValue
	private Long id;


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
