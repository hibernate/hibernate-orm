//$Id$
package org.hibernate.jpa.test.lock;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

/**
 * @author Emmanuel Bernard
 */
@Entity
@NamedQuery(name = "getAll", query = "select u from UnversionedLock u",
hints = @QueryHint( name = "javax.persistence.lock.timeout", value = "3000"))
public class UnversionedLock {
	@Id
	@GeneratedValue
	private Integer id;
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
