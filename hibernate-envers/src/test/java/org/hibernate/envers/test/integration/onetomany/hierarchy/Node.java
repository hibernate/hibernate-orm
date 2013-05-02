package org.hibernate.envers.test.integration.onetomany.hierarchy;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "NODES")
@Audited
public class Node implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@AuditJoinTable(name = "NODES_JOIN_TABLE_AUD",
					inverseJoinColumns = {@JoinColumn(name = "PARENT_ID", nullable = true, updatable = false)})
	@JoinTable(name = "NODES_JOIN_TABLE",
			   joinColumns = {@JoinColumn(name = "CHILD_ID", nullable = true, updatable = false)},
			   inverseJoinColumns = {@JoinColumn(name = "PARENT_ID", nullable = true, updatable = false)})
	private Node parent;

	@OneToMany(mappedBy = "parent")
	private List<Node> children = new ArrayList<Node>();

	public Node() {
	}

	public Node(String data, Node parent) {
		this.data = data;
		this.parent = parent;
	}

	public Node(String data, Long id) {
		this.id = id;
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Node) ) {
			return false;
		}

		Node node = (Node) o;

		if ( data != null ? !data.equals( node.data ) : node.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( node.id ) : node.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Node(id = " + id + ", data = " + data + ")";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public List<Node> getChildren() {
		return children;
	}

	public void setChildren(List<Node> children) {
		this.children = children;
	}
}
