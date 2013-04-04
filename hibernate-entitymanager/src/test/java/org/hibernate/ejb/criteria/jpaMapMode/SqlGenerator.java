package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.List;
import java.util.Map;

/**
 * Used for generating SQL for creating the schema in a database.
 *
 * @author Brad Koehn
 */
public interface SqlGenerator {
    ColumnType getColumnTypeForPropertyType(PropertyType propertyType);

    List<CharSequence> createDocumentTables(MetaModelMapping metaModelMapping);

    String createColumnName(ColumnType columnType, int i);

    String createTableName(int i);

    Column createTextColumn(int i);

    Column createNumericColumn(int i);

    Column createForeignKeyColumn(int i);

    Map<ColumnType, Integer> getColumnTypeToSqlType();

    List<String> createBlobTable();
}
