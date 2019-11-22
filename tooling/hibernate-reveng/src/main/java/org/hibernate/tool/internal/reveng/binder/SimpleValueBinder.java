package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

public class SimpleValueBinder {

	public static SimpleValue bind(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			ReverseEngineeringStrategy revengStrategy,
			Table table, 
			Column column, 
			Mapping mapping, 
			boolean generatedIdentifier) {
		SimpleValue value = new SimpleValue(metadataBuildingContext, table);
		value.addColumn(column);
		value.setTypeName(TypeUtils.determinePreferredType(
				metadataCollector, 
				revengStrategy,
				table, 
				column, 
				mapping, 
				generatedIdentifier));
		return value;
	}

}
