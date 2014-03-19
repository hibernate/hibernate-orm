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
package org.hibernate.metamodel.source.internal.annotations.global;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.IdentifierGeneratorSource;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

/**
 * Binds {@link SequenceGenerator}, {@link javax.persistence.TableGenerator}, {@link GenericGenerator}, and
 * {@link GenericGenerators} annotations.
 *
 * @author Hardy Ferentschik
 */
public class IdGeneratorProcessor {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			IdGeneratorProcessor.class.getName()
	);

	private IdGeneratorProcessor() {
	}

	/**
	 * Binds all {@link SequenceGenerator}, {@link javax.persistence.TableGenerator}, {@link GenericGenerator}, and
	 * {@link GenericGenerators} annotations to the supplied metadata.
	 *
	 * @param bindingContext the context for annotation binding
	 */
	public static Iterable<IdentifierGeneratorSource> extractGlobalIdentifierGeneratorSources(AnnotationBindingContext bindingContext) {
		List<IdentifierGeneratorSource> identifierGeneratorSources = new ArrayList<IdentifierGeneratorSource>();
		Collection<AnnotationInstance> annotations = bindingContext.getJandexAccess()
				.getIndex()
				.getAnnotations( JPADotNames.SEQUENCE_GENERATOR );
		for ( AnnotationInstance generator : annotations ) {
			bindSequenceGenerator( generator, identifierGeneratorSources, bindingContext );
		}

		annotations = bindingContext.getJandexAccess().getIndex().getAnnotations( JPADotNames.TABLE_GENERATOR );
		for ( AnnotationInstance generator : annotations ) {
			bindTableGenerator( generator, identifierGeneratorSources, bindingContext );
		}

		annotations = JandexHelper.getAnnotations(
				bindingContext.getJandexAccess().getIndex(),
				HibernateDotNames.GENERIC_GENERATOR,
				HibernateDotNames.GENERIC_GENERATORS,
				bindingContext.getServiceRegistry().getService( ClassLoaderService.class )
		);
		for ( AnnotationInstance generator : annotations ) {
			bindGenericGenerator( generator, identifierGeneratorSources, bindingContext );
		}
		return identifierGeneratorSources;
	}

	private static void addStringParameter(
			final AnnotationInstance annotation,
			final String element,
			final Map<String, String> parameters,
			final String parameter,
			final AnnotationBindingContext bindingContext) {
		String string = JandexHelper.getValue( annotation, element, String.class,
				bindingContext.getServiceRegistry().getService( ClassLoaderService.class ));
		if ( StringHelper.isNotEmpty( string ) ) {
			parameters.put( parameter, string );
		}
	}

	private static void bindGenericGenerator(
			final AnnotationInstance generator,
			final List<IdentifierGeneratorSource> identifierGeneratorSources,
			final AnnotationBindingContext bindingContext) {
		final ClassLoaderService classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );
		final String name = JandexHelper.getValue( generator, "name", String.class, classLoaderService );
		final Map<String, String> parameterMap = new HashMap<String, String>();
		final AnnotationInstance[] parameterAnnotations = JandexHelper.getValue(
				generator,
				"parameters",
				AnnotationInstance[].class,
				classLoaderService
		);
		for ( AnnotationInstance parameterAnnotation : parameterAnnotations ) {
			parameterMap.put(
					JandexHelper.getValue( parameterAnnotation, "name", String.class, classLoaderService ),
					JandexHelper.getValue( parameterAnnotation, "value", String.class, classLoaderService )
			);
		}
		identifierGeneratorSources.add(
				new IdentifierGeneratorSourceImpl(
						name,
						JandexHelper.getValue( generator, "strategy", String.class, classLoaderService ),
						parameterMap
				)
		);
		LOG.tracef( "Add generic generator with name: %s", name );
	}

	private static void bindSequenceGenerator(
			final AnnotationInstance generator,
			final List<IdentifierGeneratorSource> identifierGeneratorSources,
			final AnnotationBindingContext bindingContext) {
		final ClassLoaderService classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );
		final String name = JandexHelper.getValue( generator, "name", String.class, classLoaderService );

		final boolean useNewIdentifierGenerators = bindingContext.getBuildingOptions().isUseNewIdentifierGenerators();
		final String strategy = EnumConversionHelper.generationTypeToGeneratorStrategyName(
				GenerationType.SEQUENCE,
				useNewIdentifierGenerators
		);

		final Map<String, String> parameterMap = new HashMap<String, String>();
		addStringParameter( generator, "sequenceName", parameterMap, SequenceStyleGenerator.SEQUENCE_PARAM, bindingContext );

		if ( useNewIdentifierGenerators ) {
			addStringParameter( generator, "catalog", parameterMap, PersistentIdentifierGenerator.CATALOG, bindingContext );
			addStringParameter( generator, "schema", parameterMap, PersistentIdentifierGenerator.SCHEMA, bindingContext );
			parameterMap.put(
					SequenceStyleGenerator.INCREMENT_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class,
							classLoaderService ) )
			);
			parameterMap.put(
					SequenceStyleGenerator.INITIAL_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "initialValue", Integer.class,
							classLoaderService ) )
			);
		}
		else {
			if ( JandexHelper.getValue( generator, "initialValue", Integer.class, classLoaderService ) != 1 ) {
				LOG.unsupportedInitialValue( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS );
			}
			parameterMap.put(
					SequenceHiLoGenerator.MAX_LO,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class,
							classLoaderService ) - 1 )
			);
		}
		identifierGeneratorSources.add( new IdentifierGeneratorSourceImpl( name, strategy, parameterMap ) );
		LOG.tracef( "Add sequence generator with name: %s", name );
	}

	private static void bindTableGenerator(
			final AnnotationInstance generator,
			final List<IdentifierGeneratorSource> identifierGeneratorSources,
			final AnnotationBindingContext bindingContext) {
		final ClassLoaderService classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );
		final String name = JandexHelper.getValue( generator, "name", String.class, classLoaderService );

		final boolean useNewIdentifierGenerators = bindingContext.getBuildingOptions().isUseNewIdentifierGenerators();
		final String strategy = EnumConversionHelper.generationTypeToGeneratorStrategyName(
				GenerationType.TABLE,
				useNewIdentifierGenerators
		);

		final Map<String, String> parameterMap = new HashMap<String, String>();
		addStringParameter( generator, "catalog", parameterMap, PersistentIdentifierGenerator.CATALOG, bindingContext );
		addStringParameter( generator, "schema", parameterMap, PersistentIdentifierGenerator.SCHEMA, bindingContext );

		if ( useNewIdentifierGenerators ) {
			parameterMap.put( TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );
			addStringParameter( generator, "table", parameterMap, TableGenerator.TABLE_PARAM, bindingContext );
			addStringParameter( generator, "pkColumnName", parameterMap, TableGenerator.SEGMENT_COLUMN_PARAM, bindingContext );
			addStringParameter( generator, "pkColumnValue", parameterMap, TableGenerator.SEGMENT_VALUE_PARAM, bindingContext );
			addStringParameter( generator, "valueColumnName", parameterMap, TableGenerator.VALUE_COLUMN_PARAM, bindingContext );
			parameterMap.put(
					TableGenerator.INCREMENT_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class,
							classLoaderService ) )
			);
			parameterMap.put(
					TableGenerator.INITIAL_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "initialValue", Integer.class,
							classLoaderService ) + 1 )
			);
		}
		else {
			addStringParameter( generator, "table", parameterMap, MultipleHiLoPerTableGenerator.ID_TABLE, bindingContext );
			addStringParameter( generator, "pkColumnName", parameterMap, MultipleHiLoPerTableGenerator.PK_COLUMN_NAME, bindingContext );
			addStringParameter( generator, "pkColumnValue", parameterMap, MultipleHiLoPerTableGenerator.PK_VALUE_NAME, bindingContext );
			addStringParameter(
					generator,
					"valueColumnName",
					parameterMap,
					MultipleHiLoPerTableGenerator.VALUE_COLUMN_NAME,
					bindingContext
			);
			parameterMap.put(
					TableHiLoGenerator.MAX_LO,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class,
							classLoaderService ) - 1 )
			);
		}
		if ( JandexHelper.getValue( generator, "uniqueConstraints", AnnotationInstance[].class,
				classLoaderService ).length > 0 ) {
			LOG.ignoringTableGeneratorConstraints( name );
		}
		identifierGeneratorSources.add( new IdentifierGeneratorSourceImpl( name, strategy, parameterMap ) );
		LOG.tracef( "Add table generator with name: %s", name );
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
