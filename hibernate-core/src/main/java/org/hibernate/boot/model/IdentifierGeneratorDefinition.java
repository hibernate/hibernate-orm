/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import jakarta.persistence.GenerationType;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.TypeDetails;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretSequenceGenerator;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretTableGenerator;
import static org.hibernate.boot.model.internal.GeneratorStrategies.generatorStrategy;
import static org.hibernate.boot.models.JpaAnnotations.SEQUENCE_GENERATOR;
import static org.hibernate.boot.models.JpaAnnotations.TABLE_GENERATOR;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Models the definition of an {@linkplain org.hibernate.id.IdentityGenerator identifier generator}
 *
 * @implSpec Should be immutable.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 * @author Strong Liu
 */
public class IdentifierGeneratorDefinition implements Serializable {
	private final String name;
	private final String strategy;
	private final Map<String, String> parameters;

	public IdentifierGeneratorDefinition(
			final String name,
			final String strategy,
			final Map<String, String> parameters) {
		this.name = name;
		this.strategy = strategy;
		this.parameters = isEmpty( parameters ) ? emptyMap() : unmodifiableMap( parameters );
	}

	public IdentifierGeneratorDefinition(
			final String name,
			final Map<String, String> parameters) {
		this( name, name, parameters );
	}

	public IdentifierGeneratorDefinition(String name) {
		this( name, name );
	}

	public IdentifierGeneratorDefinition(String name, String strategy) {
		this.name = name;
		this.strategy = strategy;
		this.parameters = emptyMap();
	}

	/**
	 * @return identifier generator strategy
	 */
	public String getStrategy() {
		return strategy;
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
			TypeDetails idType,
			String generatorName,
			GenerationType generationType) {
		// If we were unable to locate an actual matching named generator assume
		// a sequence/table of the given name, make one based on GenerationType.

		if ( generationType == null ) {
			generationType = GenerationType.SEQUENCE;
		}

		switch ( generationType ) {
			case SEQUENCE:
				return buildSequenceGeneratorDefinition( name );
			case TABLE:
				return buildTableGeneratorDefinition( name );
			case IDENTITY:
				throw new AnnotationException(
						"@GeneratedValue annotation specified 'strategy=IDENTITY' and 'generator'"
								+ " but the generator name is unnecessary"
				);
			case UUID:
				throw new AnnotationException(
						"@GeneratedValue annotation specified 'strategy=UUID' and 'generator'"
								+ " but the generator name is unnecessary"
				);
			case AUTO:
				return new IdentifierGeneratorDefinition(
						name,
						generatorStrategy( generationType, generatorName, idType ),
						singletonMap( IdentifierGenerator.GENERATOR_NAME, name )
				);
			default:
				throw new AssertionFailure( "unknown generator type: " + generationType );
		}
	}

	private static IdentifierGeneratorDefinition buildTableGeneratorDefinition(String name) {
		final Builder builder = new Builder();
		final TableGeneratorJpaAnnotation tableGeneratorUsage = TABLE_GENERATOR.createUsage( null );
		if ( StringHelper.isNotEmpty( name ) ) {
			tableGeneratorUsage.name( name );
		}
		interpretTableGenerator( tableGeneratorUsage, builder );
		return builder.build();
	}

	private static IdentifierGeneratorDefinition buildSequenceGeneratorDefinition(String name) {
		final Builder builder = new Builder();
		final SequenceGeneratorJpaAnnotation sequenceGeneratorUsage = SEQUENCE_GENERATOR.createUsage( null );
		if ( StringHelper.isNotEmpty( name ) ) {
			sequenceGeneratorUsage.name( name );
		}
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
			&& Objects.equals(strategy, that.strategy)
			&& Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( strategy != null ? strategy.hashCode() : 0 );
		result = 31 * result + ( parameters != null ? parameters.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "IdentifierGeneratorDefinition{" +
				"name='" + name + '\'' +
				", strategy='" + strategy + '\'' +
				", parameters=" + parameters +
				'}';
	}

	public static class Builder {
		private String name;
		private String strategy;
		private Map<String, String> parameters;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getStrategy() {
			return strategy;
		}

		public void setStrategy(String strategy) {
			this.strategy = strategy;
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
			return new IdentifierGeneratorDefinition( name, strategy, parameters );
		}
	}
}
