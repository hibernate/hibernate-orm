//$Id$
package org.hibernate.test.annotations.manytomany;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
class Inspector implements Serializable {
	@Id
	@GeneratedValue
	@Column(name = "id")
	private Long _id;

	private String name;

	public Long getId() {
		return _id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

