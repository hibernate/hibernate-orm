/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.strategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.hibernate.MappingException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.strategy.MetaAttributeHelper.SimpleMetaAttribute;
import org.hibernate.tool.internal.util.JdbcToHibernateTypeHelper;
import org.hibernate.tool.internal.util.TableNameQualifier;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public class OverrideRepository  {

    final private static Logger log = Logger.getLogger( OverrideRepository.class );

    final private Map<TypeMappingKey, List<SQLTypeMapping>> typeMappings; // from sqltypes to list of SQLTypeMapping

    final private List<TableFilter> tableFilters;

    final private Map<TableIdentifier, List<ForeignKey>> foreignKeys; // key: TableIdentifier element: List of foreignkeys that references the Table

    final private Map<TableColumnKey, String> typeForColumn;

    final private Map<TableColumnKey, String> propertyNameForColumn;

    final private Map<TableIdentifier, String> identifierStrategyForTable;

    final private Map<TableIdentifier, Properties> identifierPropertiesForTable;

    final private Map<TableIdentifier, List<String>> primaryKeyColumnsForTable;

    final private Set<TableColumnKey> excludedColumns;

    final private TableToClassName tableToClassName;

    final private List<SchemaSelection> schemaSelections;

    final private Map<TableIdentifier, String> propertyNameForPrimaryKey;

    final private Map<TableIdentifier, String> compositeIdNameForTable;

    final private Map<String, String> foreignKeyToOneName;

    final private Map<String, String> foreignKeyToInverseName;

    final private Map<String, Boolean> foreignKeyInverseExclude;

    final private Map<String, Boolean> foreignKeyToOneExclude;

    final private Map<String, AssociationInfo> foreignKeyToEntityInfo;
    final private Map<String, AssociationInfo> foreignKeyToInverseEntityInfo;

    final private Map<TableIdentifier, MultiValuedMap<String, SimpleMetaAttribute>> tableMetaAttributes; // TI -> MultiMap of SimpleMetaAttributes

    final private Map<TableColumnKey, MultiValuedMap<String, SimpleMetaAttribute>> columnMetaAttributes;

    //private String defaultCatalog;
    //private String defaultSchema;

    public OverrideRepository() {
        //this.defaultCatalog = null;
        //this.defaultSchema = null;
        typeMappings = new HashMap<>();
        tableFilters = new ArrayList<>();
        foreignKeys = new HashMap<>();
        typeForColumn = new HashMap<>();
        propertyNameForColumn = new HashMap<>();
        identifierStrategyForTable = new HashMap<>();
        identifierPropertiesForTable = new HashMap<>();
        primaryKeyColumnsForTable = new HashMap<>();
        propertyNameForPrimaryKey = new HashMap<>();
        tableToClassName = new TableToClassName();
        excludedColumns = new HashSet<>();
        schemaSelections = new ArrayList<>();
        compositeIdNameForTable = new HashMap<>();
        foreignKeyToOneName = new HashMap<>();
        foreignKeyToInverseName = new HashMap<>();
        foreignKeyInverseExclude = new HashMap<>();
        foreignKeyToOneExclude = new HashMap<>();
        tableMetaAttributes = new HashMap<>();
        columnMetaAttributes = new HashMap<>();
        foreignKeyToEntityInfo = new HashMap<>();
        foreignKeyToInverseEntityInfo = new HashMap<>();
    }

    public void addFile(File xmlFile) {
        log.info( "Override file: " + xmlFile.getPath() );
        try {
            addInputStream( xmlFile );
        }
        catch ( Exception e ) {
            log.error( "Could not configure overrides from file: " + xmlFile.getPath(), e );
            throw new MappingException( "Could not configure overrides from file: " + xmlFile.getPath(), e );
        }

    }

    /**
     * Read override from an application resource trying different classloaders.
     * This method will try to load the resource first from the thread context
     * classloader and then from the classloader that loaded Hibernate.
     */
    public OverrideRepository addResource(String path) throws MappingException {
        log.info( "Mapping resource: " + path );
        InputStream rsrc = Thread.currentThread().getContextClassLoader().getResourceAsStream( path );
        if ( rsrc == null ) rsrc = OverrideRepository.class.getClassLoader().getResourceAsStream( path );
        if ( rsrc == null ) throw new MappingException( "Resource: " + path + " not found" );
        try {
            return addInputStream( rsrc );
        }
        catch ( MappingException me ) {
            throw new MappingException( "Error reading resource: " + path, me );
        }
    }

    public OverrideRepository addInputStream(InputStream xmlInputStream) throws MappingException {
        try {
            final List<SAXParseException> errors = new ArrayList<>();
            ErrorHandler errorHandler = new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    log.warn("warning while parsing xml", exception);
                }
                @Override
                public void error(SAXParseException exception) {
                    errors.add(exception);
                }
                @Override
                public void fatalError(SAXParseException exception) {
                    error(exception);
                }
            };
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(
                    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
                    Thread.currentThread().getContextClassLoader());
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(errorHandler);
            Document document = db.parse(xmlInputStream);
            if ( !errors.isEmpty()) throw new MappingException( "invalid override definition", errors.get( 0 ) );
            add( document );
            return this;
        }
        catch ( MappingException me ) {
            throw me;
        }
        catch ( Exception e ) {
            log.error( "Could not configure overrides from input stream", e );
            throw new MappingException( e );
        }
    }

    public void addInputStream(File file) throws MappingException {
        try (InputStream xmlInputStream = new FileInputStream( file )) {
            try {
                addInputStream( xmlInputStream );
            }
            catch (Exception e) {
                log.error( "Could not configure overrides from input stream", e );
                throw new MappingException( e );
            }
        }
        catch (IOException ioe) {
            log.error( "could not close input stream", ioe );
        }
    }

    private void add(Document doc) {
        OverrideBinder.bindRoot(this, doc);
    }

    private String getPreferredHibernateType(int sqlType, int length, int precision, int scale, boolean nullable) {
        List<SQLTypeMapping> l = typeMappings.get(new TypeMappingKey(sqlType,length) );

        if(l == null) { // if no precise length match found, then try to find matching unknown length matches
            l = typeMappings.get(new TypeMappingKey(sqlType,SQLTypeMapping.UNKNOWN_LENGTH) );
        }
        return scanForMatch( sqlType, length, precision, scale, nullable, l );
    }

    private String scanForMatch(int sqlType, int length, int precision, int scale, boolean nullable, List<SQLTypeMapping> l) {
        if(l!=null) {
            for ( SQLTypeMapping element : l ) {
                if ( element.getJDBCType() != sqlType ) return null;
                if ( element.match( sqlType, length, precision, scale, nullable ) ) {
                    return element.getHibernateType();
                }
            }
        }
        return null;
    }

    public void addTypeMapping(SQLTypeMapping sqltype) {
        TypeMappingKey key = new TypeMappingKey(sqltype);
        List<SQLTypeMapping> list = typeMappings.computeIfAbsent( key, k -> new ArrayList<>() );
        list.add(sqltype);
    }

    static class TypeMappingKey {

        int type;
        int length;

        TypeMappingKey(SQLTypeMapping mpa) {
            type = mpa.getJDBCType();
            length = mpa.getLength();
        }

        public TypeMappingKey(int sqlType, int length) {
            this.type = sqlType;
            this.length = length;
        }

        public boolean equals(Object obj) {
            if(obj==null) return false;
            if(!(obj instanceof TypeMappingKey other)) return false;


            return type==other.type && length==other.length;
        }

        public int hashCode() {
            return (type + length) % 17;
        }

        public String toString() {
            return this.getClass() + "(type:" + type + ", length:" + length + ")";
        }
    }

    protected String getPackageName(TableIdentifier identifier) {
        for ( TableFilter tf : tableFilters ) {
            String value = tf.getPackage( identifier );
            if ( value != null ) {
                return value;
            }
        }
        return null;
    }

    protected boolean excludeTable(TableIdentifier identifier) {
        Iterator<TableFilter> iterator = tableFilters.iterator();
        boolean hasInclude = false;

        while(iterator.hasNext() ) {
            TableFilter tf = iterator.next();
            Boolean value = tf.exclude(identifier);
            if(value!=null) {
                return value;
            }
            if(!tf.getExclude() ) {
                hasInclude = true;
            }
        }

        return hasInclude;
    }

    public void addTableFilter(TableFilter filter) {
        tableFilters.add(filter);
    }

    public RevengStrategy getReverseEngineeringStrategy(RevengStrategy delegate) {
        return new DelegatingStrategy(delegate) {

            public boolean excludeTable(TableIdentifier ti) {
                return OverrideRepository.this.excludeTable(ti);
            }

            public Map<String,MetaAttribute> tableToMetaAttributes(TableIdentifier tableIdentifier) {
                return OverrideRepository.this.tableToMetaAttributes(tableIdentifier);
            }

            public Map<String, MetaAttribute> columnToMetaAttributes(TableIdentifier tableIdentifier, String column) {
                return OverrideRepository.this.columnToMetaAttributes(tableIdentifier, column);
            }

            public boolean excludeColumn(TableIdentifier identifier, String columnName) {
                return excludedColumns.contains(new TableColumnKey(identifier, columnName));
            }

            public String tableToCompositeIdName(TableIdentifier identifier) {
                String result = compositeIdNameForTable.get(identifier);
                if(result==null) {
                    return super.tableToCompositeIdName(identifier);
                }
                else {
                    return result;
                }
            }
            public List<SchemaSelection> getSchemaSelections() {
                if(schemaSelections.isEmpty()) {
                    return super.getSchemaSelections();
                }
                else {
                    return schemaSelections;
                }
            }

            public String columnToHibernateTypeName(TableIdentifier table, String columnName, int sqlType, int length, int precision, int scale, boolean nullable, boolean generatedIdentifier) {
                String result;
                String location = "";
                String info = " t:" + JdbcToHibernateTypeHelper.getJDBCTypeName( sqlType ) + " l:" + length + " p:" + precision + " s:" + scale + " n:" + nullable + " id:" + generatedIdentifier;
                if(table!=null) {
                    location = TableNameQualifier.qualify(table.getCatalog(), table.getSchema(), table.getName() ) + "." + columnName;
                }
                else {

                    location += " Column: " + columnName + info;
                }
                if(table!=null && columnName!=null) {
                    result = typeForColumn.get(new TableColumnKey(table, columnName));
                    if(result!=null) {
                        log.debug("explicit column mapping found for [" + location + "] to [" + result + "]");
                        return result;
                    }
                }

                result = OverrideRepository.this.getPreferredHibernateType(sqlType, length, precision, scale, nullable);
                if(result==null) {
                    return super.columnToHibernateTypeName(table, columnName, sqlType, length, precision, scale, nullable, generatedIdentifier);
                }
                else {
                    log.debug("<type-mapping> found for [" + location + info + "] to [" + result + "]");
                    return result;
                }
            }

            public String tableToClassName(TableIdentifier tableIdentifier) {
                String className = tableToClassName.get(tableIdentifier);

                if(className!=null) {
                    if( className.contains( "." ) ) {
                        return className;
                    }
                    else {
                        String packageName = getPackageName(tableIdentifier);
                        if(packageName==null) {
                            return className;
                        }
                        else {
                            return StringHelper.qualify(packageName, className);
                        }
                    }
                }

                String packageName = getPackageName(tableIdentifier);
                if(packageName==null) {
                    return super.tableToClassName(tableIdentifier);
                }
                else {
                    String string = super.tableToClassName(tableIdentifier);
                    if(string==null) return null;
                    return StringHelper.qualify(packageName, StringHelper.unqualify(string));
                }
            }

            public List<ForeignKey> getForeignKeys(TableIdentifier referencedTable) {
                List<ForeignKey> list = foreignKeys.get(referencedTable);
                if(list==null) {
                    return super.getForeignKeys(referencedTable);
                }
                else {
                    return list;
                }
            }

            public String columnToPropertyName(TableIdentifier table, String column) {
                String result = propertyNameForColumn.get(new TableColumnKey(table, column));
                if(result==null) {
                    return super.columnToPropertyName(table, column);
                }
                else {
                    return result;
                }
            }

            public String tableToIdentifierPropertyName(TableIdentifier tableIdentifier) {
                String result = propertyNameForPrimaryKey.get(tableIdentifier);
                if(result==null) {
                    return super.tableToIdentifierPropertyName(tableIdentifier);
                }
                else {
                    return result;
                }
            }

            public String getTableIdentifierStrategyName(TableIdentifier tableIdentifier) {
                String result = identifierStrategyForTable.get(tableIdentifier);
                if(result==null) {
                    return super.getTableIdentifierStrategyName( tableIdentifier );
                }
                else {
                    log.debug("tableIdentifierStrategy for " + tableIdentifier + " -> '" + result + "'");
                    return result;
                }
            }

            public Properties getTableIdentifierProperties(TableIdentifier tableIdentifier) {
                Properties result = identifierPropertiesForTable.get(tableIdentifier);
                if(result==null) {
                    return super.getTableIdentifierProperties( tableIdentifier );
                }
                else {
                    return result;
                }
            }

            public List<String> getPrimaryKeyColumnNames(TableIdentifier tableIdentifier) {
                List<String> result = primaryKeyColumnsForTable.get(tableIdentifier);
                if(result==null) {
                    return super.getPrimaryKeyColumnNames(tableIdentifier);
                }
                else {
                    return result;
                }
            }

            public String foreignKeyToEntityName(String keyname, TableIdentifier fromTable, List<?> fromColumnNames, TableIdentifier referencedTable, List<?> referencedColumnNames, boolean uniqueReference) {
                String property = foreignKeyToOneName.get(keyname);
                if(property==null) {
                    return super.foreignKeyToEntityName(keyname, fromTable, fromColumnNames, referencedTable, referencedColumnNames, uniqueReference);
                }
                else {
                    return property;
                }
            }


            public String foreignKeyToInverseEntityName(String keyname,
                                                        TableIdentifier fromTable, List<?> fromColumnNames,
                                                        TableIdentifier referencedTable,
                                                        List<?> referencedColumnNames, boolean uniqueReference) {

                String property = foreignKeyToInverseName.get(keyname);
                if(property==null) {
                    return super.foreignKeyToInverseEntityName(keyname, fromTable, fromColumnNames, referencedTable, referencedColumnNames, uniqueReference);
                }
                else {
                    return property;
                }
            }

            public String foreignKeyToCollectionName(String keyname, TableIdentifier fromTable, List<?> fromColumns, TableIdentifier referencedTable, List<?> referencedColumns, boolean uniqueReference) {
                String property = foreignKeyToInverseName.get(keyname);
                if(property==null) {
                    return super.foreignKeyToCollectionName(keyname, fromTable, fromColumns, referencedTable, referencedColumns, uniqueReference);
                }
                else {
                    return property;
                }
            }

            public boolean excludeForeignKeyAsCollection(
                    String keyname,
                    TableIdentifier fromTable,
                    List<?> fromColumns,
                    TableIdentifier referencedTable,
                    List<?> referencedColumns) {
                Boolean bool = foreignKeyInverseExclude.get(keyname);
                return Objects.requireNonNullElseGet(bool, () -> super.excludeForeignKeyAsCollection(keyname, fromTable, fromColumns,
                        referencedTable, referencedColumns));
            }

            public boolean excludeForeignKeyAsManytoOne(
                    String keyname,
                    TableIdentifier fromTable,
                    List<?> fromColumns, TableIdentifier
                            referencedTable,
                    List<?> referencedColumns) {
                Boolean bool = foreignKeyToOneExclude.get(keyname);
                return Objects.requireNonNullElseGet(bool, () -> super.excludeForeignKeyAsManytoOne(keyname, fromTable, fromColumns,
                        referencedTable, referencedColumns));
            }


            public AssociationInfo foreignKeyToInverseAssociationInfo(ForeignKey foreignKey) {
                AssociationInfo fkei = foreignKeyToInverseEntityInfo.get(foreignKey.getName());
                if(fkei!=null) {
                    return fkei;
                }
                else {
                    return super.foreignKeyToInverseAssociationInfo(foreignKey);
                }
            }

            public AssociationInfo foreignKeyToAssociationInfo(ForeignKey foreignKey) {
                AssociationInfo fkei = foreignKeyToEntityInfo.get(foreignKey.getName());
                if(fkei!=null) {
                    return fkei;
                }
                else {
                    return super.foreignKeyToAssociationInfo(foreignKey);
                }
            }
        };
    }

    protected Map<String, MetaAttribute> columnToMetaAttributes(TableIdentifier tableIdentifier, String column) {
        MultiValuedMap<String, SimpleMetaAttribute> specific = columnMetaAttributes.get( new TableColumnKey(tableIdentifier, column) );
        if(specific!=null && !specific.isEmpty()) {
            return toMetaAttributes(specific);
        }

        return null;
    }

    // TODO: optimize
    protected Map<String,MetaAttribute> tableToMetaAttributes(TableIdentifier identifier) {
        MultiValuedMap<String, SimpleMetaAttribute> specific = tableMetaAttributes.get( identifier );
        if(specific!=null && !specific.isEmpty()) {
            return toMetaAttributes(specific);
        }
        MultiValuedMap<String, SimpleMetaAttribute> general = findGeneralAttributes( identifier );
        if(general!=null && !general.isEmpty()) {
            return toMetaAttributes(general);
        }

        return null;

		/* inheritance not defined yet
		if(specific==null) { specific = Collections.EMPTY_MAP; }
		if(general==null) { general = Collections.EMPTY_MAP; }

		MultiMap map = MetaAttributeBinder.mergeMetaMaps( specific, general );
		*/
		/*
		if(map!=null && !map.isEmpty()) {
			return toMetaAttributes(null, map);
		}
		else {
			return null;
		}
		*/
    }

    private MultiValuedMap<String, SimpleMetaAttribute> findGeneralAttributes(TableIdentifier identifier) {
        for ( TableFilter tf : tableFilters ) {
            MultiValuedMap<String, SimpleMetaAttribute> value = tf.getMetaAttributes( identifier );
            if ( value != null ) {
                return value;
            }
        }
        return null;
    }

    private Map<String, MetaAttribute> toMetaAttributes(MultiValuedMap<String, SimpleMetaAttribute> mvm) {
        Map<String, MetaAttribute> result = new HashMap<>();
        for (MapIterator<String, SimpleMetaAttribute> iter = mvm.mapIterator(); iter.hasNext();) {
            String key = iter.next();
            Collection<SimpleMetaAttribute> values = mvm.get(key);
            result.put(key, MetaAttributeHelper.toRealMetaAttribute(key, values));
        }
        return result;
    }

    /**
     * @deprecated Use {@link #getReverseEngineeringStrategy(RevengStrategy)}
     *     with {@code delegate=null} to explicitly ignore the delegate.
     */
    @Deprecated
    public RevengStrategy getReverseEngineeringStrategy() {
        return getReverseEngineeringStrategy(null);
    }

    public void addTable(Table table, String wantedClassName) {
        for (ForeignKey fk : table.getForeignKeyCollection()) {
            TableIdentifier identifier = TableIdentifier.create(fk.getReferencedTable());
            List<ForeignKey> existing = foreignKeys.computeIfAbsent( identifier, k -> new ArrayList<>() );
            existing.add( fk );
        }

        if(StringHelper.isNotEmpty(wantedClassName)) {
            TableIdentifier tableIdentifier = TableIdentifier.create(table);
            String className = wantedClassName;
	/* If wantedClassName specifies a package, it is given by
		<hibernate-reverse-engineering><table class="xxx"> config so do no more. */
            if(!wantedClassName.contains(".")) {
		/* Now look for the package name specified by
		<hibernate-reverse-engineering><table-filter package="xxx"> config. */
                String packageName = getPackageName(tableIdentifier);
                if (packageName != null && !packageName.isBlank()) {
                    className = packageName + "." + wantedClassName;
                }
            }
            tableToClassName.put(tableIdentifier, className);
        }
    }

    static class TableColumnKey {
        private final TableIdentifier query;
        private final String name;

        TableColumnKey(TableIdentifier query, String name){
            this.query = query;
            this.name = name;
        }

        @Override
        public int hashCode() {
            final int prime = 29;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((query == null) ? 0 : query.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            TableColumnKey other = (TableColumnKey) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            if (query == null) {
                return other.query == null;
            }
            else return query.equals( other.query );
        }

    }

    public void setTypeNameForColumn(TableIdentifier identifier, String columnName, String type) {
        if(StringHelper.isNotEmpty(type)) {
            typeForColumn.put(new TableColumnKey(identifier, columnName), type);
        }
    }

    public void setExcludedColumn(TableIdentifier tableIdentifier, String columnName) {
        excludedColumns.add(new TableColumnKey(tableIdentifier, columnName));
    }

    public void setPropertyNameForColumn(TableIdentifier identifier, String columnName, String property) {
        if(StringHelper.isNotEmpty(property)) {
            propertyNameForColumn.put(new TableColumnKey(identifier, columnName), property);
        }
    }

    public void addTableIdentifierStrategy(Table table, String identifierClass, Properties params) {
        if(identifierClass!=null) {
            final TableIdentifier tid = TableIdentifier.create(table);
            identifierStrategyForTable.put(tid, identifierClass);
            identifierPropertiesForTable.put(tid, params);
        }
    }

    public void addPrimaryKeyNamesForTable(Table table, List<String> boundColumnNames, String propertyName, String compositeIdName) {
        TableIdentifier tableIdentifier = TableIdentifier.create(table);
        if(boundColumnNames!=null && !boundColumnNames.isEmpty()) {
            primaryKeyColumnsForTable.put(tableIdentifier, boundColumnNames);
        }
        if(StringHelper.isNotEmpty(propertyName)) {
            propertyNameForPrimaryKey.put(tableIdentifier, propertyName);
        }
        if(StringHelper.isNotEmpty(compositeIdName)) {
            compositeIdNameForTable.put(tableIdentifier, compositeIdName);
        }
    }

	/*public String getCatalog(String string) {
		return string==null?defaultCatalog:string;
	}*/

	/*public String getSchema(String string) {
		return string==null?defaultSchema:string;
	}*/

    public void addSchemaSelection(SchemaSelection schemaSelection) {
        schemaSelections.add(schemaSelection);
    }

    /**
     * Both sides of the FK are important,
     * the owning side can generate a toOne (ManyToOne or OneToOne), we call this side foreignKeyToOne
     * the inverse side can generate a OneToMany OR a OneToOne (in case we have a pure bidirectional OneToOne, we call this side foreignKeyToInverse
     */
    public void addForeignKeyInfo(String constraintName, String toOneProperty, Boolean excludeToOne, String inverseProperty, Boolean excludeInverse, AssociationInfo associationInfo, AssociationInfo inverseAssociationInfo) {
        if(StringHelper.isNotEmpty(toOneProperty)) {
            foreignKeyToOneName.put(constraintName, toOneProperty);
        }
        if(StringHelper.isNotEmpty(inverseProperty)) {
            foreignKeyToInverseName.put(constraintName, inverseProperty);
        }
        if(excludeInverse!=null) {
            foreignKeyInverseExclude.put(constraintName, excludeInverse);
        }
        if(excludeToOne!=null) {
            foreignKeyToOneExclude.put(constraintName, excludeToOne);
        }
        if(associationInfo!=null) {
            foreignKeyToEntityInfo.put(constraintName, associationInfo);
        }
        if(inverseAssociationInfo!=null) {
            foreignKeyToInverseEntityInfo.put(constraintName, inverseAssociationInfo);
        }

    }

    public void addMetaAttributeInfo(Table table, MultiValuedMap<String, SimpleMetaAttribute> map) {
        if(map!=null && !map.isEmpty()) {
            tableMetaAttributes.put(TableIdentifier.create(table), map);
        }

    }

    public void addMetaAttributeInfo(
            TableIdentifier tableIdentifier,
            String name,
            MultiValuedMap<String, SimpleMetaAttribute> map) {
        if(map!=null && !map.isEmpty()) {
            columnMetaAttributes.put(new TableColumnKey( tableIdentifier, name ), map);
        }

    }

/*It is not possible to match a table on TableMapper alone because RootClassBinder.bind()
calls nullifyDefaultCatalogAndSchema(table) before doing this TableToClassName lookup.
So only use the table name for initial matching, and catalog or schema names when they
are not null.
*/

    private static class TableToClassName {
        Map<String, TableMapper> map = new HashMap<>();

        private String get(TableIdentifier tableIdentifier) {
            TableMapper mapper = map.get(tableIdentifier.getName());
            if (mapper != null) {
                if (mapper.catalog == null || tableIdentifier.getCatalog() == null ||
                        mapper.catalog.equals(tableIdentifier.getCatalog())){
                    if (mapper.schema == null || tableIdentifier.getSchema() == null ||
                            mapper.schema.equals(tableIdentifier.getSchema())){
                        if ( mapper.packageName.isEmpty() ) {
                            return mapper.className;
                        }
                        else {
                            return  mapper.packageName + "." + mapper.className;
                        }
                    }
                }
            }
            return null;
        }

        private void put(TableIdentifier tableIdentifier, String wantedClassName) {
            TableMapper tableMapper = new TableMapper(
                    tableIdentifier.getCatalog(),
                    tableIdentifier.getSchema(),
                    wantedClassName );
            map.put(tableIdentifier.getName(), tableMapper);
        }
    }

    private static class TableMapper {
        String catalog;
        String schema;
        String className;
        String packageName;

        private TableMapper(String catalog, String schema, String wantedClassName) {
            this.catalog = catalog;
            this.schema = schema;
            if (wantedClassName.contains(".")) {
                int nameStartPos = wantedClassName.lastIndexOf(".");
                this.className = wantedClassName.substring(nameStartPos+1);
                this.packageName = wantedClassName.substring(0, nameStartPos);
            }
            else {
                this.className = wantedClassName;
                this.packageName = "";
            }
        }
    }
}
