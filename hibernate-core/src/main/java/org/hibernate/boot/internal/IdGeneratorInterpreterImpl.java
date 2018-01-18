/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.UUID;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * The root (composition) IdGenerationTypeInterpreter.
 *
 * @author Steve Ebersole
 */
public class IdGeneratorInterpreterImpl implements IdGeneratorStrategyInterpreter {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( IdGeneratorInterpreterImpl.class );

	private IdGeneratorStrategyInterpreter fallbackInterpreter = FallbackInterpreter.INSTANCE;
	private ArrayList<IdGeneratorStrategyInterpreter> delegates;

	@Override
	public String determineGeneratorName(GenerationType generationType, GeneratorNameDeterminationContext context) {
		if ( delegates != null ) {
			for ( IdGeneratorStrategyInterpreter delegate : delegates ) {
				final String result = delegate.determineGeneratorName( generationType, context );
				if ( result != null ) {
					return result;
				}
			}
		}
		return fallbackInterpreter.determineGeneratorName( generationType, context );
	}

	@Override
	public void interpretTableGenerator(
			TableGenerator tableGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		fallbackInterpreter.interpretTableGenerator( tableGeneratorAnnotation, definitionBuilder );

		if ( delegates != null ) {
			for ( IdGeneratorStrategyInterpreter delegate : delegates ) {
				delegate.interpretTableGenerator( tableGeneratorAnnotation, definitionBuilder );
			}
		}
	}

	@Override
	public void interpretSequenceGenerator(
			SequenceGenerator sequenceGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		fallbackInterpreter.interpretSequenceGenerator( sequenceGeneratorAnnotation, definitionBuilder );

		if ( delegates != null ) {
			for ( IdGeneratorStrategyInterpreter delegate : delegates ) {
				delegate.interpretSequenceGenerator( sequenceGeneratorAnnotation, definitionBuilder );
			}
		}
	}

	public void enableLegacyFallback() {
		fallbackInterpreter = LegacyFallbackInterpreter.INSTANCE;
	}

	public void disableLegacyFallback() {
		fallbackInterpreter = FallbackInterpreter.INSTANCE;
	}

	public void addInterpreterDelegate(IdGeneratorStrategyInterpreter delegate) {
		if ( delegates == null ) {
			delegates = new ArrayList<IdGeneratorStrategyInterpreter>();
		}
		delegates.add( delegate );
	}

	private static class LegacyFallbackInterpreter implements IdGeneratorStrategyInterpreter {
		/**
		 * Singleton access
		 */
		public static final LegacyFallbackInterpreter INSTANCE = new LegacyFallbackInterpreter();

		@Override
		public String determineGeneratorName(GenerationType generationType, GeneratorNameDeterminationContext context) {
			switch ( generationType ) {
				case IDENTITY: {
					return "identity";
				}
				case SEQUENCE: {
					return "seqhilo";
				}
				case TABLE: {
					return MultipleHiLoPerTableGenerator.class.getName();
				}
				default: {
					// AUTO

					if ( "increment".equalsIgnoreCase( context.getGeneratedValueGeneratorName() ) ) {
						return IncrementGenerator.class.getName();
					}

					final Class javaType = context.getIdType();
					if ( UUID.class.isAssignableFrom( javaType ) ) {
						return UUIDGenerator.class.getName();
					}

					return "native";
				}
			}
		}

		@Override
		public void interpretTableGenerator(
				TableGenerator tableGeneratorAnnotation,
				IdentifierGeneratorDefinition.Builder definitionBuilder) {
			definitionBuilder.setName( tableGeneratorAnnotation.name() );
			definitionBuilder.setStrategy( MultipleHiLoPerTableGenerator.class.getName() );

			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.table() ) ) {
				definitionBuilder.addParam(
						MultipleHiLoPerTableGenerator.ID_TABLE,
						tableGeneratorAnnotation.table()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.catalog() ) ) {
				definitionBuilder.addParam(
						PersistentIdentifierGenerator.CATALOG,
						tableGeneratorAnnotation.catalog()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.schema() ) ) {
				definitionBuilder.addParam(
						PersistentIdentifierGenerator.SCHEMA,
						tableGeneratorAnnotation.schema()
				);
			}

			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.pkColumnName() ) ) {
				definitionBuilder.addParam(
						MultipleHiLoPerTableGenerator.PK_COLUMN_NAME,
						tableGeneratorAnnotation.pkColumnName()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.valueColumnName() ) ) {
				definitionBuilder.addParam(
						MultipleHiLoPerTableGenerator.VALUE_COLUMN_NAME,
						tableGeneratorAnnotation.valueColumnName()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.pkColumnValue() ) ) {
				definitionBuilder.addParam(
						MultipleHiLoPerTableGenerator.PK_VALUE_NAME,
						tableGeneratorAnnotation.pkColumnValue()
				);
			}
			definitionBuilder.addParam(
					MultipleHiLoPerTableGenerator.MAX_LO,
					String.valueOf( tableGeneratorAnnotation.allocationSize() - 1 )
			);

			// TODO : implement unique-constraint support
			if ( tableGeneratorAnnotation.uniqueConstraints() != null
					&& tableGeneratorAnnotation.uniqueConstraints().length > 0 ) {
				log.ignoringTableGeneratorConstraints( tableGeneratorAnnotation.name() );
			}
		}

		@Override
		@SuppressWarnings("deprecation")
		public void interpretSequenceGenerator(
				SequenceGenerator sequenceGeneratorAnnotation,
				IdentifierGeneratorDefinition.Builder definitionBuilder) {
			definitionBuilder.setName( sequenceGeneratorAnnotation.name() );

			definitionBuilder.setStrategy( "seqhilo" );

			if ( !BinderHelper.isEmptyAnnotationValue( sequenceGeneratorAnnotation.sequenceName() ) ) {
				definitionBuilder.addParam( org.hibernate.id.SequenceGenerator.SEQUENCE, sequenceGeneratorAnnotation.sequenceName() );
			}
			//FIXME: work on initialValue() through SequenceGenerator.PARAMETERS
			//		steve : or just use o.h.id.enhanced.SequenceStyleGenerator
			if ( sequenceGeneratorAnnotation.initialValue() != 1 ) {
				log.unsupportedInitialValue( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS );
			}
			definitionBuilder.addParam( SequenceHiLoGenerator.MAX_LO, String.valueOf( sequenceGeneratorAnnotation.allocationSize() - 1 ) );
		}
	}

	private static class FallbackInterpreter implements IdGeneratorStrategyInterpreter {
		/**
		 * Singleton access
		 */
		public static final FallbackInterpreter INSTANCE = new FallbackInterpreter();

		@Override
		public String determineGeneratorName(GenerationType generationType, GeneratorNameDeterminationContext context) {
			switch ( generationType ) {
				case IDENTITY: {
					return "identity";
				}
				case SEQUENCE: {
					return org.hibernate.id.enhanced.SequenceStyleGenerator.class.getName();
				}
				case TABLE: {
					return org.hibernate.id.enhanced.TableGenerator.class.getName();
				}
				default: {
					// AUTO

					if ( "increment".equalsIgnoreCase( context.getGeneratedValueGeneratorName() ) ) {
						return IncrementGenerator.class.getName();
					}

					final Class javaType = context.getIdType();
					if ( UUID.class.isAssignableFrom( javaType ) ) {
						return UUIDGenerator.class.getName();
					}

					return org.hibernate.id.enhanced.SequenceStyleGenerator.class.getName();
				}
			}
		}

		@Override
		public void interpretTableGenerator(
				TableGenerator tableGeneratorAnnotation,
				IdentifierGeneratorDefinition.Builder definitionBuilder) {
			definitionBuilder.setName( tableGeneratorAnnotation.name() );
			definitionBuilder.setStrategy( org.hibernate.id.enhanced.TableGenerator.class.getName() );
			definitionBuilder.addParam( org.hibernate.id.enhanced.TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );

			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.catalog() ) ) {
				definitionBuilder.addParam( PersistentIdentifierGenerator.CATALOG, tableGeneratorAnnotation.catalog() );
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.schema() ) ) {
				definitionBuilder.addParam( PersistentIdentifierGenerator.SCHEMA, tableGeneratorAnnotation.schema() );
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.table() ) ) {
				definitionBuilder.addParam(
						org.hibernate.id.enhanced.TableGenerator.TABLE_PARAM,
						tableGeneratorAnnotation.table()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.pkColumnName() ) ) {
				definitionBuilder.addParam(
						org.hibernate.id.enhanced.TableGenerator.SEGMENT_COLUMN_PARAM,
						tableGeneratorAnnotation.pkColumnName()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.pkColumnValue() ) ) {
				definitionBuilder.addParam(
						org.hibernate.id.enhanced.TableGenerator.SEGMENT_VALUE_PARAM,
						tableGeneratorAnnotation.pkColumnValue()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableGeneratorAnnotation.valueColumnName() ) ) {
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
				log.ignoringTableGeneratorConstraints( tableGeneratorAnnotation.name() );
			}
		}

		@Override
		public void interpretSequenceGenerator(
				SequenceGenerator sequenceGeneratorAnnotation,
				IdentifierGeneratorDefinition.Builder definitionBuilder) {
			definitionBuilder.setName( sequenceGeneratorAnnotation.name() );
			definitionBuilder.setStrategy( SequenceStyleGenerator.class.getName() );

			if ( !BinderHelper.isEmptyAnnotationValue( sequenceGeneratorAnnotation.catalog() ) ) {
				definitionBuilder.addParam(
						PersistentIdentifierGenerator.CATALOG,
						sequenceGeneratorAnnotation.catalog()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( sequenceGeneratorAnnotation.schema() ) ) {
				definitionBuilder.addParam(
						PersistentIdentifierGenerator.SCHEMA,
						sequenceGeneratorAnnotation.schema()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( sequenceGeneratorAnnotation.sequenceName() ) ) {
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
}
