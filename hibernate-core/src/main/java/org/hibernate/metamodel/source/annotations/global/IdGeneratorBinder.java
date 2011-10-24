/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.global;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.EnumConversionHelper;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * Binds {@link SequenceGenerator}, {@link javax.persistence.TableGenerator}, {@link GenericGenerator}, and
 * {@link GenericGenerators} annotations.
 *
 * @author Hardy Ferentschik
 */
public class IdGeneratorBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			IdGeneratorBinder.class.getName()
	);

	private IdGeneratorBinder() {
	}

	/**
	 * Binds all {@link SequenceGenerator}, {@link javax.persistence.TableGenerator}, {@link GenericGenerator}, and
	 * {@link GenericGenerators} annotations to the supplied metadata.
	 *
	 * @param bindingContext the context for annotation binding
	 */
	public static void bind(AnnotationBindingContext bindingContext) {
		List<AnnotationInstance> annotations = bindingContext.getIndex()
				.getAnnotations( JPADotNames.SEQUENCE_GENERATOR );
		for ( AnnotationInstance generator : annotations ) {
			bindSequenceGenerator( bindingContext.getMetadataImplementor(), generator );
		}

		annotations = bindingContext.getIndex().getAnnotations( JPADotNames.TABLE_GENERATOR );
		for ( AnnotationInstance generator : annotations ) {
			bindTableGenerator( bindingContext.getMetadataImplementor(), generator );
		}

		annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.GENERIC_GENERATOR );
		for ( AnnotationInstance generator : annotations ) {
			bindGenericGenerator( bindingContext.getMetadataImplementor(), generator );
		}

		annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.GENERIC_GENERATORS );
		for ( AnnotationInstance generators : annotations ) {
			for ( AnnotationInstance generator : JandexHelper.getValue(
					generators,
					"value",
					AnnotationInstance[].class
			) ) {
				bindGenericGenerator( bindingContext.getMetadataImplementor(), generator );
			}
		}
	}

	private static void addStringParameter(AnnotationInstance annotation,
										   String element,
										   Map<String, String> parameters,
										   String parameter) {
		String string = JandexHelper.getValue( annotation, element, String.class );
		if ( StringHelper.isNotEmpty( string ) ) {
			parameters.put( parameter, string );
		}
	}

	private static void bindGenericGenerator(MetadataImplementor metadata, AnnotationInstance generator) {
		String name = JandexHelper.getValue( generator, "name", String.class );
		Map<String, String> parameterMap = new HashMap<String, String>();
		AnnotationInstance[] parameterAnnotations = JandexHelper.getValue(
				generator,
				"parameters",
				AnnotationInstance[].class
		);
		for ( AnnotationInstance parameterAnnotation : parameterAnnotations ) {
			parameterMap.put(
					JandexHelper.getValue( parameterAnnotation, "name", String.class ),
					JandexHelper.getValue( parameterAnnotation, "value", String.class )
			);
		}
		metadata.addIdGenerator(
				new IdGenerator(
						name,
						JandexHelper.getValue( generator, "strategy", String.class ),
						parameterMap
				)
		);
		LOG.tracef( "Add generic generator with name: %s", name );
	}

	private static void bindSequenceGenerator(MetadataImplementor metadata, AnnotationInstance generator) {
		String name = JandexHelper.getValue( generator, "name", String.class );
		Map<String, String> parameterMap = new HashMap<String, String>();
		addStringParameter( generator, "sequenceName", parameterMap, SequenceStyleGenerator.SEQUENCE_PARAM );
		boolean useNewIdentifierGenerators = metadata.getOptions().useNewIdentifierGenerators();
		String strategy = EnumConversionHelper.generationTypeToGeneratorStrategyName(
				GenerationType.SEQUENCE,
				useNewIdentifierGenerators
		);
		if ( useNewIdentifierGenerators ) {
			addStringParameter( generator, "catalog", parameterMap, PersistentIdentifierGenerator.CATALOG );
			addStringParameter( generator, "schema", parameterMap, PersistentIdentifierGenerator.SCHEMA );
			parameterMap.put(
					SequenceStyleGenerator.INCREMENT_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class ) )
			);
			parameterMap.put(
					SequenceStyleGenerator.INITIAL_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "initialValue", Integer.class ) )
			);
		}
		else {
			if ( JandexHelper.getValue( generator, "initialValue", Integer.class ) != 1 ) {
				LOG.unsupportedInitialValue( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS );
			}
			parameterMap.put(
					SequenceHiLoGenerator.MAX_LO,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class ) - 1 )
			);
		}
		metadata.addIdGenerator( new IdGenerator( name, strategy, parameterMap ) );
		LOG.tracef( "Add sequence generator with name: %s", name );
	}

	private static void bindTableGenerator(MetadataImplementor metadata, AnnotationInstance generator) {
		String name = JandexHelper.getValue( generator, "name", String.class );
		Map<String, String> parameterMap = new HashMap<String, String>();
		addStringParameter( generator, "catalog", parameterMap, PersistentIdentifierGenerator.CATALOG );
		addStringParameter( generator, "schema", parameterMap, PersistentIdentifierGenerator.SCHEMA );
		boolean useNewIdentifierGenerators = metadata.getOptions().useNewIdentifierGenerators();
		String strategy = EnumConversionHelper.generationTypeToGeneratorStrategyName(
				GenerationType.TABLE,
				useNewIdentifierGenerators
		);
		if ( useNewIdentifierGenerators ) {
			parameterMap.put( TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );
			addStringParameter( generator, "table", parameterMap, TableGenerator.TABLE_PARAM );
			addStringParameter( generator, "pkColumnName", parameterMap, TableGenerator.SEGMENT_COLUMN_PARAM );
			addStringParameter( generator, "pkColumnValue", parameterMap, TableGenerator.SEGMENT_VALUE_PARAM );
			addStringParameter( generator, "valueColumnName", parameterMap, TableGenerator.VALUE_COLUMN_PARAM );
			parameterMap.put(
					TableGenerator.INCREMENT_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", String.class ) )
			);
			parameterMap.put(
					TableGenerator.INITIAL_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "initialValue", String.class ) + 1 )
			);
		}
		else {
			addStringParameter( generator, "table", parameterMap, MultipleHiLoPerTableGenerator.ID_TABLE );
			addStringParameter( generator, "pkColumnName", parameterMap, MultipleHiLoPerTableGenerator.PK_COLUMN_NAME );
			addStringParameter( generator, "pkColumnValue", parameterMap, MultipleHiLoPerTableGenerator.PK_VALUE_NAME );
			addStringParameter( generator, "valueColumnName", parameterMap, MultipleHiLoPerTableGenerator.VALUE_COLUMN_NAME );
			parameterMap.put(
					TableHiLoGenerator.MAX_LO,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class ) - 1 )
			);
		}
		if ( JandexHelper.getValue( generator, "uniqueConstraints", AnnotationInstance[].class ).length > 0 ) {
			LOG.ignoringTableGeneratorConstraints( name );
		}
		metadata.addIdGenerator( new IdGenerator( name, strategy, parameterMap ) );
		LOG.tracef( "Add table generator with name: %s", name );
	}
}
