/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.List;
import java.util.UUID;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationUsage;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;

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
			AnnotationUsage<TableGenerator> tableGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		definitionBuilder.setName( tableGeneratorAnnotation.getString( "name" ) );
		definitionBuilder.setStrategy( org.hibernate.id.enhanced.TableGenerator.class.getName() );
		definitionBuilder.addParam( org.hibernate.id.enhanced.TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );

		final String catalog = tableGeneratorAnnotation.getString( "catalog" );
		if ( StringHelper.isNotEmpty( catalog ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.CATALOG, catalog );
		}

		final String schema = tableGeneratorAnnotation.getString( "schema" );
		if ( StringHelper.isNotEmpty( schema ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.SCHEMA, schema );
		}

		final String table = tableGeneratorAnnotation.getString( "table" );
		if ( StringHelper.isNotEmpty( table ) ) {
			definitionBuilder.addParam( org.hibernate.id.enhanced.TableGenerator.TABLE_PARAM, table );
		}

		final String pkColumnName = tableGeneratorAnnotation.getString( "pkColumnName" );
		if ( StringHelper.isNotEmpty( pkColumnName ) ) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.SEGMENT_COLUMN_PARAM,
					pkColumnName
			);
		}

		final String pkColumnValue = tableGeneratorAnnotation.getString( "pkColumnValue" );
		if ( StringHelper.isNotEmpty( pkColumnValue ) ) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.SEGMENT_VALUE_PARAM,
					pkColumnValue
			);
		}

		final String valueColumnName = tableGeneratorAnnotation.getString( "valueColumnName" );
		if ( StringHelper.isNotEmpty( valueColumnName ) ) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.VALUE_COLUMN_PARAM,
					valueColumnName
			);
		}

		final String options = tableGeneratorAnnotation.getString( "options" );
		if ( StringHelper.isNotEmpty( options ) ) {
			definitionBuilder.addParam(
					PersistentIdentifierGenerator.OPTIONS,
					options
			);
		}

		definitionBuilder.addParam(
				org.hibernate.id.enhanced.TableGenerator.INCREMENT_PARAM,
				String.valueOf( tableGeneratorAnnotation.getInteger( "allocationSize" ) )
		);

		// See comment on HHH-4884 wrt initialValue.  Basically initialValue is really the stated value + 1
		definitionBuilder.addParam(
				org.hibernate.id.enhanced.TableGenerator.INITIAL_PARAM,
				String.valueOf( tableGeneratorAnnotation.getInteger( "initialValue" ) + 1 )
		);

		// TODO : implement unique-constraint support
		final List<AnnotationUsage<UniqueConstraint>> uniqueConstraints = tableGeneratorAnnotation.getList( "uniqueConstraints" );
		if ( CollectionHelper.isNotEmpty( uniqueConstraints ) ) {
			LOG.ignoringTableGeneratorConstraints( tableGeneratorAnnotation.getString( "name" ) );
		}
	}

	public void interpretSequenceGenerator(
			AnnotationUsage<SequenceGenerator> sequenceGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		definitionBuilder.setName( sequenceGeneratorAnnotation.getString( "name" ) );
		definitionBuilder.setStrategy( SequenceStyleGenerator.class.getName() );

		final String catalog = sequenceGeneratorAnnotation.getString( "catalog" );
		if ( StringHelper.isNotEmpty( catalog ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.CATALOG, catalog );
		}

		final String schema = sequenceGeneratorAnnotation.getString( "schema" );
		if ( StringHelper.isNotEmpty( schema ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.SCHEMA, schema );
		}

		final String sequenceName = sequenceGeneratorAnnotation.getString( "sequenceName" );
		if ( StringHelper.isNotEmpty( sequenceName ) ) {
			definitionBuilder.addParam( SequenceStyleGenerator.SEQUENCE_PARAM, sequenceName );
		}

		definitionBuilder.addParam(
				SequenceStyleGenerator.INCREMENT_PARAM,
				String.valueOf( sequenceGeneratorAnnotation.getInteger( "allocationSize" ) )
		);
		definitionBuilder.addParam(
				SequenceStyleGenerator.INITIAL_PARAM,
				String.valueOf( sequenceGeneratorAnnotation.getInteger( "initialValue" ) )
		);

		final String options = sequenceGeneratorAnnotation.getString( "options" );
		if ( StringHelper.isNotEmpty( options ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.OPTIONS, options );
		}
	}
}
