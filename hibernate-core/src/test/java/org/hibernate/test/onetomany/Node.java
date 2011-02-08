/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.test.onetomany;
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
