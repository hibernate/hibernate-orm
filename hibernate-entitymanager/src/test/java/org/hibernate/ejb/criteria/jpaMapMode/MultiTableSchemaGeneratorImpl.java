package org.hibernate.ejb.criteria.jpaMapMode;


import java.util.*;

public class MultiTableSchemaGeneratorImpl {
    private SqlGenerator sqlGenerator;
    private int textColumnsPerTable = 3;
    private int numberOfTables = 5;
    private int numericColumnsPerTable = 2;
    private int foreignKeyColumnsPerTable = 2;

    public void setSqlGenerator(final SqlGenerator sqlGenerator)
    {
        this.sqlGenerator = sqlGenerator;
    }

    public MetaModelMapping createMetaModelMapping(MetaModel metaModel) {
        MetaModelMapping mapping = new MetaModelMapping(metaModel);
        createTables(mapping);
        mapMetaModel(mapping);

        return mapping;
    }

    protected void mapMetaModel(MetaModelMapping mapping) {
        mapDocumentsToTables(mapping);
        mapRelationshipsToTables(mapping);
    }

    protected void mapRelationshipsToTables(MetaModelMapping mapping) {
        for (Relationship relationship : mapping.getMetaModel().getRelationships()) {
            RelationshipColumnMapping relationshipColumnMapping = createRelationshipColumnMapping(relationship, mapping);
            mapping.addRelationshipColumnMapping(relationshipColumnMapping);
        }
    }

    protected RelationshipColumnMapping createRelationshipColumnMapping(Relationship relationship, MetaModelMapping mapping) {
        Multiplicity fm = relationship.getFromMultiplicity();
        Multiplicity tm = relationship.getToMultiplicity();

        boolean fmm = isMultiplicityMany(fm);
        boolean tmm = isMultiplicityMany(tm);

        if (!(fmm && tmm)) {
            DocumentTableMapping documentTableMapping;
            Relationship.Side side;
            if (!fmm) {
                documentTableMapping = mapping.getDocumentTableMappings().get(relationship.getFrom());
                side = Relationship.Side.FROM;
            } else {
                documentTableMapping = mapping.getDocumentTableMappings().get(relationship.getTo());
                side = Relationship.Side.TO;
            }

            Column column = findUnusedForeignKeyColumn(documentTableMapping);

            return new RelationshipColumnMapping(relationship, documentTableMapping, column, side);
        } else {
            throw new IllegalStateException("I thought you weren't going to have many-to-many mappings!");
        }
    }

    protected Column findUnusedForeignKeyColumn(DocumentTableMapping documentTableMapping) {
        Set<Column> columns = new HashSet<Column>(documentTableMapping.getTable().getColumns());
        for (PropertyColumnMapping propertyColumnMapping : documentTableMapping.getPropertyMappings().values()) {
            columns.remove(propertyColumnMapping.getColumn());
        }

        return findUnusedColumn(columns, PropertyType.ID);
    }

    private boolean isMultiplicityMany(Multiplicity tm) {
        return tm == Multiplicity.ZERO_OR_MORE || tm == Multiplicity.ONE_OR_MORE;
    }

    protected void mapDocumentsToTables(MetaModelMapping mapping) {
        for (Document document : mapping.getMetaModel().getDocuments()) {
            Table table = findAvailableTable(mapping);
            DocumentTableMapping documentTableMapping = mapDocumentToTable(
                    document, table);
            mapping.addDocumentTableMapping(documentTableMapping);
        }
    }

    protected DocumentTableMapping mapDocumentToTable(Document document,
                                                      Table table) {
        Set<PropertyColumnMapping> propertyMappings = mapPropertiesToColumns(
                document, table);
        return new DocumentTableMapping(document,
                propertyMappings, table);
    }

    protected Set<PropertyColumnMapping> mapPropertiesToColumns(
            Document document, Table table) {
        Set<Column> unusedColumns = new HashSet<Column>(table.getColumns());
        Set<PropertyColumnMapping> propertyMappings = new HashSet<PropertyColumnMapping>(document
                .getProperties().size());

        for (Property property : document.getProperties()) {
            propertyMappings.add(createPropertyMapping(unusedColumns, property,
                    table));
        }

        return propertyMappings;
    }

    protected PropertyColumnMapping createPropertyMapping(
            Set<Column> unusedColumns, Property property, Table table) {
        Column column = findUnusedColumn(table, property, unusedColumns);
        return new PropertyColumnMapping(property,
                column);
    }

    protected Column findUnusedColumn(Table table, Property property,
                                      Set<Column> unusedColumns) {
        PropertyType propertyType = property.getType();

        Column column = findUnusedColumn(unusedColumns, propertyType);

        if (column != null) {
            return column;
        }

        throw new IllegalStateException(
                "Unable to find an available column for a property of type "
                        + propertyType.toString() + " in table "
                        + table.getSqlName() + " while mapping "
                        + property.getDocument().getName() + "."
                        + property.getName());
    }

    protected Column findUnusedColumn(Set<Column> unusedColumns, PropertyType propertyType) {
        ColumnType columnType = getColumnTypeForPropertyType(propertyType);
        Integer sqlType = sqlGenerator.getColumnTypeToSqlType().get(columnType);

        for (Iterator<Column> iter = unusedColumns.iterator(); iter.hasNext(); ) {
            Column column = iter.next();
            if (column.getType() == sqlType) {
                iter.remove();
                return column;
            }
        }

        return null;
    }

    protected ColumnType getColumnTypeForPropertyType(PropertyType propertyType) {
        return sqlGenerator.getColumnTypeForPropertyType(propertyType);
    }

    protected Table findAvailableTable(MetaModelMapping mapping) {
        for (Table table : mapping.getTables()) {
            if (table.getDocumentMapping() == null) {
                return table;
            }
        }
        throw new IllegalStateException(
                "Unable to locate table. Probably should have made more to begin with");
    }

    protected void createTables(MetaModelMapping mapping) {
        for (int i = 0; i < numberOfTables; i++) {
            mapping.addTable(createTable(i));
        }
    }

    protected Table createTable(int i) {
        Set<Column> columns = createColumns();
        return new Table(getTableName(i), "id", null,
                columns);
    }

    private Set<Column> createColumns() {
        Set<Column> columns = new HashSet<Column>(numericColumnsPerTable
                + textColumnsPerTable);
        columns.addAll(createTextColumns());
        columns.addAll(createNumericColumns());
        columns.addAll(createForeignKeyColumns());
        return columns;
    }

    protected Collection<? extends Column> createForeignKeyColumns() {
        Set<Column> columns = new HashSet<Column>(foreignKeyColumnsPerTable);
        for (int i = 0; i < foreignKeyColumnsPerTable; i++) {
            columns.add(createForeignKeyColumn(i));
        }
        return columns;
    }

    protected Column createForeignKeyColumn(int i) {
        return sqlGenerator.createForeignKeyColumn(i);
    }

    protected Set<Column> createTextColumns() {
        Set<Column> columns = new HashSet<Column>(textColumnsPerTable);
        for (int i = 0; i < textColumnsPerTable; i++) {
            columns.add(createTextColumn(i));
        }
        return columns;
    }

    protected Column createTextColumn(int i) {
        return sqlGenerator.createTextColumn(i);
    }

    protected Set<Column> createNumericColumns() {
        Set<Column> columns = new HashSet<Column>(numericColumnsPerTable);
        for (int i = 0; i < numericColumnsPerTable; i++) {
            columns.add(sqlGenerator.createNumericColumn(i));
        }
        return columns;
    }

    protected String getTableName(int i) {
        return sqlGenerator.createTableName(i);
    }

}