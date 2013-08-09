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
package org.hibernate.metamodel.internal.source.annotations.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularNonAssociationAttributeBinding;

/**
 * Binds <ul>
 * <li>{@link javax.persistence.SqlResultSetMapping}</li>
 * <li>{@link javax.persistence.SqlResultSetMappings}</li>
 * <li>{@link javax.persistence.EntityResult}</li>
 * <li>{@link javax.persistence.FieldResult}</li>
 * <li>{@link javax.persistence.ColumnResult}</li>
 * </ul>
 *
 * @author Strong Liu <stliu@hibernate.org>
 */
public class SqlResultSetProcessor {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			QueryProcessor.class.getName()
	);

	private SqlResultSetProcessor() {
	}

	public static void bind(final AnnotationBindingContext bindingContext) {
		Collection<AnnotationInstance> annotations = bindingContext.getIndex()
				.getAnnotations( JPADotNames.SQL_RESULT_SET_MAPPING );
		for ( final AnnotationInstance sqlResultSetMappingAnnotationInstance : annotations ) {
			bindSqlResultSetMapping( bindingContext, sqlResultSetMappingAnnotationInstance );
		}

		annotations = bindingContext.getIndex().getAnnotations( JPADotNames.SQL_RESULT_SET_MAPPINGS );
		for ( final AnnotationInstance sqlResultSetMappingsAnnotationInstance : annotations ) {
			for ( AnnotationInstance annotationInstance : JandexHelper.getValue(
					sqlResultSetMappingsAnnotationInstance,
					"value",
					AnnotationInstance[].class,
					bindingContext.getServiceRegistry().getService( ClassLoaderService.class )
			) ) {
				bindSqlResultSetMapping( bindingContext, annotationInstance );
			}
		}
	}

	private static int entityAliasIndex = 0;

	private static void bindSqlResultSetMapping(final AnnotationBindingContext bindingContext, final AnnotationInstance annotation) {
		final ClassLoaderService classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );
		entityAliasIndex = 0;
		final String name = JandexHelper.getValue( annotation, "name", String.class, classLoaderService );
		LOG.debugf( "Binding @SqlResultSetMapping(name=%s)", name );
		final ResultSetMappingDefinition definition = new ResultSetMappingDefinition( name );
		for ( final AnnotationInstance entityResult : JandexHelper.getValue(
				annotation,
				"entities",
				AnnotationInstance[].class,
				classLoaderService
		) ) {
			bindEntityResult( bindingContext, entityResult, definition );
		}
		for ( final AnnotationInstance columnResult : JandexHelper.getValue(
				annotation,
				"columns",
				AnnotationInstance[].class,
				classLoaderService
		) ) {
			bindColumnResult( bindingContext, columnResult, definition );
		}

		bindingContext.getMetadataImplementor().addResultSetMapping( definition );
	}

	private static void bindEntityResult(final AnnotationBindingContext bindingContext,
										 final AnnotationInstance entityResult,
										 final ResultSetMappingDefinition definition) {
		final ClassLoaderService classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );
		final String className = JandexHelper.getValue( entityResult, "entityClass", String.class, classLoaderService );
		final EntityBinding targetEntityBinding = bindingContext.getMetadataImplementor().getEntityBinding( className );
		if ( targetEntityBinding == null ) {
			throw new MappingException(
					String.format(
							"Entity[%s] not found in SqlResultMapping[%s]",
							className,
							definition.getName()
					)
			);
		}

		final String discriminatorColumn = JandexHelper.getValue( entityResult, "discriminatorColumn", String.class,
				classLoaderService );

		final Map<String, String[]> propertyResults = new HashMap<String, String[]>();


		if ( StringHelper.isNotEmpty( discriminatorColumn ) ) {
			final String quotingNormalizedName = bindingContext.getMetadataImplementor()
					.getObjectNameNormalizer()
					.normalizeIdentifierQuoting(
							discriminatorColumn
					);
			propertyResults.put( "class", new String[] { quotingNormalizedName } );
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
				targetEntityBinding.getEntity().getName(),
				propertyResults,
				LockMode.READ
		);
		definition.addQueryReturn( result );
	}

	//todo see org.hibernate.cfg.annotations.ResultsetMappingSecondPass#getSubPropertyIterator
	private static List<FieldResult> reorderFieldResult(AnnotationBindingContext bindingContext,
														AnnotationInstance entityResult,
														EntityBinding entityBinding,
														String resultSetMappingDefinitionName) {
		final ClassLoaderService classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );
		final AnnotationInstance[] fieldResultAnnotationInstances = JandexHelper.getValue(
				entityResult,
				"fields",
				AnnotationInstance[].class,
				classLoaderService
		);
		List<FieldResult> results = new ArrayList<FieldResult>( fieldResultAnnotationInstances.length );
		List<String> propertyNames = new ArrayList<String>();
		final Set<String> uniqueReturnProperty = new HashSet<String>();
		for ( final AnnotationInstance fieldResult : fieldResultAnnotationInstances ) {
			final String name = JandexHelper.getValue( fieldResult, "name", String.class, classLoaderService );
			if ( !uniqueReturnProperty.add( name ) ) {
				throw new MappingException(
						"duplicate @FieldResult for property " + name +
								" on @Entity " + entityBinding.getEntity()
								.getName() + " in " + resultSetMappingDefinitionName
				);
			}
			if ( "class".equals( name ) ) {
				throw new MappingException(
						"class is not a valid property name to use in a @FieldResult, use @EntityResult(discriminatorColumn) instead"
				);
			}
			final String column = JandexHelper.getValue( fieldResult, "column", String.class,
					classLoaderService );
			final String quotingNormalizedColumnName = normalize( bindingContext, column );
			if ( name.contains( "." ) ) {
				int dotIndex = name.lastIndexOf( '.' );
				String reducedName = name.substring( 0, dotIndex );
				AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( reducedName );
				Iterable<? extends AttributeBinding> attributeBindings = null;
				if ( CompositeAttributeBinding.class.isInstance( attributeBinding ) ) {
					CompositeAttributeBinding compositeAttributeBinding = CompositeAttributeBinding.class.cast(
							attributeBinding
					);
					attributeBindings = compositeAttributeBinding.attributeBindings();

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
						SingularNonAssociationAttributeBinding identifierAttributeBinding = referencedEntityBinding.getHierarchyDetails()
								.getEntityIdentifier()
								.getAttributeBinding();
						if ( CompositeAttributeBinding.class.isInstance( identifierAttributeBinding ) ) {
							attributeBindings = CompositeAttributeBinding.class.cast( identifierAttributeBinding )
									.attributeBindings();
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

	private static void bindColumnResult(final AnnotationBindingContext bindingContext,
										 final AnnotationInstance columnResult,
										 final ResultSetMappingDefinition definition) {
		final String name = JandexHelper.getValue( columnResult, "name", String.class,
				bindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );
		final String normalizedName = normalize( bindingContext, name );
		//todo TYPE
		definition.addQueryReturn( new NativeSQLQueryScalarReturn( normalizedName, null ) );
	}

	private static String normalize(final AnnotationBindingContext bindingContext, String name) {
		return bindingContext.getMetadataImplementor()
				.getObjectNameNormalizer()
				.normalizeIdentifierQuoting( name );
	}
}
