package org.hibernate.tool.schema.spi;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.service.Service;

public interface SchemaFilter {

	boolean includeSchema(Schema schema);
	
	boolean includeTable( Table table );

	boolean includeSequence( Sequence sequence );
	
}
