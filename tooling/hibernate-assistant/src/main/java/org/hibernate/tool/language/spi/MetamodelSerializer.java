/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.language.spi;

import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.Incubating;

/**
 * Contract used to provide the LLM with a textual representation of the
 * Hibernate metamodel, that is, the classes and mapping information
 * that constitute the persistence layer.
 */
@Incubating
public interface MetamodelSerializer {
	/**
	 * Utility method that generates a textual representation of the mapping information
	 * contained in the provided {@link Metamodel metamodel} instance. The representation
	 * does not need to follow a strict scheme, and is more akin to natural language,
	 * as it's mainly meant for consumption by a LLM.
	 *
	 * @param metamodel the metamodel instance containing information on the persistence structures
	 *
	 * @return the textual representation of the provided {@link Metamodel metamodel}
	 */
	String toString(Metamodel metamodel);
}
