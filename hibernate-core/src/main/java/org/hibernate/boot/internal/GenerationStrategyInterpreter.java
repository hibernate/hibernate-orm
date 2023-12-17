/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.UUID;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

/**
 * Interpretations related to value generators based on the {@linkplain GenerationType strategy/type}
 *
 * @author Steve Ebersole
 */
public class GenerationStrategyInterpreter {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( GenerationStrategyInterpreter.class );
	public static final GenerationStrategyInterpreter STRATEGY_INTERPRETER = new GenerationStrategyInterpreter();

	public interface GeneratorNameDeterminationContext {
		/**
		 * The Java type of the attribute defining the id whose value is to
		 * be generated.
		 */
		Class<?> getIdType();

		/**
		 * The {@link GeneratedValue#generator()} name.
		 */
		String getGeneratedValueGeneratorName();
	}

	private GenerationStrategyInterpreter() {
	}

	public String determineGeneratorName(GenerationType generationType, GeneratorNameDeterminationContext context) {
		switch ( generationType ) {
			case IDENTITY: {
				return "identity";
			}
			case SEQUENCE: {
				return SequenceStyleGenerator.class.getName();
			}
			case TABLE: {
				return org.hibernate.id.enhanced.TableGenerator.class.getName();
			}
			case UUID: {
				return UUIDGenerator.class.getName();
			}
			case AUTO: {
				if ( UUID.class.isAssignableFrom( context.getIdType() ) ) {
					return UUIDGenerator.class.getName();
				}
				else if ( "increment".equalsIgnoreCase( context.getGeneratedValueGeneratorName() ) ) {
					// special case for @GeneratedValue(name="increment")
					// for some reason we consider there to be an implicit
					// generator named 'increment' (doesn't seem very useful)
					return IncrementGenerator.class.getName();
				}
				else {
					return SequenceStyleGenerator.class.getName();
				}
			}
			default: {
				throw new UnsupportedOperationException( "Unsupported generation type:" + generationType );
			}
		}
	}

	public void interpretTableGenerator(
			TableGenerator tableGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		definitionBuilder.setName( tableGeneratorAnnotation.name() );
		definitionBuilder.setStrategy( org.hibernate.id.enhanced.TableGenerator.class.getName() );
		definitionBuilder.addParam( org.hibernate.id.enhanced.TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );

		if ( !tableGeneratorAnnotation.catalog().isEmpty()) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.CATALOG, tableGeneratorAnnotation.catalog() );
		}
		if ( !tableGeneratorAnnotation.schema().isEmpty()) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.SCHEMA, tableGeneratorAnnotation.schema() );
		}
		if ( !tableGeneratorAnnotation.table().isEmpty()) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.TABLE_PARAM,
					tableGeneratorAnnotation.table()
			);
		}
		if ( !tableGeneratorAnnotation.pkColumnName().isEmpty()) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.SEGMENT_COLUMN_PARAM,
					tableGeneratorAnnotation.pkColumnName()
			);
		}
		if ( !tableGeneratorAnnotation.pkColumnValue().isEmpty()) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.SEGMENT_VALUE_PARAM,
					tableGeneratorAnnotation.pkColumnValue()
			);
		}
		if ( !tableGeneratorAnnotation.valueColumnName().isEmpty()) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.VALUE_COLUMN_PARAM,
					tableGeneratorAnnotation.valueColumnName()
			);
		}
		definitionBuilder.addParam(
				org.hibernate.id.enhanced.TableGenerator.INCREMENT_PARAM,
				String.valueOf( tableGeneratorAnnotation.allocationSize() )
		);
		// See comment on HHH-4884 wrt initialValue.  Basically initialValue is really the stated value + 1
		definitionBuilder.addParam(
				org.hibernate.id.enhanced.TableGenerator.INITIAL_PARAM,
				String.valueOf( tableGeneratorAnnotation.initialValue() + 1 )
		);

		// TODO : implement unique-constraint support
		if ( tableGeneratorAnnotation.uniqueConstraints() != null
				&& tableGeneratorAnnotation.uniqueConstraints().length > 0 ) {
			LOG.ignoringTableGeneratorConstraints( tableGeneratorAnnotation.name() );
		}
	}

	public void interpretSequenceGenerator(
			SequenceGenerator sequenceGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		definitionBuilder.setName( sequenceGeneratorAnnotation.name() );
		definitionBuilder.setStrategy( SequenceStyleGenerator.class.getName() );

		if ( !sequenceGeneratorAnnotation.catalog().isEmpty()) {
			definitionBuilder.addParam(
					PersistentIdentifierGenerator.CATALOG,
					sequenceGeneratorAnnotation.catalog()
			);
		}
		if ( !sequenceGeneratorAnnotation.schema().isEmpty()) {
			definitionBuilder.addParam(
					PersistentIdentifierGenerator.SCHEMA,
					sequenceGeneratorAnnotation.schema()
			);
		}
		if ( !sequenceGeneratorAnnotation.sequenceName().isEmpty()) {
			definitionBuilder.addParam(
					SequenceStyleGenerator.SEQUENCE_PARAM,
					sequenceGeneratorAnnotation.sequenceName()
			);
		}

		definitionBuilder.addParam(
				SequenceStyleGenerator.INCREMENT_PARAM,
				String.valueOf( sequenceGeneratorAnnotation.allocationSize() )
		);
		definitionBuilder.addParam(
				SequenceStyleGenerator.INITIAL_PARAM,
				String.valueOf( sequenceGeneratorAnnotation.initialValue() )
		);
	}
}
