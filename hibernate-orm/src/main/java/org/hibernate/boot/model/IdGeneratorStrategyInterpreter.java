/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

/**
 * Strategy for interpreting identifier generator related information.
 *
 * @author Steve Ebersole
 */
public interface IdGeneratorStrategyInterpreter {
	public static interface GeneratorNameDeterminationContext {
		public Class getIdType();
	}

	/**
	 * Determine the name of the generator which should be used based on the
	 * GenerationType, returning {@code null} to indicate that this interpreter
	 * did not have a match and that any additional resolutions should be performed.
	 *
	 * @param generationType The {@link javax.persistence.GeneratedValue#strategy} value
	 * @param context The context for resolution (method parameter object)
	 */
	String determineGeneratorName(GenerationType generationType, GeneratorNameDeterminationContext context);

	/**
	 * Extract the IdentifierGeneratorDefinition related to the given TableGenerator annotation
	 *
	 * @param tableGeneratorAnnotation The annotation
	 * @param definitionBuilder The IdentifierGeneratorDefinition builder to which to apply
	 * any interpreted/extracted configuration
	 */
	void interpretTableGenerator(TableGenerator tableGeneratorAnnotation, IdentifierGeneratorDefinition.Builder definitionBuilder);

	/**
	 * Extract the IdentifierGeneratorDefinition related to the given SequenceGenerator annotation
	 *
	 * @param sequenceGeneratorAnnotation The annotation
	 * @param definitionBuilder The IdentifierGeneratorDefinition builder to which to apply
	 * any interpreted/extracted configuration
	 */
	void interpretSequenceGenerator(SequenceGenerator sequenceGeneratorAnnotation, IdentifierGeneratorDefinition.Builder definitionBuilder);
}
