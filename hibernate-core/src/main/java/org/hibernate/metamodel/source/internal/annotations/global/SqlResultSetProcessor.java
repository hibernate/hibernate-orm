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
package org.hibernate.metamodel.source.internal.annotations.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryConstructorReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.SQL_RESULT_SET_MAPPING;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.SQL_RESULT_SET_MAPPINGS;

/**
 * Handles processing of SQL ResultSet mappings as defined via
 * {@link javax.persistence.SqlResultSetMapping} and
 * {@link javax.persistence.SqlResultSetMappings} annotations, including
 * their related annotations:<ul>
 *     <li>
 *         {@link javax.persistence.EntityResult} (and
 *         {@link javax.persistence.FieldResult})
 *     </li>
 *     <li>
 *         {@link javax.persistence.ColumnResult}
 *     </li>
 *     <li>
 *         {@link javax.persistence.ConstructorResult}
 *     </li>
 * </ul>
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public class SqlResultSetProcessor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QueryProcessor.class );

	/**
	 * Disallow direct instantiation.
	 */
	private SqlResultSetProcessor() {
	}

	/**
	 * Main entry point into processing SQL ResultSet mappings
	 *
	 * @param bindingContext The binding context
	 */
	public static void bind(AnnotationBindingContext bindingContext) {
		// singular form
		{
			final Collection<AnnotationInstance> sqlResultSetMappingAnnotations =
					bindingContext.getJandexAccess().getIndex().getAnnotations( SQL_RESULT_SET_MAPPING );
			for ( final AnnotationInstance sqlResultSetMappingAnnotation : sqlResultSetMappingAnnotations ) {
				bindSqlResultSetMapping( sqlResultSetMappingAnnotation, bindingContext );
			}
		}

		// plural form
		{
			final Collection<AnnotationInstance> sqlResultSetMappingsAnnotations =
					bindingContext.getJandexAccess().getIndex().getAnnotations( SQL_RESULT_SET_MAPPINGS );
			for ( final AnnotationInstance sqlResultSetMappingsAnnotationInstance : sqlResultSetMappingsAnnotations ) {
				final AnnotationInstance[] sqlResultSetMappingAnnotations = JandexHelper.extractAnnotationsValue(
						sqlResultSetMappingsAnnotationInstance,
						"value"
				);
				for ( AnnotationInstance sqlResultSetMappingAnnotation : sqlResultSetMappingAnnotations ) {
					bindSqlResultSetMapping( sqlResultSetMappingAnnotation, bindingContext );
				}
			}
		}
	}

	private static int entityAliasIndex = 0;

	private static void bindSqlResultSetMapping(AnnotationInstance annotation, AnnotationBindingContext bindingContext) {
		entityAliasIndex = 0;

		// `name` is required...
		final String name = annotation.value( "name" ).asString();
		LOG.debugf( "Binding @SqlResultSetMapping(name=%s)", name );
		final ResultSetMappingDefinition definition = new ResultSetMappingDefinition( name );

		final AnnotationInstance[] entityResults = JandexHelper.extractAnnotationsValue(
				annotation,
				"entities"
		);
		if ( entityResults != null && entityResults.length > 0 ) {
			for ( AnnotationInstance entityResult : entityResults ) {
				bindEntityResult( entityResult, definition, bindingContext );
			}
		}

		final AnnotationInstance[] columnResults = JandexHelper.extractAnnotationsValue(
				annotation,
				"columns"
		);
		if ( columnResults != null && columnResults.length > 0 ) {
			for ( AnnotationInstance columnResult : columnResults ) {
				bindColumnResult( columnResult, definition, bindingContext );
			}
		}

		final AnnotationInstance[] constructorResults = JandexHelper.extractAnnotationsValue(
				annotation,
				"classes"
		);
		if ( constructorResults != null && constructorResults.length > 0 ) {
			for ( AnnotationInstance constructorResult : constructorResults ) {
				bindConstructorResult( constructorResult, definition, bindingContext );
			}
		}

		bindingContext.getMetadataCollector().addResultSetMapping( definition );
	}

	private static void bindEntityResult(
			final AnnotationInstance entityResult,
			final ResultSetMappingDefinition definition,
			final AnnotationBindingContext bindingContext) {
		final String className = entityResult.value( "entityClass" ).asString();
		final EntityBinding targetEntityBinding = bindingContext.getMetadataCollector().getEntityBinding( className );
		if ( targetEntityBinding == null ) {
			throw new MappingException(
					String.format(
							"Entity [%s] not found in SqlResultMapping [%s]",
							className,
							definition.getName()
					)
			);
		}

		final Map<String, String[]> propertyResults = new HashMap<String, String[]>();

		final AnnotationValue discriminatorColumnValue = entityResult.value( "discriminatorColumn" );
		if ( discriminatorColumnValue != null ) {
			final String discriminatorColumn = discriminatorColumnValue.asString();
			if ( StringHelper.isNotEmpty( discriminatorColumn ) ) {
				propertyResults.put(
						"class",
						new String[] { normalize( discriminatorColumn, bindingContext ) }
				);

			}
		}

		List<FieldResult> fieldResultList = reorderFieldResult(
				bindingContext,
				entityResult,
				targetEntityBinding,
				definition.getName()
		);

		for ( final FieldResult fieldResult : fieldResultList ) {
			insert(  fieldResult.column, StringHelper.root( fieldResult.name ), propertyResults );
		}

		final NativeSQLQueryRootReturn result = new NativeSQLQueryRootReturn(
				"alias" + entityAliasIndex++,
				targetEntityBinding.getEntityName(),
				propertyResults,
				LockMode.READ
		);
		definition.addQueryReturn( result );
	}

	private static String normalize(String name, AnnotationBindingContext bindingContext) {
		return bindingContext.getMetadataCollector()
				.getObjectNameNormalizer()
				.normalizeIdentifierQuoting( name );
	}

	//todo see org.hibernate.cfg.annotations.ResultsetMappingSecondPass#getSubPropertyIterator
	private static List<FieldResult> reorderFieldResult(
			AnnotationBindingContext bindingContext,
			AnnotationInstance entityResult,
			EntityBinding entityBinding,
			String resultSetMappingDefinitionName) {
		final AnnotationInstance[] fieldResultAnnotationInstances = JandexHelper.extractAnnotationsValue(
				entityResult,
				"fields"
		);

		final List<FieldResult> results = new ArrayList<FieldResult>( fieldResultAnnotationInstances.length );
		final List<String> propertyNames = new ArrayList<String>();
		final Set<String> uniqueReturnProperty = new HashSet<String>();

		for ( final AnnotationInstance fieldResult : fieldResultAnnotationInstances ) {
			final String name = fieldResult.value( "name" ).asString();
			if ( !uniqueReturnProperty.add( name ) ) {
				throw new MappingException(
						"duplicate @FieldResult for property " + name
								+ " on @Entity " + entityBinding.getEntityName()
								+ " in " + resultSetMappingDefinitionName
				);
			}
			if ( "class".equals( name ) ) {
				throw new MappingException(
						"class is not a valid property name to use in a @FieldResult, " +
								"use @EntityResult(discriminatorColumn) instead"
				);
			}

			final String column = fieldResult.value( "column" ).asString();
			final String quotingNormalizedColumnName = normalize( column, bindingContext );
			if ( name.contains( "." ) ) {
				int dotIndex = name.lastIndexOf( '.' );
				String reducedName = name.substring( 0, dotIndex );
				AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( reducedName );
				Iterable<? extends AttributeBinding> attributeBindings = null;
				if ( EmbeddedAttributeBinding.class.isInstance( attributeBinding ) ) {
					EmbeddedAttributeBinding embeddedAttributeBinding = EmbeddedAttributeBinding.class.cast(
							attributeBinding
					);
					attributeBindings = embeddedAttributeBinding.getEmbeddableBinding().attributeBindings();

				}
				else if ( ManyToOneAttributeBinding.class.isInstance( attributeBinding ) ) {
					ManyToOneAttributeBinding manyToOneAttributeBinding = ManyToOneAttributeBinding.class.cast(
							attributeBinding
					);
					EntityBinding referencedEntityBinding = manyToOneAttributeBinding.getReferencedEntityBinding();
					Set<SingularAssociationAttributeBinding> referencingAttributeBindings = manyToOneAttributeBinding.getEntityReferencingAttributeBindings();

					if ( CollectionHelper.isNotEmpty( referencingAttributeBindings ) ) {
						attributeBindings = referencingAttributeBindings;
					}
					else {
//						EntityIdentifierNature entityIdentifierNature= referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getNature();
//						switch ( entityIdentifierNature ) {
//							case SIMPLE:
//								throw new MappingException(
//										"dotted notation reference neither a component nor a many/one to one"
//								);
//							case AGGREGATED_COMPOSITE:
//							case COMPOSITE:
//								referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().isSingleAttribute();//.isIdentifierMapper();
//						}

						//todo check if this logic is correct
						final EntityIdentifier idInfo = referencedEntityBinding.getHierarchyDetails()
								.getEntityIdentifier();

						if ( EntityIdentifier.AggregatedCompositeIdentifierBinding.class.isInstance( idInfo.getEntityIdentifierBinding() ) ) {
							final EntityIdentifier.AggregatedCompositeIdentifierBinding identifierBinding =
									(EntityIdentifier.AggregatedCompositeIdentifierBinding) idInfo.getEntityIdentifierBinding();
							attributeBindings = identifierBinding.getAttributeBinding()
									.getEmbeddableBinding().attributeBindings();
						}
						else if ( EntityIdentifier.NonAggregatedCompositeIdentifierBinding.class.isInstance( idInfo.getEntityIdentifierBinding() ) ) {
							final EntityIdentifier.NonAggregatedCompositeIdentifierBinding identifierBinding =
									(EntityIdentifier.NonAggregatedCompositeIdentifierBinding) idInfo.getEntityIdentifierBinding();
							attributeBindings = identifierBinding.getVirtualEmbeddableBinding().attributeBindings();
						}
						else {
							throw new MappingException(
									"dotted notation reference neither a component nor a many/one to one"
							);
						}
					}


				}
				else {
					throw new MappingException( "dotted notation reference neither a component nor a many/one to one" );
				}
				List<String> followers = getFollowers( attributeBindings, reducedName, name );
				int index = results.size();
				int followersSize = followers.size();
				for ( int loop = 0; loop < followersSize; loop++ ) {
					String follower = followers.get( loop );
					int currentIndex = getIndexOfFirstMatchingProperty( propertyNames, follower );
					index = currentIndex != -1 && currentIndex < index ? currentIndex : index;
				}
				propertyNames.add( index, name );
				results.add( index, new FieldResult( name, quotingNormalizedColumnName ) );
			}
			else {
				propertyNames.add( name );
				results.add( new FieldResult( name, quotingNormalizedColumnName ) );
			}

		}
		return results;
	}

	private static int getIndexOfFirstMatchingProperty(List<String> propertyNames, String follower) {
		int propertySize = propertyNames.size();
		for ( int propIndex = 0; propIndex < propertySize; propIndex++ ) {
			if ( ( propertyNames.get( propIndex ) ).startsWith( follower ) ) {
				return propIndex;
			}
		}
		return -1;
	}

	private static class FieldResult {
		String name;
		String column;

		private FieldResult(String column, String name) {
			this.column = column;
			this.name = name;
		}
	}

	private static List<String> getFollowers(Iterable<? extends AttributeBinding> attributeBindings, String reducedName, String name) {
		boolean hasFollowers = false;
		List<String> followers = new ArrayList<String>();
		for ( final AttributeBinding attributeBinding : attributeBindings ) {
			String currentPropertyName = attributeBinding.getAttribute().getName();
			String currentName = reducedName + '.' + currentPropertyName;
			if ( hasFollowers ) {
				followers.add( currentName );
			}
			if ( name.equals( currentName ) ) {
				hasFollowers = true;
			}
		}
		return followers;
	}


	private static void insert(String key, String value, Map<String, String[]> map) {
		if ( map.containsKey( key ) ) {
			String[] oldValues = map.get( key );
			String[] values = Arrays.copyOf( oldValues, oldValues.length + 1 );
			values[oldValues.length] = value;
			map.put( key, values );
		}
		else {
			map.put( key, new String[] { value } );
		}
	}

	private static void bindColumnResult(
			AnnotationInstance columnResult,
			ResultSetMappingDefinition definition,
			AnnotationBindingContext bindingContext) {
		definition.addQueryReturn( extractColumnResult( columnResult, bindingContext ) );
	}

	private static NativeSQLQueryScalarReturn extractColumnResult(
			AnnotationInstance columnResult,
			AnnotationBindingContext bindingContext) {
		// `name` is required
		final String name = columnResult.value( "name" ).asString();
		final String normalizedName = normalize( name, bindingContext );
		//todo TYPE
		return new NativeSQLQueryScalarReturn( normalizedName, null );
	}

	private static void bindConstructorResult(
			AnnotationInstance constructorResult,
			ResultSetMappingDefinition definition,
			AnnotationBindingContext bindingContext) {
		final Class classToConstruct = bindingContext.getServiceRegistry()
				.getService( ClassLoaderService.class )
				.classForName( constructorResult.value( "targetClass" ).asString() );

		final List<NativeSQLQueryScalarReturn> columns = new ArrayList<NativeSQLQueryScalarReturn>();
		final AnnotationInstance[] columnResults = JandexHelper.extractAnnotationsValue(
				constructorResult,
				"columns"
		);
		if ( columnResults != null && columnResults.length > 0 ) {
			for ( AnnotationInstance columnResult : columnResults ) {
				columns.add( extractColumnResult( columnResult, bindingContext ) );
			}
		}

		definition.addQueryReturn(
				new NativeSQLQueryConstructorReturn( classToConstruct, columns )
		);
	}
}
