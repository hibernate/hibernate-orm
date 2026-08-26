/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module.subject;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "EmbeddableSubjectEntity")
public class EmbeddableSubjectEntity {
	@Id
	public Long id;

	@Embedded
	public StubEmbeddable embeddable;
}
