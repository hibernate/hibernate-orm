//$Id$
package org.hibernate.test.annotations.cid;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


/**
 * @author Artur Legan
 */
@Entity
public class B {

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
