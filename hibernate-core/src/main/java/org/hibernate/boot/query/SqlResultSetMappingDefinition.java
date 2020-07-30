/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.EntityResult;
import javax.persistence.SqlResultSetMapping;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.BootLogging;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.NamedResultSetMappingMementoImpl;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.EntityResultBuilder;
import org.hibernate.query.results.InstantiationResultBuilder;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ScalarResultBuilder;
import org.hibernate.query.results.StandardInstantiationResultBuilder;
import org.hibernate.query.results.StandardScalarResultBuilder;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqlResultSetMappingDefinition implements NamedResultSetMappingDefinition {

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

	public static SqlResultSetMappingDefinition from(
			SqlResultSetMapping mappingAnnotation,
			MetadataBuildingContext context) {

		final List<EntityResultMapping> entityResultMappings;
		final List<ConstructorResultMapping> constructorResultMappings;
		final List<JpaColumnResultMapping> columnResultMappings;

		final EntityResult[] entityResults = mappingAnnotation.entities();
		if ( entityResults.length > 0 ) {
			entityResultMappings = Collections.emptyList();
		}
		else {
			entityResultMappings = new ArrayList<>( entityResults.length );
			for ( int i = 0; i < entityResults.length; i++ ) {
				final EntityResult entityResult = entityResults[i];
				entityResultMappings.add( EntityResultMapping.from( entityResult, context ) );
			}
		}

		final ConstructorResult[] constructorResults = mappingAnnotation.classes();
		if ( constructorResults.length == 0 ) {
			constructorResultMappings = Collections.emptyList();
		}
		else {
			constructorResultMappings = new ArrayList<>( constructorResults.length );
			for ( int i = 0; i < constructorResults.length; i++ ) {
				final ConstructorResult constructorResult = constructorResults[i];
				constructorResultMappings.add( ConstructorResultMapping.from( constructorResult, context ) );
			}
		}

		final ColumnResult[] columnResults = mappingAnnotation.columns();
		if ( columnResults.length == 0 ) {
			columnResultMappings = Collections.emptyList();
		}
		else {
			columnResultMappings = new ArrayList<>( columnResults.length );
			for ( int i = 0; i < columnResults.length; i++ ) {
				final ColumnResult columnResult = columnResults[i];
				columnResultMappings.add( JpaColumnResultMapping.from( columnResult, context ) );
			}
		}

		return new SqlResultSetMappingDefinition(
				mappingAnnotation.name(),
				entityResultMappings,
				constructorResultMappings,
				columnResultMappings,
				context
		);
	}

	private final String mappingName;

	private final List<EntityResultMapping> entityResultMappings;
	private final List<ConstructorResultMapping> constructorResultMappings;
	private final List<JpaColumnResultMapping> columnResultMappings;

	private SqlResultSetMappingDefinition(
			String mappingName,
			List<EntityResultMapping> entityResultMappings,
			List<ConstructorResultMapping> constructorResultMappings,
			List<JpaColumnResultMapping> columnResultMappings,
			MetadataBuildingContext context) {
		this.mappingName = mappingName;
		this.entityResultMappings = entityResultMappings;
		this.constructorResultMappings = constructorResultMappings;
		this.columnResultMappings = columnResultMappings;
	}

	@Override
	public String getRegistrationName() {
		return mappingName;
	}

	@Override
	public NamedResultSetMappingMemento resolve(SessionFactoryImplementor factory) {
		final List<EntityResultBuilder> entityResultBuilders = new ArrayList<>();
		for ( int i = 0; i < entityResultMappings.size(); i++ ) {
			final EntityResultMapping resultMapping = entityResultMappings.get( i );
			entityResultBuilders.add( resultMapping.resolve( factory ) );
		}

		final List<InstantiationResultBuilder> instantiationResultBuilders = new ArrayList<>();
		for ( int i = 0; i < constructorResultMappings.size(); i++ ) {
			final ConstructorResultMapping resultMapping = constructorResultMappings.get( i );
			instantiationResultBuilders.add( resultMapping.resolve( factory ) );
		}

		final List<ScalarResultBuilder> scalarResultBuilders = new ArrayList<>();
		for ( int i = 0; i < columnResultMappings.size(); i++ ) {
			final JpaColumnResultMapping resultMapping = columnResultMappings.get( i );
			scalarResultBuilders.add( resultMapping.resolve( factory ) );
		}

		return new NamedResultSetMappingMementoImpl(
				mappingName,
				entityResultBuilders,
				instantiationResultBuilders,
				scalarResultBuilders,
				factory
		);
	}


	/**
	 * @see javax.persistence.ColumnResult
	 */
	private static class JpaColumnResultMapping implements ResultMapping {
		private final String columnName;
		private final Class<?> explicitJavaType;

		public JpaColumnResultMapping(String columnName, Class<?> explicitJavaType) {
			this.columnName = columnName;
			this.explicitJavaType = explicitJavaType == void.class
					? null
					: explicitJavaType;
		}

		public static JpaColumnResultMapping from(ColumnResult columnResult, MetadataBuildingContext context) {
			return new JpaColumnResultMapping( columnResult.name(), columnResult.type() );
		}

		public String getColumnName() {
			return columnName;
		}

		public Class<?> getExplicitJavaType() {
			return explicitJavaType;
		}

		@Override
		public ScalarResultBuilder resolve(SessionFactoryImplementor factory) {
			if ( explicitJavaType != null ) {
				final JavaTypeDescriptor<?> jtd = factory.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( explicitJavaType );

				BootLogging.LOGGER.debugf( "ColumnResult explicit type resolved to : " + jtd );

				return new StandardScalarResultBuilder( columnName, jtd );
			}

			return new StandardScalarResultBuilder( columnName );
		}
	}

	/**
	 * @see javax.persistence.ConstructorResult
	 */
	private static class ConstructorResultMapping implements ResultMapping {

		public static ConstructorResultMapping from(
				ConstructorResult constructorResult,
				MetadataBuildingContext context) {
			final ColumnResult[] columnResults = constructorResult.columns();
			if ( columnResults.length == 0 ) {
				throw new IllegalArgumentException( "ConstructorResult did not define any ColumnResults" );
			}

			final List<ResultMapping> argumentResultMappings = new ArrayList<>( columnResults.length );
			for ( int i = 0; i < columnResults.length; i++ ) {
				final ColumnResult columnResult = columnResults[i];
				argumentResultMappings.add( JpaColumnResultMapping.from( columnResult, context ) );
			}

			return new ConstructorResultMapping(
					constructorResult.targetClass(),
					argumentResultMappings
			);
		}

		private final Class<?> targetJavaType;
		private final List<ResultMapping> argumentResultMappings;

		public ConstructorResultMapping(
				Class<?> targetJavaType,
				List<ResultMapping> argumentResultMappings) {
			this.targetJavaType = targetJavaType;
			this.argumentResultMappings = argumentResultMappings;
		}

		@Override
		public InstantiationResultBuilder resolve(SessionFactoryImplementor factory) {
			final List<ResultBuilder> argumentResultBuilders = new ArrayList<>( argumentResultMappings.size() );
			argumentResultMappings.forEach( mapping -> argumentResultBuilders.add( mapping.resolve( factory ) ) );

			final JavaTypeDescriptor<?> targetJtd = factory.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( targetJavaType );

			return new StandardInstantiationResultBuilder( targetJtd, argumentResultBuilders );
		}
	}

	/**
	 * @see javax.persistence.EntityResult
	 */
	private static class EntityResultMapping implements ResultMapping {
		public static EntityResultMapping from(
				EntityResult entityResult,
				MetadataBuildingContext context) {
			throw new NotYetImplementedFor6Exception( "Support for dynamic-instantiation results not yet implemented" );
		}

		@Override
		public EntityResultBuilder resolve(SessionFactoryImplementor factory) {
			throw new NotYetImplementedFor6Exception( getClass() );
		}
	}
}
