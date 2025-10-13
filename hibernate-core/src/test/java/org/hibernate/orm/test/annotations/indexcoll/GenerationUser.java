/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class GenerationUser {

	@Id
	@GeneratedValue
	private int id;

	@OneToMany
	@MapKey(name="generation")
	private Map<Generation, GenerationGroup> ref = new HashMap<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Map<Generation, GenerationGroup> getRef() {
		return ref;
	}

	public void setRef(Map<Generation, GenerationGroup> ref) {
		this.ref = ref;
	}


}
