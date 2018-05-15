/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.inheritance.joined;
import org.hibernate.annotations.Tables;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@SecondaryTables({
	@SecondaryTable(name="POOL_ADDRESS"),
	@SecondaryTable(name="POOL_ADDRESS_2")
})
@Tables({
	@org.hibernate.annotations.Table(appliesTo="POOL_ADDRESS", optional=true),
	@org.hibernate.annotations.Table(appliesTo="POOL_ADDRESS_2", optional=true, inverse = true)
})
public class Pool {
	@Id @GeneratedValue 
	private Integer id;
	
	@Embedded
	private PoolAddress address;

	@Embedded
	@AttributeOverride(name = "address", column = @Column(table = "POOL_ADDRESS_2"))
	private PoolAddress secondaryAddress;

	public PoolAddress getAddress() {
		return address;
	}

	public void setAddress(PoolAddress address) {
		this.address = address;
	}

	public PoolAddress getSecondaryAddress() {
		return secondaryAddress;
	}

	public void setSecondaryAddress(PoolAddress secondaryAddress) {
		this.secondaryAddress = secondaryAddress;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
