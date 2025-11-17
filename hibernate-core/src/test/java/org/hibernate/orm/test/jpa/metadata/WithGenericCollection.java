/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
* This class has a List of mapped entity objects that are themselves parameterized.
* This class was added for JIRA issue #HHH-
*
* @author Kahli Burke
*/
@Entity
@Table(name = "WITH_GENERIC_COLLECTION")
public class WithGenericCollection<T> implements java.io.Serializable {
	@Id
	@Column(name = "ID")
	private String id;

	@Basic(optional=false)
	private double d;

	@ManyToOne(optional=false)
	@JoinColumn(name="PARENT_ID", insertable=false, updatable=false)
	private WithGenericCollection<? extends Object> parent = null;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name="PARENT_ID")
	private List<WithGenericCollection<? extends Object>> children = new ArrayList<WithGenericCollection<? extends Object>>();

	public WithGenericCollection() {
	}

	//====================================================================
	// getters and setters for State fields

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setD(double d) {
		this.d = d;
	}

	public double getD() {
		return d;
	}

	public List<WithGenericCollection<? extends Object>> getChildren() {
		return children;
	}

	public void setChildren(List<WithGenericCollection<? extends Object>> children) {
		this.children = children;
	}


}
