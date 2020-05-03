/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.manytoone;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.ForeignKey;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class ForestType {
	private Integer id;
	private String name;
	private Set<TreeType> trees;
	private BiggestForest biggestRepresentative;

	@OneToOne
	@JoinTable(name="BiggestRepPerForestType",
		joinColumns = @JoinColumn(name="forest_type"),
		inverseJoinColumns = @JoinColumn(name="forest")
	)
	@ForeignKey(name="A_TYP_FK",
			inverseName = "A_FOR_FK" //inverse fail cause it involves a Join
	)
	public BiggestForest getBiggestRepresentative() {
		return biggestRepresentative;
	}

	public void setBiggestRepresentative(BiggestForest biggestRepresentative) {
		this.biggestRepresentative = biggestRepresentative;
	}

	@OneToMany(mappedBy="forestType")
	public Set<TreeType> getTrees() {
		return trees;
	}

	public void setTrees(Set<TreeType> trees) {
		this.trees = trees;
	}

	@Id @GeneratedValue
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
