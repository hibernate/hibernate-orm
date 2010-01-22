package org.hibernate.jpamodelgen.test.rawTypes;

import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class B implements java.io.Serializable {

	@Id
	protected String id;

	@Basic
	protected String name;

	@Basic
	protected int value;


	@ManyToMany(targetEntity = A.class, mappedBy = "bCol", cascade = CascadeType.ALL)
	protected Collection aCol = new java.util.ArrayList();


	public B() {
	}


	public Collection getACol() {
		return aCol;
	}

	public void setACol(Collection aCol) {
		this.aCol = aCol;
	}

	public String getBId() {
		return id;
	}

	public String getBName() {
		return name;
	}

	public int getBValue() {
		return value;
	}
}
