/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.ColumnResult;
import javax.persistence.SqlResultSetMapping;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.BootLogging;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.NamedResultSetMappingMementoImpl;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.ScalarResultBuilder;
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
		if ( mappingAnnotation.classes().length > 0 ) {
			throw new NotYetImplementedFor6Exception(
					"Support for dynamic-instantiation result mappings not yet implemented"
			);
		}

		if ( mappingAnnotation.entities().length > 0 ) {
			throw new NotYetImplementedFor6Exception(
					"Support for entity result mappings not yet implemented"
			);
		}

		if ( mappingAnnotation.columns().length == 0 ) {
			throw new NotYetImplementedFor6Exception( "Should never get here" );
		}

		final List<JpaColumnResultMapping> columnResultMappings;
		if ( mappingAnnotation.columns().length == 0 ) {
			columnResultMappings = null;
		}
		else {
			columnResultMappings = new ArrayList<>( mappingAnnotation.columns().length );
			for ( int i = 0; i < mappingAnnotation.columns().length; i++ ) {
				final ColumnResult columnMapping = mappingAnnotation.columns()[i];
				columnResultMappings.add(
						new JpaColumnResultMapping( columnMapping.name(), columnMapping.type() )
				);
			}
		}

		return new SqlResultSetMappingDefinition(
				mappingAnnotation.name(),
				columnResultMappings,
				context
		);
	}

	private final String mappingName;

	private final List<JpaColumnResultMapping> columnResultMappings;

	private SqlResultSetMappingDefinition(
			String mappingName,
			List<JpaColumnResultMapping> columnResultMappings,
			MetadataBuildingContext context) {
		this.mappingName = mappingName;
		this.columnResultMappings = columnResultMappings;
	}

	@Override
	public String getRegistrationName() {
		return mappingName;
	}

	@Override
	public NamedResultSetMappingMemento resolve(SessionFactoryImplementor factory) {
		final List<ScalarResultBuilder> scalarResultBuilders = new ArrayList<>();

		for ( int i = 0; i < columnResultMappings.size(); i++ ) {
			final JpaColumnResultMapping resultMapping = columnResultMappings.get( i );
			scalarResultBuilders.add( resultMapping.resolve( factory ) );
		}

		return new NamedResultSetMappingMementoImpl(
				mappingName,
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
}
