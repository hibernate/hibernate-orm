/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.id;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * @author Steve Ebersole
 */
@Entity
@IdClass(EmbeddedIdClassEntity.Pk.class)
public class EmbeddedIdClassEntity {
	@Id
	@Embedded
	private Code code;

	@Id
	@Column(name = "local_id")
	private Integer localId;

	@Embeddable
	public static class Code implements Serializable {
		@Column(name = "code_part")
		private String part;
	}

	public static class Pk implements Serializable {
		private Code code;
		private Integer localId;
	}
}
