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
package org.hibernate.metamodel.binder.source.annotations.global;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
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
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.annotations.JandexHelper;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binder.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.binder.source.annotations.JPADotNames;

public class IdGeneratorBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			IdGeneratorBinder.class.getName()
	);

	private IdGeneratorBinder() {
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

	/**
	 * Binds all {@link SequenceGenerator}, {@link javax.persistence.TableGenerator}, {@link GenericGenerator}, and {
	 * {@link GenericGenerators} annotations to the supplied metadata.
	 *
	 * @param metadata the global metadata
	 * @param jandex the jandex index
	 */
	public static void bind(MetadataImplementor metadata, Index jandex) {
		for ( AnnotationInstance generator : jandex.getAnnotations( JPADotNames.SEQUENCE_GENERATOR ) ) {
			bindSequenceGenerator( metadata, generator );
		}
		for ( AnnotationInstance generator : jandex.getAnnotations( JPADotNames.TABLE_GENERATOR ) ) {
			bindTableGenerator( metadata, generator );
		}
		for ( AnnotationInstance generator : jandex.getAnnotations( HibernateDotNames.GENERIC_GENERATOR ) ) {
			bindGenericGenerator( metadata, generator );
		}
		for ( AnnotationInstance generators : jandex.getAnnotations( HibernateDotNames.GENERIC_GENERATORS ) ) {
			for ( AnnotationInstance generator : JandexHelper.getValue(
					generators,
					"value",
					AnnotationInstance[].class
			) ) {
				bindGenericGenerator( metadata, generator );
			}
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
		String strategy;
		Map<String, String> prms = new HashMap<String, String>();
		addStringParameter( generator, "sequenceName", prms, SequenceStyleGenerator.SEQUENCE_PARAM );
		boolean useNewIdentifierGenerators = metadata.getOptions().useNewIdentifierGenerators();
		strategy = generatorType( GenerationType.SEQUENCE, useNewIdentifierGenerators );
		if ( useNewIdentifierGenerators ) {
			addStringParameter( generator, "catalog", prms, PersistentIdentifierGenerator.CATALOG );
			addStringParameter( generator, "schema", prms, PersistentIdentifierGenerator.SCHEMA );
			prms.put(
					SequenceStyleGenerator.INCREMENT_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class ) )
			);
			prms.put(
					SequenceStyleGenerator.INITIAL_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "initialValue", Integer.class ) )
			);
		}
		else {
			if ( JandexHelper.getValue( generator, "initialValue", Integer.class ) != 1 ) {
				LOG.unsupportedInitialValue( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS );
			}
			prms.put(
					SequenceHiLoGenerator.MAX_LO,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class ) - 1 )
			);
		}
		metadata.addIdGenerator( new IdGenerator( name, strategy, prms ) );
		LOG.tracef( "Add sequence generator with name: %s", name );
	}

	private static void bindTableGenerator(MetadataImplementor metadata, AnnotationInstance generator) {
		String name = JandexHelper.getValue( generator, "name", String.class );
		String strategy;
		Map<String, String> prms = new HashMap<String, String>();
		addStringParameter( generator, "catalog", prms, PersistentIdentifierGenerator.CATALOG );
		addStringParameter( generator, "schema", prms, PersistentIdentifierGenerator.SCHEMA );
		boolean useNewIdentifierGenerators = metadata.getOptions().useNewIdentifierGenerators();
		strategy = generatorType( GenerationType.TABLE, useNewIdentifierGenerators );
		if ( useNewIdentifierGenerators ) {
			prms.put( TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );
			addStringParameter( generator, "table", prms, TableGenerator.TABLE_PARAM );
			addStringParameter( generator, "pkColumnName", prms, TableGenerator.SEGMENT_COLUMN_PARAM );
			addStringParameter( generator, "pkColumnValue", prms, TableGenerator.SEGMENT_VALUE_PARAM );
			addStringParameter( generator, "valueColumnName", prms, TableGenerator.VALUE_COLUMN_PARAM );
			prms.put(
					TableGenerator.INCREMENT_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", String.class ) )
			);
			prms.put(
					TableGenerator.INITIAL_PARAM,
					String.valueOf( JandexHelper.getValue( generator, "initialValue", String.class ) + 1 )
			);
		}
		else {
			addStringParameter( generator, "table", prms, MultipleHiLoPerTableGenerator.ID_TABLE );
			addStringParameter( generator, "pkColumnName", prms, MultipleHiLoPerTableGenerator.PK_COLUMN_NAME );
			addStringParameter( generator, "pkColumnValue", prms, MultipleHiLoPerTableGenerator.PK_VALUE_NAME );
			addStringParameter( generator, "valueColumnName", prms, MultipleHiLoPerTableGenerator.VALUE_COLUMN_NAME );
			prms.put(
					TableHiLoGenerator.MAX_LO,
					String.valueOf( JandexHelper.getValue( generator, "allocationSize", Integer.class ) - 1 )
			);
		}
		if ( JandexHelper.getValue( generator, "uniqueConstraints", AnnotationInstance[].class ).length > 0 ) {
			LOG.ignoringTableGeneratorConstraints( name );
		}
		metadata.addIdGenerator( new IdGenerator( name, strategy, prms ) );
		LOG.tracef( "Add table generator with name: %s", name );
	}

	public static String generatorType(GenerationType generatorEnum, boolean useNewGeneratorMappings) {
		switch ( generatorEnum ) {
			case IDENTITY:
				return "identity";
			case AUTO:
				return useNewGeneratorMappings
						? "enhanced-sequence"
						: "native";
			case TABLE:
				return useNewGeneratorMappings
						? "enhanced-table"
						: MultipleHiLoPerTableGenerator.class.getName();
			case SEQUENCE:
				return useNewGeneratorMappings
						? "enhanced-sequence"
						: "seqhilo";
		}
		throw new AssertionFailure( "Unknown GeneratorType: " + generatorEnum );
	}
}
