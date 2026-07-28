/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import jakarta.persistence.GenerationType;
import org.hibernate.AnnotationException;
import org.hibernate.Internal;
import org.hibernate.Remove;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.generator.Generator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretSequenceGenerator;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretTableGenerator;
import static org.hibernate.boot.models.JpaAnnotations.SEQUENCE_GENERATOR;
import static org.hibernate.boot.models.JpaAnnotations.TABLE_GENERATOR;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Models a named identifier-generator definition.
 *
 * @implSpec Should be immutable.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 * @author Strong Liu
 */
@Remove
public class IdentifierGeneratorDefinition implements Serializable {
	private final String name;
	private final Class<? extends Generator> generatorClass;
	private final Map<String, String> parameters;

	public IdentifierGeneratorDefinition(
			final String name,
			final Class<? extends Generator> generatorClass,
			final Map<String, String> parameters) {
		this.name = name;
		this.generatorClass = Objects.requireNonNull( generatorClass, "generatorClass" );
		this.parameters = isEmpty( parameters )
				? emptyMap()
				: unmodifiableMap( parameters );
	}

	public IdentifierGeneratorDefinition(String name, Class<? extends Generator> generatorClass) {
		this.name = name;
		this.generatorClass = Objects.requireNonNull( generatorClass, "generatorClass" );
		this.parameters = emptyMap();
	}

	/**
	 * @return the identifier generator implementation class
	 */
	public Class<? extends Generator> getGeneratorClass() {
		return generatorClass;
	}

	/**
	 * @return generator name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return generator configuration parameters
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Internal
	public static IdentifierGeneratorDefinition createImplicit(
			String name,
			String generatorName,
			GenerationType generationType) {
		// If we were unable to locate an actual matching named generator assume
		// a sequence/table of the given name, make one based on GenerationType.

		return switch ( generationType == null ? GenerationType.SEQUENCE : generationType ) {
			case SEQUENCE -> buildSequenceGeneratorDefinition( name );
			case TABLE -> buildTableGeneratorDefinition( name );
			case AUTO -> new IdentifierGeneratorDefinition(
					name,
					"increment".equalsIgnoreCase( generatorName )
							? IncrementGenerator.class
							: SequenceStyleGenerator.class,
					singletonMap( IdentifierGenerator.GENERATOR_NAME, name )
			);
			case IDENTITY, UUID -> throw new AnnotationException(
					"@GeneratedValue annotation specified 'strategy=" + generationType
					+ "' and 'generator' but the generator name is unnecessary"
			);
		};
	}

	private static IdentifierGeneratorDefinition buildTableGeneratorDefinition(String name) {
		final TableGeneratorJpaAnnotation tableGeneratorUsage = TABLE_GENERATOR.createUsage( null );
		if ( isNotEmpty( name ) ) {
			tableGeneratorUsage.name( name );
		}
		final Builder builder = new Builder();
		interpretTableGenerator( tableGeneratorUsage, builder );
		return builder.build();
	}

	private static IdentifierGeneratorDefinition buildSequenceGeneratorDefinition(String name) {
		final SequenceGeneratorJpaAnnotation sequenceGeneratorUsage = SEQUENCE_GENERATOR.createUsage( null );
		if ( isNotEmpty( name ) ) {
			sequenceGeneratorUsage.name( name );
		}
		final Builder builder = new Builder();
		interpretSequenceGenerator( sequenceGeneratorUsage, builder );
		return builder.build();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof IdentifierGeneratorDefinition that) ) {
			return false;
		}

		return Objects.equals(name, that.name)
			&& Objects.equals(generatorClass, that.generatorClass)
			&& Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, generatorClass, parameters );
	}

	@Override
	public String toString() {
		return "IdentifierGeneratorDefinition{" +
				"name='" + name + '\'' +
				", generatorClass=" + generatorClass.getName() +
				", parameters=" + parameters +
				'}';
	}

	public static class Builder {
		private String name;
		private Class<? extends Generator> generatorClass;
		private Map<String, String> parameters;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Class<? extends Generator> getGeneratorClass() {
			return generatorClass;
		}

		public void setGeneratorClass(Class<? extends Generator> generatorClass) {
			this.generatorClass = generatorClass;
		}

		public void addParam(String name, String value) {
			parameters().put( name, value );
		}

		private Map<String, String> parameters() {
			if ( parameters == null ) {
				parameters = new HashMap<>();
			}
			return parameters;
		}

		public void addParams(Map<String,String> parameters) {
			parameters().putAll( parameters );
		}

		public IdentifierGeneratorDefinition build() {
			return new IdentifierGeneratorDefinition( name, generatorClass, parameters );
		}
	}
}
