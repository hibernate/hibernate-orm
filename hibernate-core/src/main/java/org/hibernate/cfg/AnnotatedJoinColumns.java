package org.hibernate.cfg;

import org.hibernate.AnnotationException;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitPrimaryKeyJoinColumnNameSource;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;

import java.util.Locale;

import static org.hibernate.cfg.BinderHelper.isEmptyOrNullAnnotationValue;
import static org.hibernate.internal.util.StringHelper.isQuoted;

/**
 * A list of {@link jakarta.persistence.JoinColumn}s that form a single join
 * condition, similar in concept to {@link jakarta.persistence.JoinColumns},
 * but not every instance of this class corresponds to an explicit annotation
 * in the Java code.
 *
 * @author Gavin King
 */
public class AnnotatedJoinColumns {
    private AnnotatedJoinColumn[] columns;
    private PropertyHolder propertyHolder;
    private MetadataBuildingContext buildingContext;

    //TODO: do we really need to hang so many strings off this class?
    private String mappedBy;
    private String mappedByPropertyName; //property name on the owning side if any
    private String mappedByTableName; //table name on the mapped by side if any
    private String mappedByEntityName;
    private boolean elementCollection;
    private String manyToManyOwnerSideEntityName;

    AnnotatedJoinColumns() {
        mappedBy = "";
    }

    public AnnotatedJoinColumn[] getColumns() {
        return columns;
    }

    void setColumns(AnnotatedJoinColumn[] columns) {
        this.columns = columns;
        if ( columns != null ) {
            for ( AnnotatedJoinColumn column : columns ) {
                column.setParent( this );
            }
        }
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
    }

    /**
     * @return true if the association mapping annotation did specify
     *         {@link jakarta.persistence.OneToMany#mappedBy() mappedBy},
     *         meaning that this {@code @JoinColumn} mapping belongs to an
     *         unowned many-valued association.
     */
    public boolean hasMappedBy() {
        return !isEmptyOrNullAnnotationValue( getMappedBy() );
    }

    public String getMappedByEntityName() {
        return mappedByEntityName;
    }

    public String getMappedByPropertyName() {
        return mappedByPropertyName;
    }

    public String getMappedByTableName() {
        return mappedByTableName;
    }

    public PropertyHolder getPropertyHolder() {
        return propertyHolder;
    }

    public MetadataBuildingContext getBuildingContext() {
        return buildingContext;
    }

    public static AnnotatedJoinColumns fromColumns(
            AnnotatedJoinColumn[] columns,
            String mappedBy,
            PropertyHolder propertyHolder,
            MetadataBuildingContext context) {
        final AnnotatedJoinColumns joinColumns = new AnnotatedJoinColumns();
        joinColumns.buildingContext = context;
        joinColumns.propertyHolder = propertyHolder;
        joinColumns.setColumns( columns );
        joinColumns.mappedBy = mappedBy;
        return joinColumns;
    }

    public boolean isElementCollection() {
        return elementCollection;
    }

    public void setElementCollection(boolean elementCollection) {
        this.elementCollection = elementCollection;
    }

    public void setManyToManyOwnerSideEntityName(String entityName) {
        manyToManyOwnerSideEntityName = entityName;
    }

    public String getManyToManyOwnerSideEntityName() {
        return manyToManyOwnerSideEntityName;
    }

    public void setMappedBy(String entityName, String logicalTableName, String mappedByProperty) {
        mappedByEntityName = entityName;
        mappedByTableName = logicalTableName;
        mappedByPropertyName = mappedByProperty;
    }

    String buildDefaultColumnName(PersistentClass referencedEntity, String logicalReferencedColumn) {
        final InFlightMetadataCollector collector = buildingContext.getMetadataCollector();
        final Database database = collector.getDatabase();
        final MetadataBuildingOptions options = buildingContext.getBuildingOptions();
        final ImplicitNamingStrategy implicitNamingStrategy = options.getImplicitNamingStrategy();
        final PhysicalNamingStrategy physicalNamingStrategy = options.getPhysicalNamingStrategy();

        boolean mappedBySide = getMappedByTableName() != null || getMappedByPropertyName() != null;
        boolean ownerSide = getPropertyName() != null;
        boolean isRefColumnQuoted = isQuoted( logicalReferencedColumn );

        Identifier columnIdentifier;
        if ( mappedBySide ) {
            // NOTE : While it is completely misleading here to allow for the combination
            //		of a "JPA ElementCollection" to be mappedBy, the code that uses this
            // 		class relies on this behavior for handling the inverse side of
            // 		many-to-many mappings
            columnIdentifier = implicitNamingStrategy.determineJoinColumnName(
                    new ImplicitJoinColumnNameSource() {
                        final AttributePath attributePath = AttributePath.parse( getMappedByPropertyName() );
                        final ImplicitJoinColumnNameSource.Nature implicitNamingNature = getImplicitNature();

                        private final EntityNaming entityNaming = new EntityNaming() {
                            @Override
                            public String getClassName() {
                                return referencedEntity.getClassName();
                            }

                            @Override
                            public String getEntityName() {
                                return referencedEntity.getEntityName();
                            }

                            @Override
                            public String getJpaEntityName() {
                                return referencedEntity.getJpaEntityName();
                            }
                        };

                        private final Identifier referencedTableName = database.toIdentifier( getMappedByTableName() );

                        @Override
                        public Nature getNature() {
                            return implicitNamingNature;
                        }

                        @Override
                        public EntityNaming getEntityNaming() {
                            return entityNaming;
                        }

                        @Override
                        public AttributePath getAttributePath() {
                            return attributePath;
                        }

                        @Override
                        public Identifier getReferencedTableName() {
                            return referencedTableName;
                        }

                        @Override
                        public Identifier getReferencedColumnName() {
                            if ( logicalReferencedColumn != null ) {
                                return database.toIdentifier( logicalReferencedColumn );
                            }

                            if ( getMappedByEntityName() == null || getMappedByPropertyName() == null ) {
                                return null;
                            }

                            final Property mappedByProperty = collector.getEntityBinding( getMappedByEntityName() )
                                    .getProperty( getMappedByPropertyName() );
                            final SimpleValue value = (SimpleValue) mappedByProperty.getValue();
                            if ( value.getSelectables().isEmpty() ) {
                                throw new AnnotationException(
                                        String.format(
                                                Locale.ENGLISH,
                                                "Association '%s' is 'mappedBy' a property '%s' of entity '%s' with no columns",
                                                propertyHolder.getPath(),
                                                getMappedByPropertyName(),
                                                getMappedByEntityName()
                                        )
                                );
                            }
                            final Selectable selectable = value.getSelectables().get(0);
                            if ( !(selectable instanceof Column) ) {
                                throw new AnnotationException(
                                        String.format(
                                                Locale.ENGLISH,
                                                "Association '%s' is 'mappedBy' a property '%s' of entity '%s' which maps to a formula",
                                                propertyHolder.getPath(),
                                                getMappedByPropertyName(),
                                                propertyHolder.getPath()
                                        )
                                );
                            }
                            if ( value.getSelectables().size()>1 ) {
                                throw new AnnotationException(
                                        String.format(
                                                Locale.ENGLISH,
                                                "Association '%s' is 'mappedBy' a property '%s' of entity '%s' with multiple columns",
                                                propertyHolder.getPath(),
                                                getMappedByPropertyName(),
                                                propertyHolder.getPath()
                                        )
                                );
                            }
                            return database.toIdentifier( ( (Column) selectable ).getQuotedName() );
                        }

                        @Override
                        public MetadataBuildingContext getBuildingContext() {
                            return buildingContext;
                        }
                    }
            );

            //one element was quoted so we quote
            if ( isRefColumnQuoted || isQuoted( getMappedByTableName() ) ) {
                columnIdentifier = Identifier.quote( columnIdentifier );
            }
        }
        else if ( ownerSide ) {
            final String logicalTableName = collector.getLogicalTableName( referencedEntity.getTable() );

            columnIdentifier = implicitNamingStrategy.determineJoinColumnName(
                    new ImplicitJoinColumnNameSource() {
                        final ImplicitJoinColumnNameSource.Nature implicitNamingNature = getImplicitNature();

                        private final EntityNaming entityNaming = new EntityNaming() {
                            @Override
                            public String getClassName() {
                                return referencedEntity.getClassName();
                            }

                            @Override
                            public String getEntityName() {
                                return referencedEntity.getEntityName();
                            }

                            @Override
                            public String getJpaEntityName() {
                                return referencedEntity.getJpaEntityName();
                            }
                        };

                        private final AttributePath attributePath = AttributePath.parse( getPropertyName() );
                        private final Identifier referencedTableName = database.toIdentifier( logicalTableName );
                        private final Identifier referencedColumnName = database.toIdentifier( logicalReferencedColumn );

                        @Override
                        public Nature getNature() {
                            return implicitNamingNature;
                        }

                        @Override
                        public EntityNaming getEntityNaming() {
                            return entityNaming;
                        }

                        @Override
                        public AttributePath getAttributePath() {
                            return attributePath;
                        }

                        @Override
                        public Identifier getReferencedTableName() {
                            return referencedTableName;
                        }

                        @Override
                        public Identifier getReferencedColumnName() {
                            return referencedColumnName;
                        }

                        @Override
                        public MetadataBuildingContext getBuildingContext() {
                            return buildingContext;
                        }
                    }
            );

            // HHH-11826 magic. See Ejb3Column and the HHH-6005 comments
            if ( columnIdentifier.getText().contains( "_collection&&element_" ) ) {
                columnIdentifier = Identifier.toIdentifier(
                        columnIdentifier.getText().replace( "_collection&&element_", "_" ),
                        columnIdentifier.isQuoted()
                );
            }

            //one element was quoted so we quote
            if ( isRefColumnQuoted || isQuoted( logicalTableName ) ) {
                columnIdentifier = Identifier.quote( columnIdentifier );
            }
        }
        else {
            final Identifier logicalTableName = database.toIdentifier(
                    collector.getLogicalTableName( referencedEntity.getTable() )
            );

            // is an intra-entity hierarchy table join so copy the name by default
            columnIdentifier = implicitNamingStrategy.determinePrimaryKeyJoinColumnName(
                    new ImplicitPrimaryKeyJoinColumnNameSource() {
                        @Override
                        public MetadataBuildingContext getBuildingContext() {
                            return buildingContext;
                        }

                        @Override
                        public Identifier getReferencedTableName() {
                            return logicalTableName;
                        }

                        @Override
                        public Identifier getReferencedPrimaryKeyColumnName() {
                            return database.toIdentifier( logicalReferencedColumn );
                        }
                    }
            );

            if ( !columnIdentifier.isQuoted() && ( isRefColumnQuoted || logicalTableName.isQuoted() ) ) {
                columnIdentifier = Identifier.quote( columnIdentifier );
            }
        }

        return physicalNamingStrategy.toPhysicalColumnName( columnIdentifier, database.getJdbcEnvironment() )
                .render( database.getJdbcEnvironment().getDialect() );
    }

    private String getPropertyName() {
        //TODO: very lame, we should know this!
        return columns[0].getPropertyName();
    }

    /**
     * @deprecated this is not a column-level setting, so it's better to
     *             do this work somewhere else
     */
    @Deprecated
    private ImplicitJoinColumnNameSource.Nature getImplicitNature() {
        if ( getPropertyHolder().isEntity() ) {
            return ImplicitJoinColumnNameSource.Nature.ENTITY;
        }
        else if ( isElementCollection() ) {
            return ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION;
        }
        else {
            return ImplicitJoinColumnNameSource.Nature.ENTITY_COLLECTION;
        }
    }
}
