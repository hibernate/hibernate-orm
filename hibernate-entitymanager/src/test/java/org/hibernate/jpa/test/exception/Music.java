// $Id$
package org.hibernate.jpa.test.exception;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SuppressWarnings("serial")
public class Music implements Serializable {
	private Integer id;
	private Integer version;
	private String name;

	@Id @GeneratedValue public Integer getId() {
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

	@Version 
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
}
