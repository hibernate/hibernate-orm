//$Id$
package org.hibernate.jpa.test.cascade;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Son {
	@Id
	@GeneratedValue
	public Integer id;
	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	public Parent parent;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
