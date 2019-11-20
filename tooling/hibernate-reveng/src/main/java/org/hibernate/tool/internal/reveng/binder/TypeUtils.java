package org.hibernate.tool.internal.reveng.binder;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.JdbcToHibernateTypeHelper;
import org.hibernate.tool.internal.util.TableNameQualifier;
import org.hibernate.type.Type;

public class TypeUtils {
	
	private static final Logger LOGGER = Logger.getLogger(TypeUtils.class.getName());

	public static String determinePreferredType(
			InFlightMetadataCollector metadataCollector,
			ReverseEngineeringStrategy revengStrategy,
			Table table, 
			Column column, 
			Mapping mapping, 
			boolean generatedIdentifier) {

		String location = 
				"Table: " + 
				TableNameQualifier.qualify(
						table.getCatalog(), 
						table.getSchema(), 
						table.getQuotedName() ) + 
				" column: " + 
				column.getQuotedName();

		Integer sqlTypeCode = column.getSqlTypeCode();
		if(sqlTypeCode==null) {
			throw new RuntimeException("sqltype is null for " + location);
		}

		String preferredHibernateType = revengStrategy.columnToHibernateTypeName(
				TableIdentifier.create(table),
				column.getName(),
				sqlTypeCode.intValue(),
				column.getLength(), 
				column.getPrecision(), 
				column.getScale(),
				column.isNullable(), 
				generatedIdentifier
		);

		Type wantedType = metadataCollector
				.getTypeResolver()
				.heuristicType(preferredHibernateType);

		if(wantedType!=null) {

			int[] wantedSqlTypes = wantedType.sqlTypes(mapping);

			if(wantedSqlTypes.length>1) {
				throw new RuntimeException("The type " + preferredHibernateType + " found on " + location + " spans multiple columns. Only single column types allowed.");
			}

			int wantedSqlType = wantedSqlTypes[0];
			if(wantedSqlType!=sqlTypeCode.intValue() ) {
				LOGGER.log(
						Level.INFO,
						"Sql type mismatch for " + location + " between DB and wanted hibernate type. Sql type set to " + typeCodeName( sqlTypeCode.intValue() ) + " instead of " + typeCodeName(wantedSqlType) );
				column.setSqlTypeCode(Integer.valueOf(wantedSqlType));
			}
			
		}
		
		else {
			
			LOGGER.log(
					Level.INFO, 
					"No Hibernate type found for " + preferredHibernateType + ". Most likely cause is a missing UserType class.");

		}



		if(preferredHibernateType==null) {
			throw new RuntimeException("Could not find javatype for " + typeCodeName(sqlTypeCode.intValue()));
		}

		return preferredHibernateType;
	}
	
	private static String typeCodeName(int sqlTypeCode) {
		return sqlTypeCode + "(" + JdbcToHibernateTypeHelper.getJDBCTypeName(sqlTypeCode) + ")";
	}


}
