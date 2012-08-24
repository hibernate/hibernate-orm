/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.GenerationType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.source.IdentifierGeneratorSource;

/**
 * @author Steve Ebersole
 */
public class IdentifierGeneratorExtractionDelegate {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			IdentifierGeneratorExtractionDelegate.class.getName()
	);

	private final boolean useNewIdentifierGenerators;

	public IdentifierGeneratorExtractionDelegate(boolean useNewIdentifierGenerators) {
		this.useNewIdentifierGenerators = useNewIdentifierGenerators;
	}

	public Iterable<IdentifierGeneratorSource> extractIdentifierGeneratorSources(IdentifierGeneratorSourceContainer container) {
		List<IdentifierGeneratorSource> identifierGeneratorSources = new ArrayList<IdentifierGeneratorSource>();
		processIdentifierGeneratorSources( identifierGeneratorSources, container );
		return identifierGeneratorSources;
	}

	private void processIdentifierGeneratorSources(
			List<IdentifierGeneratorSource> identifierGeneratorSources,
			IdentifierGeneratorSourceContainer identifierGeneratorSourceContainer) {

		processSequenceGenerators(
				identifierGeneratorSources,
				identifierGeneratorSourceContainer.getSequenceGeneratorSources()
		);

		processTableGenerators(
				identifierGeneratorSources,
				identifierGeneratorSourceContainer.getTableGeneratorSources()
		);

		processGenericGenerators(
				identifierGeneratorSources,
				identifierGeneratorSourceContainer.getGenericGeneratorSources()
		);
	}

	private void processSequenceGenerators(
			List<IdentifierGeneratorSource> identifierGeneratorSources,
			Collection<AnnotationInstance> generatorAnnotations) {
		for ( AnnotationInstance generatorAnnotation : generatorAnnotations ) {
			final String generatorName = JandexHelper.getValue( generatorAnnotation, "name", String.class );

			final String generatorImplementationName = EnumConversionHelper.generationTypeToGeneratorStrategyName(
					GenerationType.SEQUENCE,
					useNewIdentifierGenerators
			);

			Map<String, String> parameterMap = new HashMap<String, String>();
			final String sequenceName = JandexHelper.getValue( generatorAnnotation, "sequenceName", String.class );
			if ( StringHelper.isNotEmpty( sequenceName ) ) {
				parameterMap.put( SequenceStyleGenerator.SEQUENCE_PARAM, sequenceName );
			}

			if ( useNewIdentifierGenerators ) {
				final String catalog = JandexHelper.getValue( generatorAnnotation, "catalog", String.class );
				if ( StringHelper.isNotEmpty( catalog ) ) {
					parameterMap.put( PersistentIdentifierGenerator.CATALOG, catalog );
				}
				final String schema = JandexHelper.getValue( generatorAnnotation, "schema", String.class );
				if ( StringHelper.isNotEmpty( schema ) ) {
					parameterMap.put( PersistentIdentifierGenerator.SCHEMA, schema );
				}
				parameterMap.put(
						SequenceStyleGenerator.INCREMENT_PARAM,
						String.valueOf( JandexHelper.getValue( generatorAnnotation, "allocationSize", Integer.class ) )
				);
				parameterMap.put(
						SequenceStyleGenerator.INITIAL_PARAM,
						String.valueOf( JandexHelper.getValue( generatorAnnotation, "initialValue", Integer.class ) )
				);
			}
			else {
				if ( JandexHelper.getValue( generatorAnnotation, "initialValue", Integer.class ) != 1 ) {
					LOG.unsupportedInitialValue( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS );
				}
				parameterMap.put(
						SequenceHiLoGenerator.MAX_LO,
						String.valueOf( JandexHelper.getValue( generatorAnnotation, "allocationSize", Integer.class ) - 1 )
				);
			}

			identifierGeneratorSources.add(
					new IdentifierGeneratorSourceImpl( generatorName, generatorImplementationName, parameterMap )
			);
		}
	}

	private void processTableGenerators(
			List<IdentifierGeneratorSource> identifierGeneratorSources,
			Collection<AnnotationInstance> annotations) {
		for ( AnnotationInstance generatorAnnotation : annotations ) {
			final String generatorName = JandexHelper.getValue( generatorAnnotation, "name", String.class );

			final String generatorImplementationName = EnumConversionHelper.generationTypeToGeneratorStrategyName(
					GenerationType.TABLE,
					useNewIdentifierGenerators
			);

			Map<String, String> parameterMap = new HashMap<String, String>();
			final String catalog = JandexHelper.getValue( generatorAnnotation, "catalog", String.class );
			if ( StringHelper.isNotEmpty( catalog ) ) {
				parameterMap.put( PersistentIdentifierGenerator.CATALOG, catalog );
			}
			final String schema = JandexHelper.getValue( generatorAnnotation, "schema", String.class );
			if ( StringHelper.isNotEmpty( schema ) ) {
				parameterMap.put( PersistentIdentifierGenerator.SCHEMA, schema );
			}

			if ( useNewIdentifierGenerators ) {
				parameterMap.put( TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );
				final String tableName = JandexHelper.getValue( generatorAnnotation, "table", String.class );
				if ( StringHelper.isNotEmpty( tableName ) ) {
					parameterMap.put( TableGenerator.TABLE_PARAM, tableName );
				}

				final String segmentColumnName = JandexHelper.getValue(
						generatorAnnotation,
						"pkColumnName",
						String.class
				);
				if ( StringHelper.isNotEmpty( segmentColumnName ) ) {
					parameterMap.put( TableGenerator.SEGMENT_COLUMN_PARAM, segmentColumnName );
				}

				final String segmentColumnValue = JandexHelper.getValue( generatorAnnotation, "pkColumnValue", String.class );
				if ( StringHelper.isNotEmpty( segmentColumnValue ) ) {
					parameterMap.put( TableGenerator.SEGMENT_VALUE_PARAM, segmentColumnValue );
				}

				final String valueColumnName = JandexHelper.getValue( generatorAnnotation, "valueColumnName", String.class );
				if ( StringHelper.isNotEmpty( valueColumnName ) ) {
					parameterMap.put( TableGenerator.VALUE_COLUMN_PARAM, valueColumnName );
				}

				parameterMap.put(
						TableGenerator.INCREMENT_PARAM,
						String.valueOf( JandexHelper.getValue( generatorAnnotation, "allocationSize", String.class ) )
				);

				parameterMap.put(
						TableGenerator.INITIAL_PARAM,
						String.valueOf( JandexHelper.getValue( generatorAnnotation, "initialValue", String.class ) + 1 )
				);
			}
			else {
				final String tableName = JandexHelper.getValue( generatorAnnotation, "table", String.class );
				if ( StringHelper.isNotEmpty( tableName ) ) {
					parameterMap.put( MultipleHiLoPerTableGenerator.ID_TABLE, tableName );
				}

				final String segmentColumnName = JandexHelper.getValue( generatorAnnotation, "pkColumnName", String.class );
				if ( StringHelper.isNotEmpty( segmentColumnName ) ) {
					parameterMap.put( MultipleHiLoPerTableGenerator.PK_COLUMN_NAME, segmentColumnName );
				}

				final String segmentColumnValue = JandexHelper.getValue( generatorAnnotation, "pkColumnValue", String.class );
				if ( StringHelper.isNotEmpty( segmentColumnValue ) ) {
					parameterMap.put( MultipleHiLoPerTableGenerator.PK_VALUE_NAME, segmentColumnValue );
				}

				final String valueColumnName = JandexHelper.getValue( generatorAnnotation, "valueColumnName", String.class );
				if ( StringHelper.isNotEmpty( valueColumnName ) ) {
					parameterMap.put( MultipleHiLoPerTableGenerator.VALUE_COLUMN_NAME, valueColumnName );
				}

				parameterMap.put(
						TableHiLoGenerator.MAX_LO,
						String.valueOf( JandexHelper.getValue( generatorAnnotation, "allocationSize", Integer.class ) - 1 )
				);
			}

			if ( JandexHelper.getValue( generatorAnnotation, "uniqueConstraints", AnnotationInstance[].class ).length > 0 ) {
				LOG.ignoringTableGeneratorConstraints( generatorName );
			}

			identifierGeneratorSources.add(
					new IdentifierGeneratorSourceImpl( generatorName, generatorImplementationName, parameterMap )
			);
		}
	}

	private void processGenericGenerators(
			List<IdentifierGeneratorSource> identifierGeneratorSources,
			Collection<AnnotationInstance> genericGeneratorSources) {
		for ( AnnotationInstance generatorAnnotation : genericGeneratorSources ) {
			Map<String, String> parameterMap = new HashMap<String, String>();
			AnnotationInstance[] parameterAnnotations = JandexHelper.getValue(
					generatorAnnotation,
					"parameters",
					AnnotationInstance[].class
			);
			for ( AnnotationInstance parameterAnnotation : parameterAnnotations ) {
				parameterMap.put(
						JandexHelper.getValue( parameterAnnotation, "name", String.class ),
						JandexHelper.getValue( parameterAnnotation, "value", String.class )
				);
			}

			identifierGeneratorSources.add(
					new IdentifierGeneratorSourceImpl(
							JandexHelper.getValue( generatorAnnotation, "name", String.class ),
							JandexHelper.getValue( generatorAnnotation, "strategy", String.class ),
							parameterMap
					)
			);
		}
	}

	private static class IdentifierGeneratorSourceImpl implements IdentifierGeneratorSource {
		private final String generatorName;
		private final String generatorImplementationName;
		private final Map<String, String> parameterMap;

		public IdentifierGeneratorSourceImpl(
				String generatorName,
				String generatorImplementationName,
				Map<String, String> parameterMap) {
			this.generatorName = generatorName;
			this.generatorImplementationName = generatorImplementationName;
			this.parameterMap = parameterMap;
		}

		@Override
		public String getGeneratorName() {
			return generatorName;
		}

		@Override
		public String getGeneratorImplementationName() {
			return generatorImplementationName;
		}

		@Override
		public Map<String, String> getParameters() {
			return parameterMap;
		}
	}
}
