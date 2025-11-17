/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.engine.collection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="co_son")
public class Son {
	@Id
	@GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) {  this.id = id; }
	private Integer id;

	@ManyToOne(optional = false) @JoinColumn(name = "father_id", insertable = false, updatable = false, nullable = false)
	public Father getFather() { return father; }
	public void setFather(Father father) {  this.father = father; }
	private Father father;

	@ManyToOne
	public Mother getMother() { return mother; }
	public void setMother(Mother mother) {  this.mother = mother; }
	private Mother mother;
}
