/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

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
		inverseJoinColumns = @JoinColumn(name="forest"),
		foreignKey = @ForeignKey(name="A_TYP_FK"),
		inverseForeignKey = @ForeignKey(name="A_FOR_FK") //inverse fail cause it involves a Join
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
