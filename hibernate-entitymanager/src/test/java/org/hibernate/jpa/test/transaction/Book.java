//$Id$
package org.hibernate.jpa.test.transaction;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Book {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	public Integer id;
	public String name;

	@Version
	public Integer version;
	
	public Book() {}
	
	public Book(String name, Integer version) {
		this.name = name;
		this.version = version;
	}

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

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
}
