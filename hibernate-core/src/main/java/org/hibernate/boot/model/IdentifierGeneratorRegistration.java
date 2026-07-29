/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import jakarta.persistence.GenerationType;
import org.hibernate.AnnotationException;
import org.hibernate.Internal;
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
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretSequenceGenerator;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretTableGenerator;
import static org.hibernate.boot.models.JpaAnnotations.SEQUENCE_GENERATOR;
import static org.hibernate.boot.models.JpaAnnotations.TABLE_GENERATOR;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Models an identifier-generator registration in the boot model.
 * <p>
 * A registration captures the generator implementation and the configuration
 * declared by {@code @SequenceGenerator}, {@code @TableGenerator},
 * {@code @GenericGenerator}, or the corresponding XML.  It is resolved while
 * boot metadata is being built and is later used to prepare the generator for
 * a particular identifier mapping.  It is not itself a generator instance and
 * does not model the runtime lifecycle of the generator.
 * <p>
 * Identifier-generator registrations are persistence-unit scoped and are
 * exposed through {@link org.hibernate.boot.Metadata}.
 * For an unnamed generator declaration, the owning entity's JPA entity name
 * is used as the implicit registration name.
 *
 * @implSpec Should be immutable.
 *
 * @since 9.0
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 * @author Strong Liu
 */
public final class IdentifierGeneratorRegistration implements Serializable {
	/**
	 * Identifies the resolved generator family.  The kind describes what the
	 * registration resolves to, independently of whether its name was declared
	 * explicitly or inferred.
	 */
	public enum Kind {
		SEQUENCE,
		TABLE,
		UUID,
		CUSTOM
	}

	private final String name;
	private final Kind kind;
	private final Class<? extends Generator> generatorClass;
	private final Map<String, String> parameters;

	public IdentifierGeneratorRegistration(
			final String name,
			final Kind kind,
			final Class<? extends Generator> generatorClass,
			final Map<String, String> parameters) {
		this.name = name;
		this.kind = Objects.requireNonNull( kind, "kind" );
		this.generatorClass = Objects.requireNonNull( generatorClass, "generatorClass" );
		this.parameters = isEmpty( parameters ) ? emptyMap() : Map.copyOf( parameters );
	}

	public IdentifierGeneratorRegistration(
			String name,
			Class<? extends Generator> generatorClass,
			Map<String, String> parameters) {
		this( name, Kind.CUSTOM, generatorClass, parameters );
	}

	public IdentifierGeneratorRegistration(String name, Class<? extends Generator> generatorClass) {
		this( name, Kind.CUSTOM, generatorClass, emptyMap() );
	}

	/**
	 * @return the generator implementation class, resolved during categorization
	 */
	public Class<? extends Generator> getGeneratorClass() {
		return generatorClass;
	}

	/**
	 * @return the resolved generator family
	 */
	public Kind getKind() {
		return kind;
	}

	/**
	 * @return the explicit generator name, or the implicit JPA entity name
	 * used to resolve an unnamed declaration
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return an immutable map of the declared generator configuration
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Internal
	public static IdentifierGeneratorRegistration createImplicit(
			String name,
			String generatorName,
			GenerationType generationType) {
		// If we were unable to locate an actual matching named generator assume
		// a sequence/table of the given name, make one based on GenerationType.

		return switch ( generationType == null ? GenerationType.SEQUENCE : generationType ) {
			case SEQUENCE -> buildSequenceGeneratorDefinition( name );
			case TABLE -> buildTableGeneratorDefinition( name );
			case AUTO -> new IdentifierGeneratorRegistration(
					name,
					"increment".equalsIgnoreCase( generatorName )
							? Kind.CUSTOM
							: Kind.SEQUENCE,
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

	private static IdentifierGeneratorRegistration buildTableGeneratorDefinition(String name) {
		final TableGeneratorJpaAnnotation tableGeneratorUsage = TABLE_GENERATOR.createUsage( null );
		if ( isNotEmpty( name ) ) {
			tableGeneratorUsage.name( name );
		}
		final Builder builder = new Builder();
		builder.setKind( Kind.TABLE );
		interpretTableGenerator( tableGeneratorUsage, builder );
		return builder.build();
	}

	private static IdentifierGeneratorRegistration buildSequenceGeneratorDefinition(String name) {
		final SequenceGeneratorJpaAnnotation sequenceGeneratorUsage = SEQUENCE_GENERATOR.createUsage( null );
		if ( isNotEmpty( name ) ) {
			sequenceGeneratorUsage.name( name );
		}
		final Builder builder = new Builder();
		builder.setKind( Kind.SEQUENCE );
		interpretSequenceGenerator( sequenceGeneratorUsage, builder );
		return builder.build();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof IdentifierGeneratorRegistration that) ) {
			return false;
		}

		return Objects.equals(name, that.name)
			&& kind == that.kind
			&& Objects.equals(generatorClass, that.generatorClass)
			&& Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, kind, generatorClass, parameters );
	}

	@Override
	public String toString() {
		return "IdentifierGeneratorRegistration{" +
				"name='" + name + '\'' +
				", kind=" + kind +
				", generatorClass=" + generatorClass.getName() +
				", parameters=" + parameters +
				'}';
	}

	public static class Builder {
		private String name;
		private Kind kind = Kind.CUSTOM;
		private Class<? extends Generator> generatorClass;
		private Map<String, String> parameters;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setKind(Kind kind) {
			this.kind = kind;
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

		public IdentifierGeneratorRegistration build() {
			return new IdentifierGeneratorRegistration( name, kind, generatorClass, parameters );
		}
	}
}
