/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter.GeneratorNameDeterminationContext;
import org.hibernate.id.IdentifierGenerator;

import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
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
			Class<?> idType,
			String generatorName,
			IdGeneratorStrategyInterpreter generationInterpreter,
			GenerationType generationType) {
		// If we were unable to locate an actual matching named generator assume
		// a sequence/table of the given name, make one based on GenerationType.

		if ( generationType == null) {
			return buildSequenceGeneratorDefinition( name, generationInterpreter );
		}

		final String strategyName;
		switch ( generationType ) {
			case SEQUENCE:
				return buildSequenceGeneratorDefinition( name, generationInterpreter );
			case TABLE:
				return buildTableGeneratorDefinition( name, generationInterpreter );
			// really AUTO and IDENTITY work the same in this respect, aside from the actual strategy name
			case IDENTITY:
				throw new AnnotationException(
						"@GeneratedValue annotation specified 'strategy=IDENTITY' and 'generator'"
								+ " but the generator name is unnecessary"
				);
			case AUTO:
				strategyName = generationInterpreter.determineGeneratorName(
						generationType,
						new GeneratorNameDeterminationContext() {
							@Override
							public Class<?> getIdType() {
								return idType;
							}
							@Override
							public String getGeneratedValueGeneratorName() {
								return generatorName;
							}
						}
				);
				break;
			default:
				//case UUID:
				// (use the name instead for compatibility with javax.persistence)
				if ( "UUID".equals( generationType.name() ) ) {
					throw new AnnotationException(
							"@GeneratedValue annotation specified 'strategy=UUID' and 'generator'"
									+ " but the generator name is unnecessary"
					);
				}
				else {
					throw new AssertionFailure( "unknown generator type: " + generationType );
				}
		}

		return new IdentifierGeneratorDefinition(
				name,
				strategyName,
				Collections.singletonMap( IdentifierGenerator.GENERATOR_NAME, name )
		);
	}

	private static IdentifierGeneratorDefinition buildTableGeneratorDefinition(
			String name, IdGeneratorStrategyInterpreter generationInterpreter) {
		final Builder builder = new Builder();
		generationInterpreter.interpretTableGenerator(
				new TableGenerator() {
					@Override
					public String name() {
						return name;
					}

					@Override
					public String table() {
						return "";
					}

					@Override
					public int initialValue() {
						return 0;
					}

					@Override
					public int allocationSize() {
						return 50;
					}

					@Override
					public String catalog() {
						return "";
					}

					@Override
					public String schema() {
						return "";
					}

					@Override
					public String pkColumnName() {
						return "";
					}

					@Override
					public String valueColumnName() {
						return "";
					}

					@Override
					public String pkColumnValue() {
						return "";
					}

					@Override
					public UniqueConstraint[] uniqueConstraints() {
						return new UniqueConstraint[0];
					}

					@Override
					public Index[] indexes() {
						return new Index[0];
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return TableGenerator.class;
					}
				},
				builder
		);

		return builder.build();
	}

	private static IdentifierGeneratorDefinition buildSequenceGeneratorDefinition(
			String name, IdGeneratorStrategyInterpreter generationInterpreter) {
		final Builder builder = new Builder();
		generationInterpreter.interpretSequenceGenerator(
				new SequenceGenerator() {
					@Override
					public String name() {
						return name;
					}

					@Override
					public String sequenceName() {
						return "";
					}

					@Override
					public String catalog() {
						return "";
					}

					@Override
					public String schema() {
						return "";
					}

					@Override
					public int initialValue() {
						return 1;
					}

					@Override
					public int allocationSize() {
						return 50;
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return SequenceGenerator.class;
					}
				},
				builder
		);

		return builder.build();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof IdentifierGeneratorDefinition ) ) {
			return false;
		}

		IdentifierGeneratorDefinition that = (IdentifierGeneratorDefinition) o;
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
