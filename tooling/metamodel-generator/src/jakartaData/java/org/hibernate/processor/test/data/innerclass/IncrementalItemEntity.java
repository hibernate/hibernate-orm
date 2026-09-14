/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.innerclass;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.processing.Find;

import java.util.List;

@Entity
public class IncrementalItemEntity {
	@Id
	@GeneratedValue
	public Long id;

	public String name;

	public interface Queries {
		@Find
		List<IncrementalItemEntity> findByName(String name);
	}
}
