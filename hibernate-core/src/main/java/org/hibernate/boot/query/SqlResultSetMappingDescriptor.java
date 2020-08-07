/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.SqlResultSetMapping;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.BootLogging;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.internal.BasicFetchMemento;
import org.hibernate.query.internal.EntityResultMappingMemento;
import org.hibernate.query.internal.FetchMappingMemento;
import org.hibernate.query.internal.InstantiationResultMappingMemento;
import org.hibernate.query.internal.InstantiationResultMappingMemento.ArgumentMemento;
import org.hibernate.query.internal.NamedResultSetMappingMementoImpl;
import org.hibernate.query.internal.ResultMappingMemento;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.internal.ScalarResultMappingMemento;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

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
	//			(`org.hibernate.query.results.ResultBuilder`) as part of the
	//			memento for its resolution

	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static SqlResultSetMappingDescriptor from(
			SqlResultSetMapping mappingAnnotation,
			MetadataBuildingContext context) {

		final EntityResult[] entityResults = mappingAnnotation.entities();
		final ConstructorResult[] constructorResults = mappingAnnotation.classes();
		final ColumnResult[] columnResults = mappingAnnotation.columns();

		final List<ResultDescriptor> resultDescriptors = CollectionHelper.arrayList(
				entityResults.length + columnResults.length + columnResults.length
		);

		for ( int i = 0; i < entityResults.length; i++ ) {
			final EntityResult entityResult = entityResults[i];
			resultDescriptors.add(
					EntityResultDescriptor.from( entityResult, context )
			);
		}

		for ( int i = 0; i < constructorResults.length; i++ ) {
			final ConstructorResult constructorResult = constructorResults[i];
			resultDescriptors.add(
					new ConstructorResultDescriptor( constructorResult, mappingAnnotation )
			);
		}

		for ( int i = 0; i < columnResults.length; i++ ) {
			final ColumnResult columnResult = columnResults[i];
			resultDescriptors.add(
					new JpaColumnResultDescriptor( columnResult, mappingAnnotation )
			);
		}

		return new SqlResultSetMappingDescriptor(
				mappingAnnotation.name(),
				resultDescriptors,
				context
		);
	}

	private final String mappingName;
	private final List<ResultDescriptor> resultDescriptors;

	private SqlResultSetMappingDescriptor(
			String mappingName,
			List<ResultDescriptor> resultDescriptors,
			MetadataBuildingContext context) {
		this.mappingName = mappingName;
		this.resultDescriptors = resultDescriptors;
	}

	@Override
	public String getRegistrationName() {
		return mappingName;
	}

	@Override
	public NamedResultSetMappingMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
		final List<ResultMappingMemento> resultMementos = CollectionHelper.arrayList( resultDescriptors.size() );

		resultDescriptors.forEach(
				resultDescriptor -> resultMementos.add( resultDescriptor.resolve( resolutionContext ) )
		);

		return new NamedResultSetMappingMementoImpl( mappingName, resultMementos );
	}


	/**
	 * @see javax.persistence.ColumnResult
	 */
	private static class JpaColumnResultDescriptor implements ResultDescriptor {
		private final ColumnResult columnResult;
		private final String mappingName;

		public JpaColumnResultDescriptor(
				ColumnResult columnResult,
				SqlResultSetMapping mappingAnnotation) {
			this.columnResult = columnResult;
			this.mappingName = mappingAnnotation.name();
		}

		@Override
		public ResultMappingMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
			BootLogging.LOGGER.debugf(
					"Generating ScalarResultMappingMemento for JPA ColumnResult(%s) for ResultSet mapping `%s`",
					columnResult.name(),
					mappingName
			);

			return new ScalarResultMappingMemento( columnResult, resolutionContext );
		}
	}

	/**
	 * @see javax.persistence.ConstructorResult
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
				SqlResultSetMapping mappingAnnotation) {
			this.mappingName = mappingAnnotation.name();
			this.targetJavaType = constructorResult.targetClass();

			final ColumnResult[] columnResults = constructorResult.columns();
			if ( columnResults.length == 0 ) {
				throw new IllegalArgumentException( "ConstructorResult did not define any ColumnResults" );
			}

			this.argumentResultDescriptors = CollectionHelper.arrayList( columnResults.length );
			for ( int i = 0; i < columnResults.length; i++ ) {
				final ColumnResult columnResult = columnResults[i];
				final JpaColumnResultDescriptor argumentResultDescriptor = new JpaColumnResultDescriptor(
						columnResult,
						mappingAnnotation
				);
				argumentResultDescriptors.add( new ArgumentDescriptor( argumentResultDescriptor ) );
			}
		}

		@Override
		public ResultMappingMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
			BootLogging.LOGGER.debugf(
					"Generating InstantiationResultMappingMemento for JPA ConstructorResult(%s) for ResultSet mapping `%s`",
					targetJavaType.getName(),
					mappingName
			);

			final List<ArgumentMemento> argumentResultMementos = new ArrayList<>( argumentResultDescriptors.size() );

			argumentResultDescriptors.forEach(
					(mapping) -> argumentResultMementos.add( mapping.resolve( resolutionContext ) )
			);

			final SessionFactoryImplementor sessionFactory = resolutionContext.getSessionFactory();
			final JavaTypeDescriptor<?> targetJtd = sessionFactory.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( targetJavaType );

			return new InstantiationResultMappingMemento( targetJtd, argumentResultMementos );
		}
	}

	/**
	 * @see javax.persistence.EntityResult
	 */
	public static class EntityResultDescriptor implements ResultDescriptor {
		private final NavigablePath navigablePath;

		private final String entityName;
		private final String discriminatorColumn;

		private final Map<String, AttributeFetchDescriptor> fetchMappings;

		public static EntityResultDescriptor from(EntityResult entityResult, MetadataBuildingContext context) {
			final String entityName = entityResult.entityClass().getName();

			final Map<String, AttributeFetchDescriptor> fetchMappings = new HashMap<>();
			for ( int i = 0; i < entityResult.fields().length; i++ ) {
				final FieldResult fieldResult = entityResult.fields()[ i ];
				final AttributeFetchDescriptor existing = fetchMappings.get( fieldResult.name() );
				if ( existing != null ) {
					existing.addColumn( fieldResult );
				}
				else {
					fetchMappings.put(
							fieldResult.name(),
							AttributeFetchDescriptor.from( entityName, fieldResult, context )
					);
				}
			}

			return new EntityResultDescriptor(
					entityName,
					entityResult.discriminatorColumn(),
					fetchMappings
			);
		}

		public EntityResultDescriptor(
				String entityName,
				String discriminatorColumn,
				Map<String, AttributeFetchDescriptor> fetchMappings) {
			this.navigablePath = new NavigablePath( entityName );

			this.entityName = entityName;
			this.discriminatorColumn = discriminatorColumn;

			this.fetchMappings = fetchMappings;
		}

		@Override
		public ResultMappingMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
			final RuntimeMetamodels runtimeMetamodels = resolutionContext.getSessionFactory().getRuntimeMetamodels();
			final EntityMappingType entityDescriptor = runtimeMetamodels.getEntityMappingType( entityName );

			final Map<String, FetchMappingMemento> fetchMementos = new HashMap<>();
			fetchMappings.forEach(
					(attrName, attrMapping) -> fetchMementos.put( attrName, attrMapping.resolve( resolutionContext ) )
			);

			return new EntityResultMappingMemento( entityDescriptor, discriminatorColumn, fetchMementos );
		}
	}

	private static class AttributeFetchDescriptor implements FetchDescriptor {

		private static AttributeFetchDescriptor from(
				String entityName,
				FieldResult fieldResult,
				MetadataBuildingContext context) {
			return new AttributeFetchDescriptor(
					entityName,
					fieldResult.name(),
					fieldResult.column()
			);
		}

		private final String entityName;
		private final String attributeName;
		private final List<String> columnNames;

		private AttributeFetchDescriptor(String entityName, String attributeName, String columnName) {
			this.entityName = entityName;
			this.attributeName = attributeName;
			this.columnNames = new ArrayList<>();
			columnNames.add( columnName );
		}

		private void addColumn(FieldResult fieldResult) {
			if ( ! attributeName.equals( fieldResult.name() ) ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Passed FieldResult [%s, %s] does not match AttributeFetchMapping [%s]",
								fieldResult.name(),
								fieldResult.column(),
								attributeName
						)
				);
			}

			columnNames.add( fieldResult.column() );
		}

		@Override
		public FetchMappingMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
			final RuntimeMetamodels runtimeMetamodels = resolutionContext.getSessionFactory().getRuntimeMetamodels();
			final EntityMappingType entityMapping = runtimeMetamodels.getEntityMappingType( entityName );

			final ModelPart subPart = entityMapping.findSubPart( attributeName, null );

			if ( subPart == null ) {
				// throw an exception
			}

			if ( subPart instanceof BasicValuedModelPart ) {
				assert columnNames.size() == 1;
				final BasicValuedModelPart basicPart = (BasicValuedModelPart) subPart;

				return new BasicFetchMemento( basicPart, columnNames.get( 0 ) );
			}

			throw new NotYetImplementedFor6Exception(
					"Only support for basic-valued model-parts have been implemented : " + attributeName + " [" + subPart + "]"
			);
		}
	}
}
