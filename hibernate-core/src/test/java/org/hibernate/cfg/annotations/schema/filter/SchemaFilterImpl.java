package org.hibernate.cfg.annotations.schema.filter;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.cfg.schema.SchemaNamingDefaultProvider;
import org.hibernate.cfg.schema.SchemaNamingProviderLocator;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;

public class SchemaFilterImpl implements SchemaFilter {

	/** schemaName. This is to be set for a global schema, null for specific. */
	private String schemaName;

	public SchemaFilterImpl(String schemaName) {
		this.schemaName = schemaName;
	}

	@Override
	public boolean includeNamespace(Namespace namespace) {
		Identifier schema = namespace.getName().getSchema();
		if ( schema == null ) {
			return includeEmptyNameSpace();
		}
		String schemaName = schema.getText();
		boolean included = isSchemaNameFiltered( schemaName );
//		log.debugf( "testing namespace {} for schema {} : included={}",
//				namespace.getName(), schemaName, included );
		return included;

	}

	public boolean includeEmptyNameSpace() {
		// a main component must return true to include main data e.g. hibernate sequence
		// a dynamic schema component shall include all its elements
		return this.schemaName == null;
	}

	private boolean isSchemaNameFiltered(String schemaName) {
		return this.schemaName.equals( schemaName );
	}

	@Override
	public boolean includeSequence(Sequence sequence) {
//		log.debugf( "testing sequence {}", sequence );
		String sequenceName = sequence.getName().getObjectName().getText();

		Identifier schemaNameIdentifier = sequence.getName().getSchemaName();
		String schemaName = schemaNameIdentifier != null
				? schemaNameIdentifier.getText()
				: null;
		boolean included = isSchemaNameFiltered( schemaName );
		if ( included ) {
			// then filter on table name
			included = isSchemaNameFiltered( getLocator()
					.getSchemaForSequence( sequenceName ) );
		}
//		log.debugf( "testing sequence {}  for schema {} : included={}", sequenceName,
//				schemaName, included );

		return included;
	}

	@Override
	public boolean includeTable(Table table) {

		String schemaName = table.getSchema();
		boolean included = isSchemaNameFiltered( schemaName );
		String tableName = table.getName();
		if ( included ) {
			// then filter on table name
			included = isSchemaNameFiltered( getLocator().getSchemaForTable( tableName ) );
		}
//		log.debugf( "testing table {}  for schema {} : included={}", tableName,
//				schemaName, included );
		return included;
	}
	
	public SchemaNamingDefaultProvider getLocator()
	{
		return (SchemaNamingDefaultProvider) SchemaNamingProviderLocator.getInstance();
	}

}
