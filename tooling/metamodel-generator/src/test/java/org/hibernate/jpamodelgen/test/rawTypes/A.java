package org.hibernate.jpamodelgen.test.rawTypes;

import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class A implements java.io.Serializable {

	@Id
	protected String id;

	@Basic
	protected String name;

	@Basic
	protected int value;


	public A() {
	}

	@ManyToMany(targetEntity = B.class, cascade = CascadeType.ALL)
	@JoinTable(name = "tbl_A_B",
			joinColumns =
			@JoinColumn(
					name = "A_FK", referencedColumnName = "ID"),
			inverseJoinColumns =
			@JoinColumn(
					name = "B_FK", referencedColumnName = "ID")
	)
	protected Collection bCol = new java.util.ArrayList();


	public Collection getBCol() {
		return bCol;
	}

	public void setBCol(Collection bCol) {
		this.bCol = bCol;
	}

	public String getAId() {
		return id;
	}

	public String getAName() {
		return name;
	}

	public void setAName(String aName) {
		this.name = aName;
	}


	public int getAValue() {
		return value;
	}
}