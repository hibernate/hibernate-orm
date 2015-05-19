/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.manytoone;
import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

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
	@JoinColumns({
	@JoinColumn(name = "parentName"),
	@JoinColumn(name = "parentLevel")
			})
	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

}
