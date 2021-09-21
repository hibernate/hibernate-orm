/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen;

/**
 * {@code RuntimeException} used for errors during meta model generation.
 *
 * @author Hardy Ferentschik
 */
public class MetaModelGenerationException extends RuntimeException {
	public MetaModelGenerationException() {
		super();
	}

	public MetaModelGenerationException(String message) {
		super( message );
	}
}
