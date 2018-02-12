/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.annotations.common.reflection.XClass;

/**
 * Schema naming provider default implementation delegating to SchemaNameResolver
 *
 * @author Benoit Besson
 */
public class SchemaNamingDefaultProvider implements SchemaNamingProvider {

	private static final String SEQUENCE_SCHEMA_SEPARATOR = ".";

	// the resolver to delegate strategy on
	private SchemaNameResolver resolver;

	// the current processed class
	private static XClass xClass;

	// maps used for schema name requests
	private Map<String, String> tableSchemaMap = new HashMap<>();
	private Map<String, String> sequenceSchemaMap = new HashMap<>();

	// map for class name request on classes
	private Map<Class<?>, Optional<String>> classSchemaMap = new HashMap<>();

	public SchemaNamingDefaultProvider(SchemaNameResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void setCurrentProcessingClass(XClass xClass) {
		SchemaNamingDefaultProvider.xClass = xClass;
	}

	@Override
	public String resolveSchemaName(String annotationSchemaName,
			String annotationTableName) {
		// default value is annotationSchemaName
		String resolvedSchemaName = annotationSchemaName;

		// delegate to strategy
		if ( !schemaNameisSet( annotationSchemaName ) && xClass != null ) {
			resolvedSchemaName = extractSchemaNameFromStrategy();
		}
		// append to map
		this.tableSchemaMap.put( annotationTableName, resolvedSchemaName );

		// return resolved value
		return resolvedSchemaName;
	}

	private boolean schemaNameisSet(String schemaName) {
		return schemaName != null && schemaName.length() > 0;
	}

	@Override
	public String resolveSequenceName(String annotationSequenceName) {
		// default schema name is null
		String resolvedSchemaName = null;
		// default value for sequence name is annotationSequenceName
		String resolvedSequenceName = annotationSequenceName;
		// extracted sequence name without schema name is set with annotationSequenceName
		String extractedSequenceName = annotationSequenceName;

		// try to extract schemaName from annotationSequenceName
		int index = annotationSequenceName.indexOf( SEQUENCE_SCHEMA_SEPARATOR );
		if ( index != -1 ) {
			// override default values with string elements split with separator
			resolvedSchemaName = annotationSequenceName.substring( 0, index );
			extractedSequenceName = annotationSequenceName.substring( index + 1,
					annotationSequenceName.length() );
		}

		// delegate to strategy
		if ( !schemaNameisSet( resolvedSchemaName ) && xClass != null ) {
			resolvedSchemaName = extractSchemaNameFromStrategy();
			if ( resolvedSchemaName != null ) {
				// rebuild sequence name with schema prefix
				resolvedSequenceName = resolvedSchemaName + SEQUENCE_SCHEMA_SEPARATOR
						+ annotationSequenceName;
			}
		}

		// append to map
		this.sequenceSchemaMap.put( extractedSequenceName, resolvedSchemaName );

		// return resolved value
		return resolvedSequenceName;
	}

	private String extractSchemaNameFromStrategy() {
		String className = xClass.getName();
		try {
			Class<?> clazz = Class.forName( className );
			return this.resolver.getSchema( clazz );
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException( "unable to cast to class : " + className, e );
		}
	}

	public String getSchemaForTable(String tableName) {
		return this.tableSchemaMap.get( tableName );
	}

	public String getSchemaForSequence(String sequenceName) {
		return this.sequenceSchemaMap.get( sequenceName );
	}

}
