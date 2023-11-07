/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import java.util.List;

import org.hibernate.metamodel.mapping.ModelPart;

/**
 * A container for {@linkplain org.hibernate.generator.OnExecutionGenerator database generated values}
 * retrieved by the mutation operation. The values are stored relative to the {@linkplain ModelPart property}
 * they represent.
 *
 * @author Marco Belladelli
 * @see org.hibernate.generator.OnExecutionGenerator
 */
public interface GeneratedValues {
	/**
	 * Register a generated value for the corresponding {@link ModelPart}
	 */
	void addGeneratedValue(ModelPart modelPart, Object value);

	/**
	 * Retrieve a generated value for the requested {@link ModelPart}.
	 */
	Object getGeneratedValue(ModelPart modelPart);

	/**
	 * Retrieves a list of generated values corresponding to the list of requested {@link ModelPart}s.
	 * Ensures the order of the values in the returned list corresponds to the input properties.
	 */
	List<Object> getGeneratedValues(List<? extends ModelPart> modelParts);
}
