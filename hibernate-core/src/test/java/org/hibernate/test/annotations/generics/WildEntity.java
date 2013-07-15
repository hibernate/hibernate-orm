//$Id$
package org.hibernate.test.annotations.generics;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class WildEntity implements Serializable {

	private static final long serialVersionUID = -1171578628576139205L;

	private int id;

	private String property;

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	@Transient
	public <T extends Object> T someMethod() {
		return null;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}
}
