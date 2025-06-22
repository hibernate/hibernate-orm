/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.model;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public interface ImportContext {

	/**
	 * Add fqcn to the import list. Returns fqcn as needed in source code.
	 * Attempts to handle fqcn with array and generics references.
	 * <p>
	 * e.g.
	 * {@code java.util.Collection<org.marvel.Hulk>} imports {@code java.util.Collection} and returns {@code Collection}
	 * {@code org.marvel.Hulk[]} imports {@code org.marvel.Hulk} and returns {@code Hulk}
	 *
	 * @param fqcn Fully qualified class name of the type to import.
	 *
	 * @return import string
	 */
	String importType(String fqcn);

	String staticImport(String fqcn, String member);

	String generateImports();
}
