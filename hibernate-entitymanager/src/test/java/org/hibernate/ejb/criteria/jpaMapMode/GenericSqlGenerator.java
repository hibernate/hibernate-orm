package org.hibernate.ejb.criteria.jpaMapMode;

import java.sql.Types;
import java.text.MessageFormat;
import java.util.*;

/**
 * This should probably be deprecated and re-written to use Hibernate to generate the DDL.
 */
public class GenericSqlGenerator implements SqlGenerator {
    private static final Map<PropertyType, ColumnType> propertyTypeToColumnType;
    private static final Map<Integer, String> sqlTypeToDeclaredType;
    private static final Map<ColumnType, Integer> columnTypeToSqlType;

    static {
        Map<PropertyType, ColumnType> map = new HashMap<PropertyType, ColumnType>();
        map.put(PropertyType.DATE, ColumnType.TEXT);
        map.put(PropertyType.DATETIME, ColumnType.NUMERIC);
        map.put(PropertyType.DOUBLE, ColumnType.NUMERIC);
        map.put(PropertyType.LONG, ColumnType.NUMERIC);
        map.put(PropertyType.MULTILINE_TEXT, ColumnType.TEXT);
        map.put(PropertyType.TEXT, ColumnType.TEXT);
        map.put(PropertyType.ID, ColumnType.ID);

        propertyTypeToColumnType = Collections.unmodifiableMap(map);
    }

    static {
        Map<ColumnType, Integer> map = new HashMap<ColumnType, Integer>();
        map.put(ColumnType.NUMERIC, Types.BIGINT);
        map.put(ColumnType.TEXT, Types.VARCHAR);
        map.put(ColumnType.ID, Types.BINARY);

        columnTypeToSqlType = Collections.unmodifiableMap(map);
    }

    static {
        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(Types.BIGINT, "bigint");
        map.put(Types.VARCHAR, "varchar(255)");
        map.put(Types.BINARY, "raw(16)");       // there must be a better way. hmm....

        sqlTypeToDeclaredType = Collections.unmodifiableMap(map);
    }

    @Override
    public ColumnType getColumnTypeForPropertyType(PropertyType propertyType) {
        return propertyTypeToColumnType.get(propertyType);
    }

    protected String getSqlTypeForDeclaredType(Column column) {
        final int type = column.getType();
        final Map<Integer, String> sqlTypeToDeclaredType1 = getSqlTypeToDeclaredType();
        return sqlTypeToDeclaredType1.get(type);
    }

    protected Map<Integer, String> getSqlTypeToDeclaredType() {
        return sqlTypeToDeclaredType;
    }

    @Override
    public Map<ColumnType, Integer> getColumnTypeToSqlType() {
        return columnTypeToSqlType;
    }

    @Override
    public List<String> createBlobTable() {
        return Arrays.asList("create table {0} (id varchar2(36) primary key, data blob)");  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<CharSequence> createDocumentTables(MetaModelMapping metaModelMapping) {
        List<CharSequence> commands = new ArrayList<CharSequence>();
        instantiateTables(metaModelMapping, commands);
        instantiateViews(metaModelMapping, commands);
        return commands;
    }

    protected void instantiateViews(MetaModelMapping metaModelMapping, List<CharSequence> commands) {
        for (DocumentTableMapping documentTableMapping : metaModelMapping.getDocumentTableMappings().values()) {
            instantiateView(metaModelMapping, documentTableMapping, commands);
        }
    }

    protected void instantiateTables(MetaModelMapping mapping,
                                     List<CharSequence> commands) {

        for (Table table : mapping.getTables()) {
            instantiateTable(table, commands);
        }
    }

    protected String getCreateViewTemplate() {
        return "create view {0} as select {1} from {2}";
    }

    protected void instantiateView(MetaModelMapping metaModelMapping, DocumentTableMapping documentTableMapping, List<CharSequence> commands) {
        StringBuilder builder = new StringBuilder();
        builder.append(MessageFormat.format(getCreateViewTemplate(), documentTableMapping.getDocument().getName(), getColumns(metaModelMapping, documentTableMapping), documentTableMapping.getTable().getTableName()));
        commands.add(builder);
    }

    protected CharSequence getColumns(MetaModelMapping metaModelMapping, DocumentTableMapping documentTableMapping) {
        StringBuilder builder = new StringBuilder();

        builder.append(documentTableMapping.getTable().getIdColumnName());

        for (Map.Entry<Property, PropertyColumnMapping> entry : documentTableMapping.getPropertyMappings().entrySet()) {
            builder.append(", ");
            Property property = entry.getKey();
            PropertyColumnMapping propertyColumnMapping = entry.getValue();

            builder.append(propertyColumnMapping.getColumn().getName());
            builder.append(" as ");
            builder.append(property.getName());
        }

        for (RelationshipColumnMapping relationshipColumnMapping : metaModelMapping.getRelationshipColumnMappings().values()) {
            if (relationshipColumnMapping.getRelationship().getDocument(relationshipColumnMapping.getSide()) == documentTableMapping.getDocument()) {
                builder.append(", ");

                builder.append(relationshipColumnMapping.getColumn().getName());
                builder.append(" as ");
                builder.append(relationshipColumnMapping.getRelationship().getRoleName(relationshipColumnMapping.getSide()));
            }
        }

        return builder;
    }

    protected String getCreateTableTemplate() {
        return "create table {0} ( {1} binary(16) primary key";
    }

    protected void instantiateTable(Table table, List<CharSequence> commands) {
        StringBuilder builder = new StringBuilder();
        builder.append(MessageFormat.format(getCreateTableTemplate(),
                table.getTableName(), table.getIdColumnName()));

        generateColumns(builder, table);
        builder.append(");");
        commands.add(builder);
    }

    protected void generateColumns(StringBuilder builder, Table table) {
        for (Column column : table.getColumns()) {
            generateColumn(column, builder);
        }

    }

    protected String getCreateColumnTemplate() {
        return ",\r\n    {0} {1}";
    }

    protected void generateColumn(Column column, StringBuilder builder) {
        final String sqlTypeForDeclaredType = this.getSqlTypeForDeclaredType(column);
        builder.append(MessageFormat.format(getCreateColumnTemplate(),
                column.getName(), sqlTypeForDeclaredType));
    }

    @Override
    public String createColumnName(ColumnType columnType, int i) {
        switch (columnType) {
            case NUMERIC:
                return "n_" + i;
            case TEXT:
                return "t_" + i;
            case ID:
                return "fk_" + i;
            default:
                throw new IllegalStateException("Unknown column type " + columnType);
        }
    }

    @Override
    public String createTableName(int i) {
        return "t_" + i;
    }

    @Override
    public Column createTextColumn(int i) {
        return new Column(createColumnName(ColumnType.TEXT, i), Types.VARCHAR);
    }

    @Override
    public Column createNumericColumn(int i) {
        return new Column(createColumnName(ColumnType.NUMERIC, i), Types.BIGINT);
    }

    @Override
    public Column createForeignKeyColumn(int i) {
        return new Column(createColumnName(ColumnType.ID, i), Types.BINARY);
    }

}
