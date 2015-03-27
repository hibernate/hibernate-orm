/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
