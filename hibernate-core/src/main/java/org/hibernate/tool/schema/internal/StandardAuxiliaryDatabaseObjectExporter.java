package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardAuxiliaryDatabaseObjectExporter implements Exporter<AuxiliaryDatabaseObject> {
	public StandardAuxiliaryDatabaseObjectExporter() {
	}

	@Override
	public String[] getSqlCreateStrings(AuxiliaryDatabaseObject object, Metadata metadata,
			SqlStringGenerationContext context) {
		return object.sqlCreateStrings( context );
	}

	@Override
	public String[] getSqlDropStrings(AuxiliaryDatabaseObject object, Metadata metadata,
			SqlStringGenerationContext context) {
		return object.sqlDropStrings( context );
	}
}
