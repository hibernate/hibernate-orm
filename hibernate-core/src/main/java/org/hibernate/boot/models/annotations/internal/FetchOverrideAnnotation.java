/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import jakarta.persistence.FetchType;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
public class FetchOverrideAnnotation implements FetchProfile.FetchOverride {
	private Class<?> entity;
	private String association;
	private FetchMode mode;
	private FetchType fetch;

	public FetchOverrideAnnotation() {
		this.mode = FetchMode.JOIN;
		this.fetch = FetchType.EAGER;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FetchProfile.FetchOverride.class;
	}

	@Override
	public Class<?> entity() {
		return entity;
	}

	public void entity(Class<?> value) {
		this.entity = value;
	}

	@Override
	public String association() {
		return association;
	}

	public void association(String value) {
		this.association = value;
	}

	@Override
	public FetchMode mode() {
		return mode;
	}

	public void mode(FetchMode value) {
		this.mode = value;
	}

	@Override
	public FetchType fetch() {
		return fetch;
	}

	public void fetch(FetchType value) {
		this.fetch = value;
	}
}
