//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.test.cascade.circle;
import java.util.HashSet;
import java.util.Set;


public class Tour {
	
//	@Id
//	@SequenceGenerator(name="TOUR_SEQ", sequenceName="TOUR_SEQ", initialValue=1, allocationSize=1)
//	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="TOUR_SEQ")
	private Long tourID;

	private long version;

	private String name;

	/** A List of nodes contained in this tour. */
//	@OneToMany(targetEntity=Node.class, fetch=FetchType.LAZY, cascade={CascadeType.MERGE, CascadeType.REFRESH}, mappedBy="tour")
	private Set nodes = new HashSet(0);

	public String getName() {
		return name;
	}

	protected void setTourID(Long tourID) {
		this.tourID = tourID;
	}

	public long getVersion() {
		return version;
	}

	protected void setVersion(long version) {
		this.version = version;
	}	

	public void setName(String name) {
		this.name = name;
	}

	public Set getNodes() {
		return nodes;
	}

	public void setNodes(Set nodes) {
		this.nodes = nodes;
	}

	public Long getTourID() {
		return tourID;
	}
}
