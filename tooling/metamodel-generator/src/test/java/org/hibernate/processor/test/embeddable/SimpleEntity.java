/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Chris Cranford
 */
@Entity
public class SimpleEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@NotNullAllowed
	@Embedded
	private SimpleEmbeddable simpleEmbeddable;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public SimpleEmbeddable getSimpleEmbeddable() {
		return simpleEmbeddable;
	}

	public void setSimpleEmbeddable(SimpleEmbeddable simpleEmbeddable) {
		this.simpleEmbeddable = simpleEmbeddable;
	}

	// represents a mock TYPE_USE based annotation
	@Target({ ElementType.TYPE_USE })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NotNullAllowed {
	}
}
