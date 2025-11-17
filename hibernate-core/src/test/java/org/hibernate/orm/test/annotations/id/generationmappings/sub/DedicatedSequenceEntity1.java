/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.generationmappings.sub;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Lukasz Antoniak
 */
@Entity
@Table(name = "DEDICATED_SEQ_TBL1")
public class DedicatedSequenceEntity1 implements Serializable {
	public static final String SEQUENCE_SUFFIX = "_GEN";

	private Long id;

	@Id
	@GeneratedValue(generator = "SequencePerEntityGenerator")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
