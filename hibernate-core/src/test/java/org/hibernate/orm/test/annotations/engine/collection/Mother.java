/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.engine.collection;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "co_mother")
public class Mother {
	@Id
	@GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) {  this.id = id; }
	private Integer id;

	@OneToMany(mappedBy = "mother")
	@Cascade({ CascadeType.PERSIST, CascadeType.MERGE })
	public Set<Son> getSons() { return sons; }
	public void setSons(Set<Son> sons) {  this.sons = sons; }
	private Set<Son> sons = new HashSet<>();
}
