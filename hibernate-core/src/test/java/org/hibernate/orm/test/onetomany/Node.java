/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Node implements Serializable {

	private Integer id;
	private long version;
	private Node parentNode;
	private String description;
	private List subNodes = new ArrayList();

	public Node() {
	}

	public Node(int id, String description) {
		setId( id );
		setDescription( description );
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Node getParentNode() {
		return parentNode;
	}

	public void setParentNode(Node parentNode) {
		this.parentNode = parentNode;
	}

	public List getSubNodes() {
		return subNodes;
	}

	public void setSubNodes(List subNodes) {
		this.subNodes = subNodes;
	}

	public void addSubNode(Node subNode) {
		subNodes.add( subNode );
		subNode.setParentNode( this );
	}

	public void removeSubNode(Node subNode) {
		subNodes.remove( subNode );
		subNode.setParentNode( null );
	}
}
