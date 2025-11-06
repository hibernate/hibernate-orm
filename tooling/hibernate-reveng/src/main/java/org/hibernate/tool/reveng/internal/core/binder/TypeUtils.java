/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.util.JdbcToHibernateTypeHelper;
import org.hibernate.tool.reveng.internal.util.TableNameQualifier;
import org.hibernate.type.Type;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TypeUtils {

	private static final Logger LOGGER = Logger.getLogger(TypeUtils.class.getName());

	public static final int DEFAULT_COLUMN_LENGTH = 255;
	public static final int DEFAULT_COLUMN_PRECISION = 19;
	public static final int DEFAULT_COLUMN_SCALE = 2;

	public static String determinePreferredType(
			InFlightMetadataCollector metadataCollector,
			RevengStrategy revengStrategy,
			Table table,
			Column column,
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
				sqlTypeCode,
				column.getLength() != null ? column.getLength().intValue() : DEFAULT_COLUMN_LENGTH,
				column.getPrecision() != null ? column.getPrecision() : DEFAULT_COLUMN_PRECISION,
				column.getScale() != null ? column.getScale() : DEFAULT_COLUMN_SCALE,
				column.isNullable(),
				generatedIdentifier
		);

		Type wantedType = metadataCollector
				.getTypeConfiguration()
				.getBasicTypeRegistry()
				.getRegisteredType(preferredHibernateType);

		if(wantedType!=null) {

			int[] wantedSqlTypes = wantedType.getSqlTypeCodes(metadataCollector);

			if(wantedSqlTypes.length>1) {
				throw new RuntimeException("The type " + preferredHibernateType + " found on " + location + " spans multiple columns. Only single column types allowed.");
			}

			int wantedSqlType = wantedSqlTypes[0];
			if( wantedSqlType != sqlTypeCode ) {
				LOGGER.log(
						Level.INFO,
						"Sql type mismatch for " + location + " between DB and wanted hibernate type. Sql type set to " + typeCodeName(
								sqlTypeCode ) + " instead of " + typeCodeName(wantedSqlType) );
				forceSqlTypeCode(column, wantedSqlType);
			}

		}

		else {

			LOGGER.log(
					Level.INFO,
					"No Hibernate type found for " + preferredHibernateType + ". Most likely cause is a missing UserType class.");

		}



		if(preferredHibernateType==null) {
			throw new RuntimeException("Could not find javatype for " + typeCodeName( sqlTypeCode ));
		}

		return preferredHibernateType;
	}

	private static String typeCodeName(int sqlTypeCode) {
		return sqlTypeCode + "(" + JdbcToHibernateTypeHelper.getJDBCTypeName(sqlTypeCode) + ")";
	}

	private static void forceSqlTypeCode(Column column, int sqlCode) {
		try {
			Field sqlCodeField = Column.class.getDeclaredField("sqlTypeCode");
			sqlCodeField.setAccessible(true);
			sqlCodeField.set(column, sqlCode );
		}
		catch (NoSuchFieldException |
				SecurityException |
				IllegalArgumentException |
				IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}


}
