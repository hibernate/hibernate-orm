//$Id$
package org.hibernate.test.annotations.inheritance.joined;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SecondaryTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@SecondaryTable(name="POOL_ADDRESS")
@org.hibernate.annotations.Table(appliesTo="POOL_ADDRESS", optional=true)
public class Pool {
	@Id @GeneratedValue 
	private Integer id;
	
	@Embedded
	private PoolAddress address;

	public PoolAddress getAddress() {
		return address;
	}

	public void setAddress(PoolAddress address) {
		this.address = address;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
