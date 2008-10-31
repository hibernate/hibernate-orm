//$Id: $
package org.hibernate.ejb.test.callbacks;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;

/**
 * @author Emmanuel Bernard
 */
@Entity
@EntityListeners(ExceptionListener.class)
public class Rythm {
	@Id @GeneratedValue private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
