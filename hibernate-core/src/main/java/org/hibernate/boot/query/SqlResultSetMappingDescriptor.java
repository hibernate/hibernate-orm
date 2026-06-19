/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.query.internal.FetchMementoBasicStandard;
import org.hibernate.query.internal.FetchMementoEntityStandard;
import org.hibernate.query.internal.NamedResultSetMappingMementoImpl;
import org.hibernate.query.internal.ResultMementoBasicStandard;
import org.hibernate.query.internal.ResultMementoEntityJpa;
import org.hibernate.query.internal.ResultMementoInstantiationStandard;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.spi.FetchMemento;
import org.hibernate.query.named.spi.FetchMementoBasic;
import org.hibernate.query.named.spi.NamedResultSetMappingMemento;
import org.hibernate.query.named.spi.ResultMemento;
import org.hibernate.query.named.spi.ResultMementoInstantiation.ArgumentMemento;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;

import jakarta.annotation.Nullable;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;

import static org.hibernate.boot.query.BootQueryLogging.BOOT_QUERY_LOGGER;
import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.internal.util.collections.ArrayHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;
import static org.hibernate.metamodel.mapping.EntityIdentifierMapping.ID_ROLE_NAME;

/**
 * @author Steve Ebersole
 */
public class SqlResultSetMappingDescriptor implements NamedResultSetMappingDescriptor {

	// todo (6.0) : we can probably reuse the NamedResultSetMappingDefinition
	//  		implementation between HBM and annotation handling.  We'd
	// 			just need different "builders" for each source and handle the
	//			variances in those builders.  But once we have a
	//			NamedResultSetMappingDefinition and all of its sub-parts,
	//			resolving to a memento is the same
	// 			-
	//			additionally, consider having the sub-parts (the return
	//			representations) be what is used and handed to the
	//			NamedResultSetMappingMemento directly.  They simply need
	//			to be capable of resolving themselves into ResultBuilders
	//			(`org.hibernate.query.results.spi.ResultBuilder`) as part of the
	//			memento for its resolution

	public static SqlResultSetMappingDescriptor from(SqlResultSetMapping mappingAnnotation, String name) {
		return from( mappingAnnotation, name, null );
	}

	public static SqlResultSetMappingDescriptor from(
			SqlResultSetMapping mappingAnnotation,
			String name,
			@Nullable String location) {
		return from(
				name,
				mappingAnnotation.entities(),
				mappingAnnotation.classes(),
				mappingAnnotation.columns(),
				location
		);
	}

	public static SqlResultSetMappingDescriptor from(NamedNativeQuery namedNativeQuery) {
		return from( namedNativeQuery, null );
	}

	public static SqlResultSetMappingDescriptor from(NamedNativeQuery namedNativeQuery, @Nullable String location) {
		return from(
				namedNativeQuery.name(),
				namedNativeQuery.entities(),
				namedNativeQuery.classes(),
				namedNativeQuery.columns(),
				location
		);
	}

	public static SqlResultSetMappingDescriptor from(
			String name,
			EntityResult[] entityResults,
			ConstructorResult[] constructorResults,
			ColumnResult[] columnResults) {
		return from( name, entityResults, constructorResults, columnResults, null );
	}

	public static SqlResultSetMappingDescriptor from(
			String name,
			EntityResult[] entityResults,
			ConstructorResult[] constructorResults,
			ColumnResult[] columnResults,
			@Nullable String location) {
		final List<ResultDescriptor> resultDescriptors = arrayList(
				entityResults.length + constructorResults.length + columnResults.length
		);

		for ( final var entityResult : entityResults ) {
			resultDescriptors.add( new EntityResultDescriptor( entityResult ) );
		}

		for ( final var constructorResult : constructorResults ) {
			resultDescriptors.add( new ConstructorResultDescriptor( constructorResult, name ) );
		}

		for ( final var columnResult : columnResults ) {
			resultDescriptors.add(
					new JpaColumnResultDescriptor( columnResult, name )
			);
		}

		return new SqlResultSetMappingDescriptor( name, location, resultDescriptors );
	}

	public static SqlResultSetMappingDescriptor from(SqlResultSetMapping mappingAnnotation) {
		return from( mappingAnnotation, mappingAnnotation.name() );
	}

	private final String mappingName;
	private final @Nullable String location;
	private final List<ResultDescriptor> resultDescriptors;

	private SqlResultSetMappingDescriptor(
			String mappingName,
			@Nullable String location,
			List<ResultDescriptor> resultDescriptors) {
		this.mappingName = mappingName;
		this.location = location;
		this.resultDescriptors = resultDescriptors;
	}

	@Nonnull
	@Override
	public String getRegistrationName() {
		return mappingName;
	}

	@Override
	public @Nullable String getLocation() {
		return location;
	}

	@Nonnull
	@Override
	public NamedResultSetMappingMemento resolve(@Nonnull ResultSetMappingResolutionContext resolutionContext) {
		final List<ResultMemento> resultMementos = arrayList( resultDescriptors.size() );

		resultDescriptors.forEach(
				resultDescriptor -> resultMementos.add( resultDescriptor.resolve( resolutionContext ) )
		);

		return new NamedResultSetMappingMementoImpl( mappingName, resultMementos );
	}


	/**
	 * @see jakarta.persistence.ColumnResult
	 */
	private static class JpaColumnResultDescriptor implements ResultDescriptor {
		private final ColumnResult columnResult;
		private final String mappingName;

		public JpaColumnResultDescriptor(
				ColumnResult columnResult,
				String mappingName) {
			this.columnResult = columnResult;
			this.mappingName = mappingName;
		}

		@Nonnull
		@Override
		public ResultMemento resolve(@Nonnull ResultSetMappingResolutionContext resolutionContext) {
			BOOT_QUERY_LOGGER.tracef(
					"Generating ScalarResultMappingMemento for JPA ColumnResult(%s) for ResultSet mapping `%s`",
					columnResult.name(),
					mappingName
			);

			return new ResultMementoBasicStandard( columnResult, resolutionContext );
		}
	}

	/**
	 * @see jakarta.persistence.ConstructorResult
	 */
	private static class ConstructorResultDescriptor implements ResultDescriptor {
		private static class ArgumentDescriptor {
			private final JpaColumnResultDescriptor argumentResultDescriptor;

			private ArgumentDescriptor(JpaColumnResultDescriptor argumentResultDescriptor) {
				this.argumentResultDescriptor = argumentResultDescriptor;
			}

			ArgumentMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
				return new ArgumentMemento( argumentResultDescriptor.resolve( resolutionContext ) );
			}
		}

		private final String mappingName;

		private final Class<?> targetJavaType;
		private final List<ArgumentDescriptor> argumentResultDescriptors;

		public ConstructorResultDescriptor(
				ConstructorResult constructorResult,
				String mappingName) {
			this.mappingName = mappingName;
			targetJavaType = constructorResult.targetClass();
			argumentResultDescriptors = interpretArguments( constructorResult, mappingName );
		}

		private static List<ArgumentDescriptor> interpretArguments(
				ConstructorResult constructorResult,
				String mappingName) {
			final var columnResults = constructorResult.columns();
			if ( isEmpty( columnResults ) ) {
				throw new IllegalArgumentException( "ConstructorResult did not define any ColumnResults" );
			}

			final List<ArgumentDescriptor> argumentResultDescriptors = arrayList( columnResults.length );
			for ( var columnResult : columnResults ) {
				final var argumentResultDescriptor =
						new JpaColumnResultDescriptor( columnResult, mappingName );
				argumentResultDescriptors.add( new ArgumentDescriptor(argumentResultDescriptor) );
			}
			return argumentResultDescriptors;
		}

		@Nonnull
		@Override
		public ResultMemento resolve(@Nonnull ResultSetMappingResolutionContext resolutionContext) {
			BOOT_QUERY_LOGGER.tracef(
					"Generating InstantiationResultMappingMemento for JPA ConstructorResult(%s) for ResultSet mapping `%s`",
					targetJavaType.getName(),
					mappingName
			);

			final List<ArgumentMemento> argumentResultMementos = new ArrayList<>( argumentResultDescriptors.size() );
			argumentResultDescriptors.forEach(
					(mapping) -> argumentResultMementos.add( mapping.resolve( resolutionContext ) )
			);
			final var targetJtd =
					resolutionContext.getTypeConfiguration().getJavaTypeRegistry()
							.resolveDescriptor( targetJavaType );
			return new ResultMementoInstantiationStandard( targetJtd, argumentResultMementos );
		}
	}

	/**
	 * @see jakarta.persistence.EntityResult
	 */
	public static class EntityResultDescriptor implements ResultDescriptor {
		private final NavigablePath navigablePath;
		private final String entityName;
		private final String discriminatorColumn;
		private final LockModeType lockMode;

		private final Map<String, AttributeFetchDescriptor> explicitFetchMappings;

		public EntityResultDescriptor(EntityResult entityResult) {
			entityName = entityResult.entityClass().getName();
			navigablePath = new NavigablePath( entityName );
			discriminatorColumn = entityResult.discriminatorColumn();
			lockMode = entityResult.lockMode();
			explicitFetchMappings = extractFetchMappings( navigablePath, entityResult );
		}

		private static Map<String, AttributeFetchDescriptor> extractFetchMappings(
				NavigablePath navigablePath,
				EntityResult entityResult) {
			final var fields = entityResult.fields();
			final Map<String, AttributeFetchDescriptor> explicitFetchMappings = mapOfSize( fields.length );
			for ( var fieldResult : fields ) {
				final String fieldName = fieldResult.name();
				final var existing = explicitFetchMappings.get( fieldName );
				if ( existing != null ) {
					existing.addColumn( fieldResult );
				}
				else {
					explicitFetchMappings.put( fieldName,
							AttributeFetchDescriptor.from( navigablePath, navigablePath.getFullPath(), fieldResult ) );
				}
			}
			return explicitFetchMappings;
		}

		@Nonnull
		@Override
		public ResultMemento resolve(@Nonnull ResultSetMappingResolutionContext resolutionContext) {
			final var entityDescriptor =
					resolutionContext.getMappingMetamodel()
							.getEntityDescriptor( entityName );

			final var discriminatorMemento = resolveDiscriminatorMemento(
					entityDescriptor,
					discriminatorColumn,
					navigablePath
			);

			final Map<String, FetchMemento> fetchMementos = new HashMap<>();
			explicitFetchMappings.forEach(
					(relativePath, fetchDescriptor) -> fetchMementos.put(
							relativePath,
							fetchDescriptor.resolve( resolutionContext )
					)
			);

			return new ResultMementoEntityJpa(
					entityDescriptor,
					lockMode == LockModeType.OPTIMISTIC
							? LockMode.NONE
							: LockMode.fromJpaLockMode( lockMode ),
					discriminatorMemento,
					fetchMementos
			);
		}

		private static FetchMementoBasic resolveDiscriminatorMemento(
				EntityMappingType entityMapping,
				String discriminatorColumn,
				NavigablePath entityPath) {
			final var discriminatorMapping = entityMapping.getDiscriminatorMapping();
			if ( discriminatorMapping == null || discriminatorColumn == null || !entityMapping.hasSubclasses() ) {
				return null;
			}
			else {
				return new FetchMementoBasicStandard(
						entityPath.append( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME ),
						discriminatorMapping,
						discriminatorColumn
				);
			}
		}
	}

	private static class AttributeFetchDescriptor implements FetchDescriptor {

		private static AttributeFetchDescriptor from(
				NavigablePath entityPath,
				String entityName,
				FieldResult fieldResult) {
			return new AttributeFetchDescriptor(
					entityPath,
					entityName,
					fieldResult.name(),
					fieldResult.column()
			);
		}

		private final NavigablePath navigablePath;

		private final String entityName;
		private final String propertyPath;
		private final String[] propertyPathParts;
		private final List<String> columnNames;

		private AttributeFetchDescriptor(
				NavigablePath entityPath,
				String entityName,
				String propertyPath,
				String columnName) {
			this.entityName = entityName;
			this.propertyPath = propertyPath;
			propertyPathParts = split( ".", propertyPath );
			navigablePath = entityPath;
			columnNames = new ArrayList<>();
			columnNames.add( columnName );
		}

		private void addColumn(FieldResult fieldResult) {
			if ( ! propertyPath.equals( fieldResult.name() ) ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Passed FieldResult [%s, %s] does not match AttributeFetchMapping [%s]",
								fieldResult.name(),
								fieldResult.column(),
								propertyPath
						)
				);
			}

			columnNames.add( fieldResult.column() );
		}

//		@Override
//		public ResultMemento asResultMemento(NavigablePath path, ResultSetMappingResolutionContext resolutionContext) {
//			final EntityMappingType entityMapping =
//					resolutionContext.getMappingMetamodel().getEntityDescriptor( entityName );
//
//			final ModelPart subPart = entityMapping.findSubPart( propertyPath, null );
//
//			final BasicValuedModelPart basicPart = subPart != null ? subPart.asBasicValuedModelPart() : null;
//			if ( basicPart != null ) {
//				assert columnNames.size() == 1;
//
//				return new ModelPartResultMementoBasicImpl( path, basicPart, columnNames.get( 0 ) );
//			}
//
//			throw new UnsupportedOperationException(
//					"Only support for basic-valued model-parts have been implemented : " + propertyPath
//					+ " [" + subPart + "]"
//			);
//		}

		@Nonnull
		@Override
		public FetchMemento resolve(@Nonnull ResultSetMappingResolutionContext resolutionContext) {
			final var entityMapping =
					resolutionContext.getMappingMetamodel()
							.getEntityDescriptor( entityName );

			ModelPart subPart = entityMapping.findSubPart(
					propertyPathParts[0],
					null
			);
			final NavigablePath parentNavigablePath;
			if ( !subPart.getNavigableRole().getParent().equals( entityMapping.getNavigableRole() )
					&& subPart.getNavigableRole().getParent().getLocalName().equals( ID_ROLE_NAME ) ) {
				// The attribute is defined in an ID class, append {id} to navigable path
				parentNavigablePath = new EntityIdentifierNavigablePath( this.navigablePath, null );
			}
			else {
				parentNavigablePath = this.navigablePath;
			}

			NavigablePath navigablePath = subPart.isEntityIdentifierMapping() ?
					new EntityIdentifierNavigablePath( parentNavigablePath, propertyPathParts[0] ) :
					parentNavigablePath.append( propertyPathParts[0] );
			for ( int i = 1; i < propertyPathParts.length; i++ ) {
				if ( !( subPart instanceof ModelPartContainer ) ) {
					throw new MappingException(
							String.format(
									Locale.ROOT,
									"Non-terminal property path did not reference FetchableContainer - %s ",
									navigablePath
							)
					);
				}
				navigablePath = navigablePath.append( propertyPathParts[i] );
				subPart = ( (ModelPartContainer) subPart ).findSubPart( propertyPathParts[i], null );
			}

			return getFetchMemento( navigablePath, subPart );
		}

		@Nonnull
		private FetchMemento getFetchMemento(NavigablePath navigablePath, ModelPart subPart) {
			final var basicPart = subPart.asBasicValuedModelPart();
			if ( basicPart != null ) {
				assert columnNames.size() == 1;
				return new FetchMementoBasicStandard( navigablePath, basicPart, columnNames.get( 0 ) );
			}
			else if ( subPart instanceof EntityValuedFetchable entityValuedFetchable ) {
				return new FetchMementoEntityStandard( navigablePath, entityValuedFetchable, columnNames );
			}
			else if( subPart instanceof EmbeddedAttributeMapping embeddedAttributeMapping ){
				final ModelPart subPart1 = embeddedAttributeMapping.findSubPart( propertyPath.substring(
						propertyPath.indexOf( '.' ) + 1), null );
				return getFetchMemento( navigablePath,subPart1 );
			}
			else {
				throw new UnsupportedOperationException(
						"Only support for basic-valued, entity-valued and embedded model-parts have been implemented : " + propertyPath
						+ " [" + subPart + "]"
				);
			}
		}
	}
}
