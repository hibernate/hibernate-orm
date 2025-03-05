/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Child implements Serializable {
	@Id
	@GeneratedValue
	public Integer id;

	@ManyToOne()
	@JoinColumn(name = "parentCivility", referencedColumnName = "isMale")
	@JoinColumn(name = "parentLastName", referencedColumnName = "lastName")
	@JoinColumn(name = "parentFirstName", referencedColumnName = "firstName")
	public Parent parent;
	@Column(name = "fav_sup_hero")
	public String favoriteSuperhero;
	@Column(name = "fav_singer")
	public String favoriteSinger;
}
