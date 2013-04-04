package org.hibernate.ejb.criteria.jpaMapMode;

import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.dialect.*;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.mapping.*;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

/**
 * Creates our SessionFactory. Creates a Hibernate Configuration based on the Metamodel and binds it to
 * the tables in the MetamodelMapping for the type of database we're using.
 */
public class DocumentSessionFactoryBean {
    private MetaModelMapping metaModelMapping;
    private DataSource dataSource;

    /**
     * Creates a sessionFactory for this MetaModelMapping and DataSource and Database type.
     *
     * @deprecated use getEntityManagerFactory() instead.
     */
    public SessionFactory getObject() {
        Configuration configuration = createConfiguration();
        DatasourceConnectionProviderImpl connectionProvider = new DatasourceConnectionProviderImpl();
        connectionProvider.setDataSource(dataSource);
        connectionProvider.configure(Collections.EMPTY_MAP);

        ServiceRegistry serviceRegistry = createServiceRegistry(configuration);

        return configuration
                .buildSessionFactory(serviceRegistry);
    }

    protected ServiceRegistry createServiceRegistry(final Configuration configuration)
    {
        DatasourceConnectionProviderImpl connectionProvider = new
            DatasourceConnectionProviderImpl();
        connectionProvider.setDataSource(dataSource);
        connectionProvider.configure(Collections.EMPTY_MAP);

        return new ServiceRegistryBuilder().applySettings(configuration.getProperties())
            .addService(ConnectionProvider.class, connectionProvider).buildServiceRegistry();
    }

    public void updateSchema(Configuration cfg)
    {
        ServiceRegistry serviceRegistry = createServiceRegistry(cfg);
        new SchemaUpdate(serviceRegistry, cfg).execute(true, true);
    }

    /**
     * Creates a DocumentManagerFactory for the MetaModelMapping, dataSource, and database type.
     *
     * @return The DocumentManagerFactory.
     */
    public DocumentManagerFactory getEntityManagerFactory() {
        Configuration configuration = createConfiguration();
        DatasourceConnectionProviderImpl connectionProvider = new DatasourceConnectionProviderImpl();
        connectionProvider.setDataSource(dataSource);
        connectionProvider.configure(Collections.EMPTY_MAP);

        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .addService(ConnectionProvider.class, connectionProvider)
                .buildServiceRegistry();

        configuration.buildSessionFactory(serviceRegistry);

        return new DocumentManagerFactory(PersistenceUnitTransactionType.RESOURCE_LOCAL, true,
                null, configuration, serviceRegistry, metaModelMapping.getMetaModel().getName());
    }

    /**
     * Creates our Hibernate configuration. Sets the relevant settings and maps the MetamodelMapping to
     * Hibernate.
     */
    public Configuration createConfiguration() {
        Configuration config = new Configuration();
        config.setProperty("hibernate.dialect", getDatabaseDialect().getName());
        config.setProperty("hibernate.current_session_context_class",
                ThreadLocalSessionContext.class.getName());
        config.setProperty("hibernate.show_sql", "true");
        config.setProperty(AvailableSettings.JPA_METAMODEL_POPULATION, "enabled");

        Mappings mappings = config.createMappings();

        Map<DocumentTableMapping, PersistentDocumentClass> documentTableMappingPersistentDocumentClassMap = new HashMap<DocumentTableMapping, PersistentDocumentClass>();
        Map<Document, PersistentDocumentClass> documentPersistentDocumentClassMap = new HashMap<Document, PersistentDocumentClass>();

        // map ALL the documents! (to PersistentClasses)
        for (Entry<Document, DocumentTableMapping> entry : metaModelMapping
                .getDocumentTableMappings().entrySet()) {
            PersistentDocumentClass persistentClass = createPersistentClass(mappings,
                    entry.getValue());
            mappings.addClass(persistentClass);
            documentTableMappingPersistentDocumentClassMap.put(entry.getValue(), persistentClass);
            documentPersistentDocumentClassMap.put(entry.getKey(), persistentClass);
        }

        // map relationships to synthesized properties on our PersistentClasses.
        for (Entry<Relationship, RelationshipColumnMapping> entry :
                metaModelMapping.getRelationshipColumnMappings().entrySet()) {
            RelationshipColumnMapping relationshipColumnMapping = entry.getValue();

            PersistentDocumentClass persistentDocumentClassWithColumn = documentTableMappingPersistentDocumentClassMap.get(relationshipColumnMapping.getDocumentTableMapping());
            addColumnSideProperty(mappings, relationshipColumnMapping, persistentDocumentClassWithColumn);

            PersistentDocumentClass persistentDocumentClass = documentPersistentDocumentClassMap.get(
                    relationshipColumnMapping.getRelationship().getDocument(relationshipColumnMapping.getSide().oppositeSide()));
            addNonColumnSideProperty(mappings, relationshipColumnMapping, persistentDocumentClass, persistentDocumentClassWithColumn);
        }

        return config;
    }

    /**
     * Add the "other" side of a relationship to the Hibernate configuration.  This allows the user to query for the "many"
     * side of many-to-one relationships, or the other side of one-to-one relationships. This may only be called
     * after the side of the relationship with the column has been created.
     *
     * @param mappings                  The hibernate mappings
     * @param relationshipColumnMapping The RCM
     * @param persistentDocumentClass   The class that needs the property
     * @param persistentDocumentClassWithColumn
     *                                  The class that maps the foreign key column
     * @return The Hibernate property for the non-column side.
     */
    protected HibernateRelationshipProperty addNonColumnSideProperty(Mappings mappings, RelationshipColumnMapping relationshipColumnMapping, PersistentDocumentClass persistentDocumentClass, PersistentDocumentClass persistentDocumentClassWithColumn) {
        Relationship.Side side = relationshipColumnMapping.getSide().oppositeSide();

        HibernateRelationshipProperty hibernateProperty = new HibernateRelationshipProperty(relationshipColumnMapping);

        final String name = relationshipColumnMapping.getRelationship().getRoleName(side);
        hibernateProperty
                .setName(name);

        hibernateProperty.setOptional(isOptional(relationshipColumnMapping.getRelationship().getMultiplicity(side))); // dunno about this one
        hibernateProperty.setLazy(false);
        hibernateProperty.setPersistentClass(persistentDocumentClass);

        Set set = new Set(mappings, persistentDocumentClass);
        set.setInverse(true);
        set.setLazy(false);
        set.setFetchMode(FetchMode.JOIN);
        set.setRole(name);
        set.setCollectionTable(persistentDocumentClassWithColumn.getTable());

        KeyValue keyValue = getKeyValueFor(persistentDocumentClassWithColumn, relationshipColumnMapping.getRelationship().getRoleName(relationshipColumnMapping.getSide()));
        set.setKey(keyValue);
        OneToMany collectionValue = new OneToMany(mappings, persistentDocumentClass);
        collectionValue.setAssociatedClass(persistentDocumentClassWithColumn);
        collectionValue.setReferencedEntityName(persistentDocumentClassWithColumn.getEntityName());
        set.setElement(collectionValue);

        hibernateProperty.setValue(set);
        mappings.addCollection(set);

        persistentDocumentClass.addRelationship(relationshipColumnMapping);
        persistentDocumentClass.addProperty(hibernateProperty);

        return hibernateProperty;
    }

    /**
     * Find the KeyValue for the foreign key column.
     */
    protected KeyValue getKeyValueFor(PersistentDocumentClass persistentDocumentClassWithColumn, String roleName) {
        HibernateRelationshipProperty property = (HibernateRelationshipProperty) persistentDocumentClassWithColumn.getProperty(roleName);
        return (KeyValue) property.getValue();
    }

    /**
     * This side has the foreign key on it. Map the foreign key column into the Hibernate configuration.
     *
     * @param mappings                  The hibernate mappings
     * @param relationshipColumnMapping The RCM
     * @param persistentDocumentClass   The class that needs the property
     * @return The Hibernate property for the non-column side.
     */
    protected HibernateRelationshipProperty addColumnSideProperty(Mappings mappings, RelationshipColumnMapping relationshipColumnMapping, PersistentDocumentClass persistentDocumentClass) {
        org.hibernate.mapping.Table table = persistentDocumentClass.getTable();

        HibernateRelationshipProperty hibernateProperty = new HibernateRelationshipProperty(relationshipColumnMapping);

        final Relationship.Side side = relationshipColumnMapping.getSide();
        hibernateProperty
                .setName(relationshipColumnMapping.getRelationship().getRoleName(side));
        hibernateProperty.setOptional(isOptional(relationshipColumnMapping.getRelationship().getMultiplicity(side))); // everything's optional to us

        ToOne value = new ManyToOne(mappings, table);      // TODO fix this for different relationship types
        org.hibernate.mapping.Column hibernateColumn = new org.hibernate.mapping.Column(relationshipColumnMapping.getColumn().getName());
        value.addColumn(hibernateColumn);
        table.addColumn(hibernateColumn);

        String type = relationshipColumnMapping.getRelationship().getDocument(side.oppositeSide()).getName();
        value.setTypeName(type);  // is this correct? or is there a type for object?
        value.setReferencedEntityName(type);
        value.setFetchMode(FetchMode.JOIN);

        hibernateProperty.setValue(value);

        persistentDocumentClass.addRelationship(relationshipColumnMapping);
        persistentDocumentClass.addProperty(hibernateProperty);

        return hibernateProperty;
    }

    private boolean isOptional(Multiplicity multiplicity) {
        return (multiplicity == Multiplicity.ZERO_OR_ONE) || (multiplicity == Multiplicity.ZERO_OR_MORE);
    }

    /**
     * Given our logical database type, find the appropriate Hibernate dialect. Could be done with a Map, but whatever.
     *
     * @return The dialect class
     */
    protected Class<? extends Dialect> getDatabaseDialect() {
        return H2Dialect.class;
    }

    /**
     * Create a PersistentDocumentClass that represents how Hibernate thinks of a DocumentTableMapping.
     *
     * @param mappings             Hibernate mappings
     * @param documentTableMapping What we need to map to Hibernate
     * @return The PersistentDocumentClass
     */
    protected PersistentDocumentClass createPersistentClass(Mappings mappings,
                                                            DocumentTableMapping documentTableMapping) {

        PersistentDocumentClass persistentClass = new PersistentDocumentClass(
                documentTableMapping);
        org.hibernate.mapping.Table table = createTable(documentTableMapping, mappings);
        persistentClass.setTable(table);
        persistentClass.addTuplizer(EntityMode.MAP,
                DocumentInstanceTuplizer.class.getName());

        HibernateIdProperty idProperty = createIdProperty(mappings,
                table, documentTableMapping);
        persistentClass.setIdentifierProperty(idProperty);
        persistentClass.setIdentifier((KeyValue) idProperty.getValue());

        for (PropertyColumnMapping propertyColumnMapping : documentTableMapping
                .getPropertyMappings().values()) {
            HibernateDocumentProperty hibernateDocumentProperty = createHibernateProperty(
                    mappings, table, propertyColumnMapping);

            persistentClass.addProperty(hibernateDocumentProperty);
        }

        return persistentClass;
    }

    /**
     * Creates a Hibernate representation of an ID property. Assumes all document IDs work the same way.
     * Should probably be mapped to a binary representation for a GUID (Hibernate has a built-in type for this, BTW).
     */
    protected HibernateIdProperty createIdProperty(
            Mappings mappings, org.hibernate.mapping.Table table,
            DocumentTableMapping documentTableMapping) {
        HibernateIdProperty idProperty = new HibernateIdProperty(documentTableMapping);
        idProperty.setName(documentTableMapping.getTable().getIdColumnName());
        SimpleValue value = new SimpleValue(mappings, table);
        value.setIdentifierGeneratorStrategy("uuid2");

        org.hibernate.mapping.Column column = new org.hibernate.mapping.Column(documentTableMapping.getTable()
                .getIdColumnName());
        column.setLength(16);
        value.addColumn(column);

        table.getPrimaryKey().addColumn(column);
        table.addColumn(column);

        String type = UUID.class.getName();
        value.setTypeName(type);

        idProperty.setValue(value);

        return idProperty;
    }

    /**
     * Tells Hibernate about a table used to represent a document. All Document tables are assumed to have a
     * single PK.
     */
    protected org.hibernate.mapping.Table createTable(DocumentTableMapping documentTableMapping,
        Mappings mappings) {
        Table mappingTable = documentTableMapping
                .getTable();
        org.hibernate.mapping.Table table = mappings
            .addTable(null, null, mappingTable.getTableName(), null, false);
        PrimaryKey primaryKey = new PrimaryKey();
        table.setPrimaryKey(primaryKey);
        return table;
    }

    /**
     * Represents a PropertyColumnMapping for Hibernate.
     */
    protected HibernateDocumentProperty createHibernateProperty(
            Mappings mappings, org.hibernate.mapping.Table table,
            PropertyColumnMapping propertyColumnMapping) {
        HibernateDocumentProperty hibernateDocumentProperty = new HibernateDocumentProperty(propertyColumnMapping);

        hibernateDocumentProperty
                .setName(propertyColumnMapping.getProperty().getName());
        hibernateDocumentProperty.setOptional(true); // everything's optional to us
        Value value = createValueForProperty(propertyColumnMapping, mappings,
                table);
        hibernateDocumentProperty.setValue(value);
        return hibernateDocumentProperty;
    }

    /**
     * Creates a Hibernate Value object to represent how Hibernate should represent a property value.
     * If we support components (or when we support arbitrary-length strings etc) this will need to be enhanced.
     * This is really simple just to prove out the prototype.
     */
    protected Value createValueForProperty(
            PropertyColumnMapping propertyColumnMapping, Mappings mappings,
        org.hibernate.mapping.Table table) {

        SimpleValue value = new SimpleValue(mappings, table);
        org.hibernate.mapping.Column column = createColumn(propertyColumnMapping);
        table.addColumn(column);
        value.addColumn(column);

        String type = getType(propertyColumnMapping);
        value.setTypeName(type);

        return value;
    }

    /**
     * Maps a PropertyColumnMapping type to a Hibernate type. This will need to mature a bit.
     */
    protected String getType(PropertyColumnMapping propertyColumnMapping) {

        int columnType = propertyColumnMapping.getColumn().getType();
        switch (columnType) {
            case Types.BIGINT:
            case Types.DECIMAL:
            case Types.NUMERIC:
                return "double";
            case Types.VARCHAR:
                return "string";

            default:
                throw new IllegalStateException("unknown column type: "
                        + columnType);
        }
    }

    protected org.hibernate.mapping.Column createColumn(PropertyColumnMapping propertyColumnMapping) {

        return new org.hibernate.mapping.Column(propertyColumnMapping.getColumn().getName());
    }

    public void setMetaModelMapping(MetaModelMapping metaModelMapping) {
        this.metaModelMapping = metaModelMapping;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

}
