/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

import jakarta.persistence.SequenceGenerator;

/**
 * Commonality for annotations which represent database objects.
 *
 * @apiNote While they all have names, some use an attribute other than {@code name()} - e.g. {@linkplain SequenceGenerator#sequenceName()}
 *
 * @author Steve Ebersole
 */
public interface DatabaseObjectDetails extends Annotation {
	/**
	 * The catalog in which the object exists
	 */
	String catalog();

	/**
	 * Setter for {@linkplain #catalog()}
	 */
	void catalog(String catalog);

	/**
	 * The schema in which the object exists
	 */
	String schema();

	/**
	 * Setter for {@linkplain #schema()}
	 */
	void schema(String schema);
}
