/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.manytoone;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class TreeType {
	private Integer id;
	private String name;
	private ForestType forestType;
	private ForestType alternativeForestType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinTable(name="Tree_Forest")
	public ForestType getForestType() {
		return forestType;
	}

	public void setForestType(ForestType forestType) {
		this.forestType = forestType;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinTable(name="Atl_Forest_Type",
		joinColumns = @JoinColumn(name="tree_id"),
		inverseJoinColumns = @JoinColumn(name="forest_id") )
	public ForestType getAlternativeForestType() {
		return alternativeForestType;
	}

	public void setAlternativeForestType(ForestType alternativeForestType) {
		this.alternativeForestType = alternativeForestType;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
