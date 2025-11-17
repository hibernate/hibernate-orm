/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;
import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Node implements Serializable {

	private NodePk id;
	private String description;
	private Node parent;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Node ) ) return false;

		final Node node = (Node) o;

		if ( !id.equals( node.id ) ) return false;

		return true;
	}

	public int hashCode() {
		return id.hashCode();
	}

	@Id
	public NodePk getId() {
		return id;
	}

	public void setId(NodePk id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "parentName")
	@JoinColumn(name = "parentLevel")
	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

}
