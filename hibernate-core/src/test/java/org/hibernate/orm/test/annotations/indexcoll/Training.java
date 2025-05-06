/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import java.util.SortedMap;
import java.util.TreeMap;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;

import org.hibernate.annotations.SortNatural;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Training {
	@Id @GeneratedValue private Long id;
	@SortNatural
	@MapKey(name="name") @ManyToMany SortedMap<String, Trainee> trainees = new TreeMap<String, Trainee>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SortedMap<String, Trainee> getTrainees() {
		return trainees;
	}

	public void setTrainees(SortedMap<String, Trainee> trainees) {
		this.trainees = trainees;
	}
}
