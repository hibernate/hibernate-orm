/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;


/**
 * Contract for creating the ClassDetails for a Java type we have not yet seen
 * as part of {@link ClassDetailsRegistry#resolveClassDetails}
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ClassDetailsBuilder {
	/**
	 * Build a ClassDetails descriptor for a class with the given name
	 */
	ClassDetails buildClassDetails(String name, ModelProcessingContext processingContext);
}
