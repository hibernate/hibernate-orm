/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.Serializable;

import org.hibernate.action.spi.Executable;

/**
 * We frequently need the union type of Executable, Comparable of ComparableExecutable, Serializable;
 * this interface represents such union; this helps to simplify several generic signatures.
 * Secondarily, it helps to avoid triggering type pollution by not needing to typecheck
 * for a very specific Comparable type; we represent the common needs to resolve sorting
 * by exposing primary and secondary sorting attributes.
 */
public interface ComparableExecutable extends Executable, Comparable<ComparableExecutable>, Serializable {

	/**
	 * This affect sorting order of the executables, when sorting is enabled.
	 * @return the primary sorting attribute; typically the entity name or collection role.
	 */
	String getPrimarySortClassifier();

	/**
	 * This affect sorting order of the executables, when sorting is enabled.
	 * @return the secondary sorting attribute, applied when getPrimarySortClassifier
	 * matches during a comparison; typically the entity key or collection key.
	 */
	Object getSecondarySortIndex();

}
