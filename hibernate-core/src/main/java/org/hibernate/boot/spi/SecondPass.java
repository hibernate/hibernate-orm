/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.mapping.PersistentClass;

/**
 * Hibernate builds its {@linkplain org.hibernate.mapping build-time model}
 * incrementally, often delaying operations until other pieces of information
 * are available. A second pass represents one of these delayed operations.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface SecondPass extends Serializable {
	/**
	 * Perform the operation
	 */
	void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException;
}
