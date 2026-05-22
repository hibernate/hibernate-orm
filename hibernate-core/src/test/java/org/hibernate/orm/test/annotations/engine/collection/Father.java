/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.engine.collection;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;


/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "co_father")
public class Father {
	@Id
	@GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) {  this.id = id; }
	private Integer id;

	@OneToMany(cascade = { jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE })
	@OrderColumn(name = "son_arriv")
	@JoinColumn(name = "father_id", nullable = false)
	public List<Son> getOrderedSons() { return orderedSons; }
	public void setOrderedSons(List<Son> orderedSons) {  this.orderedSons = orderedSons; }
	private List<Son> orderedSons = new ArrayList<>( );
}
