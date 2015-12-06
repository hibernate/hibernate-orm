package org.hibernate.tool.schema.spi;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;

public interface SchemaFilter {

	boolean includeNamespace( Namespace namespace );
	
	boolean includeTable( Table table );

	boolean includeSequence( Sequence sequence );
	
}
