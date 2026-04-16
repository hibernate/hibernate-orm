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
package org.hibernate.tool.internal.export.java;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.boot.Metadata;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.JoinedList;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.mapping.Value;
import org.hibernate.tool.internal.reveng.util.EnhancedBasicValue;
import org.hibernate.tool.internal.reveng.util.EnhancedValue;
import org.hibernate.tool.internal.util.AnnotationBuilder;
import org.hibernate.tool.internal.util.IteratorTransformer;
import org.hibernate.tool.internal.util.SkipBackRefPropertyIterator;
import org.hibernate.tool.internal.util.ValueUtil;
import org.hibernate.type.ForeignKeyDirection;

import static org.hibernate.tool.internal.export.java.MetaAttributeConstants.EXTENDS;
import static org.hibernate.tool.internal.export.java.MetaAttributeConstants.IMPLEMENTS;

public class EntityPOJOClass extends BasicPOJOClass {

    private final PersistentClass clazz;

    public EntityPOJOClass(PersistentClass clazz, Cfg2JavaTool cfg) {
        super(clazz, cfg);
        this.clazz = clazz;
        init();
    }

    protected String getMappedClassName() {
        return clazz.getClassName();
    }

    /**
     * @return whatever the class (or interface) extends (null if it does not extend anything)
     */
    public String getExtends() {
        String extendz = "";

        if ( isInterface() ) {
            if ( clazz.getSuperclass() != null ) {
                extendz = clazz.getSuperclass().getClassName();
            }
            if ( clazz.getMetaAttribute( EXTENDS ) != null ) {
                if ( !extendz.isEmpty() ) {
                    extendz += ",";
                }
                extendz += getMetaAsString( EXTENDS, "," );
            }
        }
        else if ( clazz.getSuperclass() != null ) {
            if (!( c2j.getPOJOClass(clazz.getSuperclass()).isInterface() )) {
                extendz = clazz.getSuperclass().getClassName();
            }
        }
        else if ( clazz.getMetaAttribute( EXTENDS ) != null ) {
            extendz = getMetaAsString( EXTENDS, "," );
        }

        return extendz.isEmpty() ? null : extendz;
    }


    public String getImplements() {
        List<String> interfaces = new ArrayList<>();

        //			implement proxy, but NOT if the proxy is the class it self!
        if ( clazz.getProxyInterfaceName() != null && ( !clazz.getProxyInterfaceName().equals( clazz.getClassName() ) ) ) {
            interfaces.add( clazz.getProxyInterfaceName() );
        }

        if ( !isInterface() ) {
            if ( clazz.getSuperclass() != null && c2j.getPOJOClass(clazz.getSuperclass()).isInterface() ) {
                interfaces.add( clazz.getSuperclass().getClassName() );
            }
            if ( clazz.getMetaAttribute( IMPLEMENTS ) != null ) {
                interfaces.addAll( clazz.getMetaAttribute( IMPLEMENTS ).getValues() );
            }
            interfaces.add( Serializable.class.getName() ); // TODO: is this "nice" ? shouldn't it be a user choice ?
        }

        if (!interfaces.isEmpty()) {
            StringBuilder sbuf = new StringBuilder();
            for ( Iterator<String> iter = interfaces.iterator(); iter.hasNext() ; ) {
                //sbuf.append(JavaTool.shortenType(iter.next().toString(), pc.getImports() ) );
                sbuf.append( iter.next() );
                if ( iter.hasNext() ) sbuf.append( "," );
            }
            return sbuf.toString();
        }
        else {
            return null;
        }
    }

    public Iterator<Property> getAllPropertiesIterator() {
        return getAllPropertiesIterator(clazz);
    }


    public Iterator<Property> getAllPropertiesIterator(PersistentClass pc) {
        List<Property> properties = new ArrayList<>();
        List<List<Property>> lists = new ArrayList<>();
        if ( pc.getSuperclass() == null ) {
            // only include identifier for the root class.
            if ( pc.hasIdentifierProperty() ) {
                properties.add( pc.getIdentifierProperty() );
            }
            else if ( pc.hasEmbeddedIdentifier() ) {
                Component embeddedComponent = (Component) pc.getIdentifier();
                lists.add( embeddedComponent.getProperties() );
            }
			/*if(clazz.isVersioned() ) { // version is already in property set
				properties.add(clazz.getVersion() );
			}*/
        }


        //		iterators.add( pc.getPropertyIterator() );
        // Need to skip <properties> element which are defined via "embedded" components
        // Best if we could return an intelligent iterator, but for now we just iterate explicitly.
        List<Property> pl = pc.getProperties();
        for(Property element : pl)
        {
            if ( element.getValue() instanceof Component component
                    && element.getPropertyAccessorName().equals( "embedded" )) {
                properties.addAll(component.getProperties());
            }
            else {
                properties.add(element);
            }
        }

        lists.add(properties);

        return new SkipBackRefPropertyIterator( new JoinedList<>( lists ).iterator() );
    }

    public boolean isComponent() {
        return false;
    }


    public boolean hasIdentifierProperty() {
        return clazz.hasIdentifierProperty() && clazz instanceof RootClass;
    }

    public Property getIdentifierProperty() {
        return clazz.getIdentifierProperty();
    }

    public String generateAnnTableUniqueConstraint() {
        if ( ! ( clazz instanceof Subclass ) ) {
            Table table = clazz.getTable();
            return generateAnnTableUniqueConstraint( table );
        }
        return "";
    }

    protected String generateAnnTableUniqueConstraint(Table table) {
        List<String> cons = new ArrayList<>();
        for (UniqueKey key : table.getUniqueKeys().values()) {
            if (table.hasPrimaryKey() && table.getPrimaryKey().getColumns().equals(key.getColumns())) {
                continue;
            }
            AnnotationBuilder constraint = AnnotationBuilder.createAnnotation( importType("jakarta.persistence.UniqueConstraint") );
            constraint.addQuotedAttributes( "columnNames", new IteratorTransformer<>( key.getColumns().iterator() ) {
                public String transform(Column column) {
                    return column.getName();
                }
            });
            cons.add( constraint.getResult() );
        }

        AnnotationBuilder builder = AnnotationBuilder.createAnnotation( "dummyAnnotation" );
        builder.addAttributes( "dummyAttribute", cons.iterator() );
        String attributeAsString = builder.getAttributeAsString( "dummyAttribute" );
        return attributeAsString==null?"":attributeAsString;
    }


    public String generateAnnIdGenerator() {
        KeyValue identifier = clazz.getIdentifier();
        String strategy;
        Properties properties;
        StringBuffer wholeString = new StringBuffer( "    " );
        if ( identifier instanceof Component ) {

            wholeString.append( AnnotationBuilder.createAnnotation( importType("jakarta.persistence.EmbeddedId") ).getResult());
        }
        else if (identifier instanceof EnhancedBasicValue enhancedBasicValue) {
            strategy = enhancedBasicValue.getIdentifierGeneratorStrategy();
            properties = c2j.getFilteredIdentifierGeneratorProperties(enhancedBasicValue);
            StringBuilder idResult = new StringBuilder();
            AnnotationBuilder builder = AnnotationBuilder.createAnnotation( importType("jakarta.persistence.Id") );
            idResult.append(builder.getResult());
            idResult.append(" ");

            boolean isGenericGenerator = false; //TODO: how to handle generic now??
            if ( !"assigned".equals( strategy ) ) {

                if ( !"native".equals( strategy ) ) {
                    if ( "identity".equals( strategy ) ) {
                        builder.resetAnnotation( importType("jakarta.persistence.GeneratedValue") );
                        builder.addAttribute( "strategy", staticImport("jakarta.persistence.GenerationType", "IDENTITY" ) );
                        idResult.append(builder.getResult());
                    }
                    else if ( "sequence".equals( strategy ) ) {
                        String sequenceGeneratorName = null;
                        if (properties != null) {
                            sequenceGeneratorName = properties.getProperty(
                                    org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM, null );
                        }
                        builder.resetAnnotation( importType("jakarta.persistence.GeneratedValue") )
                                .addAttribute( "strategy", staticImport("jakarta.persistence.GenerationType", "SEQUENCE" ) )
                                .addQuotedAttribute( "generator", clazz.getClassName()+"IdGenerator" );
                        idResult.append(builder.getResult());

                        builder.resetAnnotation( importType("jakarta.persistence.SequenceGenerator") )
                                .addQuotedAttribute( "name", clazz.getClassName()+"IdGenerator" )
                                .addQuotedAttribute( "sequenceName", sequenceGeneratorName );
                        //	TODO HA does not support initialValue and allocationSize
                        wholeString.append( builder.getResult() );
                    }
                    else if ( TableGenerator.class.getName().equals( strategy ) ) {
                        builder.resetAnnotation( importType("jakarta.persistence.GeneratedValue") )
                                .addAttribute( "strategy", staticImport("jakarta.persistence.GenerationType", "TABLE" ) )
                                .addQuotedAttribute( "generator", clazz.getClassName()+"IdGenerator" );
                        idResult.append(builder.getResult());
                        buildAnnTableGenerator( wholeString, properties );
                    }
                    else {
                        isGenericGenerator = true;
                        builder.resetAnnotation( importType("jakarta.persistence.GeneratedValue") );
                        builder.addQuotedAttribute( "generator", clazz.getClassName()+"IdGenerator" );
                        idResult.append(builder.getResult());
                    }
                }
                else {
                    builder.resetAnnotation( importType("jakarta.persistence.GeneratedValue") );
                    idResult.append(builder.getResult());
                }
            }
            if ( isGenericGenerator ) {
                builder.resetAnnotation( importType("org.hibernate.annotations.GenericGenerator") )
                        .addQuotedAttribute( "name", clazz.getClassName()+"IdGenerator" )
                        .addQuotedAttribute( "strategy", strategy);

                List<AnnotationBuilder> params = new ArrayList<>();
                //wholeString.append( "parameters = {  " );
                if ( properties != null ) {
                    Enumeration<?> propNames = properties.propertyNames();
                    while ( propNames.hasMoreElements() ) {

                        String propertyName = (String) propNames.nextElement();
                        AnnotationBuilder parameter = AnnotationBuilder.createAnnotation( importType("org.hibernate.annotations.Parameter") )
                                .addQuotedAttribute( "name", propertyName )
                                .addQuotedAttribute( "value", properties.getProperty( propertyName ) );
                        params.add( parameter );
                    }
                }
                builder.addAttributes( "parameters", params.iterator() );
                wholeString.append(builder.getResult());
            }
            wholeString.append( idResult );
        }
        return wholeString.toString();
    }

    private void buildAnnTableGenerator(StringBuffer wholeString, Properties properties) {

        AnnotationBuilder builder = AnnotationBuilder.createAnnotation( importType("jakarta.persistence.TableGenerator") );
        builder.addQuotedAttribute( "name", clazz.getClassName()+"IdGenerator" );
        builder.addQuotedAttribute( "table", properties.getProperty( "generatorTableName", "hibernate_sequences" ) );
        if (propertyDoesNotHaveDefaultValue( PersistentIdentifierGenerator.CATALOG, properties ) ) {
            builder.addQuotedAttribute( "catalog", properties.getProperty( PersistentIdentifierGenerator.CATALOG, "") );
        }
        if (propertyDoesNotHaveDefaultValue( PersistentIdentifierGenerator.SCHEMA, properties ) ) {
            builder.addQuotedAttribute( "schema", properties.getProperty( PersistentIdentifierGenerator.SCHEMA, "") );
        }
        if (propertyDoesNotHaveDefaultValue( TableGenerator.SEGMENT_VALUE_PARAM, properties ) ) {
            builder.addQuotedAttribute( "pkColumnValue", properties.getProperty( TableGenerator.SEGMENT_VALUE_PARAM, "") );
        }
        if (propertyDoesNotHaveDefaultValue( TableGenerator.INCREMENT_PARAM, properties, "50" ) ) {
            builder.addAttribute( "allocationSize", properties.getProperty( TableGenerator.INCREMENT_PARAM, "50" ) );
        }
        if (propertyDoesNotHaveDefaultValue( TableGenerator.SEGMENT_COLUMN_PARAM, properties ) ) {
            builder.addQuotedAttribute( "pkColumnName", properties.getProperty( TableGenerator.SEGMENT_COLUMN_PARAM, "") );
        }
        if (propertyDoesNotHaveDefaultValue( TableGenerator.VALUE_COLUMN_PARAM, properties) ) {
            builder.addQuotedAttribute( "valueColumnName", properties.getProperty( TableGenerator.VALUE_COLUMN_PARAM, "") );
        }
        wholeString.append(builder.getResult()).append("\n    ");
    }

    private boolean propertyDoesNotHaveDefaultValue(String property, Properties properties) {
        return propertyDoesNotHaveDefaultValue(property, properties, null);
    }

    private boolean propertyDoesNotHaveDefaultValue(String property, Properties properties, String defaultValue) {
        String propertyValue = properties.getProperty(property);
        if (defaultValue == null) {
            return StringHelper.isNotEmpty(propertyValue);
        }
        else {
            return !defaultValue.equals(propertyValue);
        }
    }

    public String generateJoinColumnsAnnotation(Property property, Metadata md) {
        boolean insertable = property.isInsertable();
        boolean updatable = property.isUpdatable();
        Value value = property.getValue();
        int span;
        Iterator<Selectable> selectablesIterator;
        Iterator<Selectable> referencedSelectablesIterator = null;
        if (value instanceof Collection collection) {
            span = collection.getKey().getColumnSpan();
            selectablesIterator = collection.getKey().getSelectables().iterator();
        }
        else {
            span = property.getColumnSpan();
            selectablesIterator = property.getSelectables().iterator();
        }

        if(property.getValue() instanceof ToOne) {
            String referencedEntityName = ((ToOne)property.getValue()).getReferencedEntityName();
            PersistentClass target = md.getEntityBinding(referencedEntityName);
            if(target!=null) {
                referencedSelectablesIterator = target.getKey().getSelectables().iterator();
            }
        }

        StringBuffer annotations = new StringBuffer( "    " );
        if ( span == 1 ) {
            Selectable selectable = selectablesIterator.next();
            buildJoinColumnAnnotation( selectable, null, annotations, insertable, updatable );
        }
        else {
            annotations.append("@").append( importType("jakarta.persistence.JoinColumns") ).append("( { " );
            buildArrayOfJoinColumnAnnotation(selectablesIterator, referencedSelectablesIterator, annotations, insertable, updatable );
            annotations.append( " } )" );
        }
        return annotations.toString();
    }

    private void buildArrayOfJoinColumnAnnotation(
            Iterator<Selectable> columns,
            Iterator<Selectable> referencedColumnsIterator,
            StringBuffer annotations,
            boolean insertable,
            boolean updatable
    ) {
        while ( columns.hasNext() ) {
            Selectable selectable = columns.next();
            Selectable referencedColumn = null;
            if(referencedColumnsIterator!=null) {
                referencedColumn = referencedColumnsIterator.next();
            }

            //TODO formula in multicolumns not supported by annotations
            if (!( selectable.isFormula() )) {
                annotations.append( "\n        " );
                buildJoinColumnAnnotation( selectable, referencedColumn, annotations, insertable, updatable );
                annotations.append( ", " );
            }
        }
        annotations.setLength( annotations.length() - 2 );
    }

    private void buildJoinColumnAnnotation(
            Selectable selectable,
            Selectable referencedColumn,
            StringBuffer annotations,
            boolean insertable,
            boolean updatable) {
        //TODO not supported by HA
        if (!( selectable.isFormula() )) {
            Column column = (Column) selectable;
            annotations.append("@").append( importType("jakarta.persistence.JoinColumn") )
                    .append("(name=\"" ).append( column.getName() ).append( "\"" );
            //TODO handle referenced column name, this is a hard one
            if(referencedColumn!=null) {
                annotations.append(", referencedColumnName=\"" ).append( referencedColumn.getText() ).append( "\"" );
            }

            appendCommonColumnInfo(annotations, column, insertable, updatable);
            //TODO support secondary table
            annotations.append( ")" );
        }
    }

    public String[] getCascadeTypes(Property property) {
        StringTokenizer st =  new StringTokenizer( property.getCascade(), ", ", false );
        List<String> types = new ArrayList<>();
        while ( st.hasMoreElements() ) {
            String element = ( (String) st.nextElement() ).toLowerCase();
            switch (element) {
                case "persist" -> types.add(importType("jakarta.persistence.CascadeType") + ".PERSIST");
                case "merge" -> types.add(importType("jakarta.persistence.CascadeType") + ".MERGE");
                case "delete" -> types.add(importType("jakarta.persistence.CascadeType") + ".REMOVE");
                case "refresh" -> types.add(importType("jakarta.persistence.CascadeType") + ".REFRESH");
                case "all" -> types.add(importType("jakarta.persistence.CascadeType") + ".ALL");
            }
        }
        return types.toArray(new String[0]);
    }

    public String generateManyToOneAnnotation(Property property) {
        return AnnotationBuilder.createAnnotation(importType("jakarta.persistence.ManyToOne"))
                .addAttribute("cascade", getCascadeTypes(property))
                .addAttribute("fetch", getFetchType(property))
                .getResult() + getHibernateCascadeTypeAnnotation(property);
    }

    public boolean isSharedPkBasedOneToOne(OneToOne oneToOne){
        Iterator<Selectable> joinSelectablesIt = oneToOne.getSelectables().iterator();
        Set<Selectable> joinSelectables = new HashSet<>();
        while ( joinSelectablesIt.hasNext() ) {
            joinSelectables.add( joinSelectablesIt.next() );
        }

        if (joinSelectables.isEmpty())
            return false;

        for (Selectable selectable : getIdentifierProperty().getSelectables()) {
            if (!joinSelectables.contains(selectable))
                return false;
        }

        return true;
    }

    public String generateOneToOneAnnotation(Property property, Metadata md) {
        OneToOne oneToOne = (OneToOne)property.getValue();

        boolean pkIsAlsoFk = isSharedPkBasedOneToOne(oneToOne);

        AnnotationBuilder ab = AnnotationBuilder.createAnnotation( importType("jakarta.persistence.OneToOne") )
                .addAttribute( "cascade", getCascadeTypes(property))
                .addAttribute( "fetch", getFetchType(property));

        if ( oneToOne.getForeignKeyType().equals(ForeignKeyDirection.TO_PARENT) ){
            ab.addQuotedAttribute("mappedBy", getOneToOneMappedBy(md, oneToOne));
        }

        StringBuilder buffer = new StringBuilder(ab.getResult());
        buffer.append(getHibernateCascadeTypeAnnotation(property));

        if ( pkIsAlsoFk && oneToOne.getForeignKeyType().equals(ForeignKeyDirection.FROM_PARENT) ){
            AnnotationBuilder ab1 = AnnotationBuilder.createAnnotation( importType("jakarta.persistence.PrimaryKeyJoinColumn") );
            buffer.append(ab1.getResult());
        }

        return buffer.toString();
    }

    public String getHibernateCascadeTypeAnnotation(Property property) {
        StringTokenizer st =  new StringTokenizer( property.getCascade(), ", ", false );
        String cascadeType = null;
        StringBuilder cascade = new StringBuilder();
        while ( st.hasMoreElements() ) {
            String element = ( (String) st.nextElement() ).toLowerCase();
            switch (element) {
                case "all-delete-orphan" -> {
                    if (cascadeType == null) cascadeType = importType("org.hibernate.annotations.CascadeType");
                    cascade.append(cascadeType).append(".ALL").append(", ")
                            .append(cascadeType).append(".DELETE_ORPHAN").append(", ");
                }
                case "delete-orphan" -> {
                    if (cascadeType == null) cascadeType = importType("org.hibernate.annotations.CascadeType");
                    cascade.append(cascadeType).append(".DELETE_ORPHAN").append(", ");
                }
                case "save-update" -> {
                    if (cascadeType == null) cascadeType = importType("org.hibernate.annotations.CascadeType");
                    cascade.append(cascadeType).append(".MERGE").append(", ");
                }
                case "replicate" -> {
                    if (cascadeType == null) cascadeType = importType("org.hibernate.annotations.CascadeType");
                    cascade.append(cascadeType).append(".REPLICATE").append(", ");
                }
                case "lock" -> {
                    if (cascadeType == null) cascadeType = importType("org.hibernate.annotations.CascadeType");
                    cascade.append(cascadeType).append(".LOCK").append(", ");
                }
                case "evict" -> {
                    if (cascadeType == null) cascadeType = importType("org.hibernate.annotations.CascadeType");
                    cascade.append(cascadeType).append(".EVICT").append(", ");
                }
            }
        }
        if ( cascade.length() >= 2 ) {
            String hibernateCascade = importType("org.hibernate.annotations.Cascade");
            cascade.insert(0, "@" + hibernateCascade + "( {");
            cascade.setLength( cascade.length() - 2 );
            cascade.append("} )");
        }
        return cascade.toString();
    }

    public String getFetchType(Property property) {
        Value value = property.getValue();
        String fetchType = importType( "jakarta.persistence.FetchType");
        boolean lazy;
        if ( value instanceof ToOne ) {
            lazy = ( (ToOne) value ).isLazy();
        }
        else if ( value instanceof Collection ) {
            lazy = ( (Collection) value ).isLazy();
        }
        else {
            //we're not collection neither *toone so we are looking for property fetching
            lazy = property.isLazy();
        }
        if ( lazy ) {
            return fetchType + "." + "LAZY";
        }
        else {
            return fetchType + "." + "EAGER";
        }
    }

    public Object getDecoratedObject() {
        return clazz;
    }

    public String generateCollectionAnnotation(Property property, Metadata md) {
        StringBuffer annotation = new StringBuffer();
        Value value = property.getValue();
        if (value instanceof Collection collection) {
            if ( collection.isOneToMany() ) {
                String mappedBy = null;
                AnnotationBuilder ab = AnnotationBuilder.createAnnotation( importType( "jakarta.persistence.OneToMany") );
                ab.addAttribute( "cascade", getCascadeTypes( property ) );
                ab.addAttribute( "fetch", getFetchType (property) );
                if ( collection.isInverse() ) {
                    mappedBy = getOneToManyMappedBy( md, collection );
                    ab.addQuotedAttribute( "mappedBy", mappedBy );
                }
                annotation.append( ab.getResult() );

                if (mappedBy == null) annotation.append("\n").append( generateJoinColumnsAnnotation(property, md) );
            }
            else {
                //TODO do the @OneToMany @JoinTable
                //TODO composite element
                String mappedBy = null;
                AnnotationBuilder ab = AnnotationBuilder.createAnnotation( importType( "jakarta.persistence.ManyToMany") );
                ab.addAttribute( "cascade", getCascadeTypes( property ) );
                ab.addAttribute( "fetch", getFetchType (property) );

                if ( collection.isInverse() ) {
                    mappedBy = getManyToManyMappedBy( md, collection );
                    ab.addQuotedAttribute( "mappedBy", mappedBy );
                }
                annotation.append(ab.getResult());
                if (mappedBy == null) {
                    annotation.append("\n    @");
                    annotation.append( importType( "jakarta.persistence.JoinTable") ).append( "(name=\"" );
                    Table table = collection.getCollectionTable();

                    annotation.append( table.getName() );
                    annotation.append( "\"" );
                    if ( StringHelper.isNotEmpty( table.getSchema() ) ) {
                        annotation.append(", schema=\"").append( table.getSchema() ).append("\"");
                    }
                    if ( StringHelper.isNotEmpty( table.getCatalog() ) ) {
                        annotation.append(", catalog=\"").append( table.getCatalog() ).append("\"");
                    }
                    String uniqueConstraint = generateAnnTableUniqueConstraint(table);
                    if (!uniqueConstraint.isEmpty()) {
                        annotation.append(", uniqueConstraints=").append(uniqueConstraint);
                    }
                    annotation.append( ", joinColumns = { ");
                    buildArrayOfJoinColumnAnnotation(
                            collection.getKey().getSelectables().iterator(),
                            null,
                            annotation,
                            property.isInsertable(),
                            property.isUpdatable()
                    );
                    annotation.append( " }");
                    annotation.append( ", inverseJoinColumns = { ");
                    buildArrayOfJoinColumnAnnotation(
                            collection.getElement().getSelectables().iterator(),
                            null,
                            annotation,
                            property.isInsertable(),
                            property.isUpdatable()
                    );
                    annotation.append( " }");
                    annotation.append(")");
                }

            }
            String hibernateCascade = getHibernateCascadeTypeAnnotation( property );
            if (!hibernateCascade.isEmpty()) annotation.append("\n    ").append(hibernateCascade);
        }
        return annotation.toString();
    }

    private String getManyToManyMappedBy(Metadata md, Collection collection) {
        String mappedBy;
        Iterator<Selectable> joinColumnsIt = collection.getKey().getSelectables().iterator();
        Set<Selectable> joinColumns = new HashSet<>();
        while ( joinColumnsIt.hasNext() ) {
            joinColumns.add( joinColumnsIt.next() );
        }
        ManyToOne manyToOne = (ManyToOne) collection.getElement();
        PersistentClass pc = md.getEntityBinding(manyToOne.getReferencedEntityName());
        Iterator<Property> properties = pc.getProperties().iterator();
        //TODO we should check the table too
        boolean isOtherSide = false;
        mappedBy = "unresolved";
        while ( ! isOtherSide && properties.hasNext() ) {
            Property collectionProperty = properties.next();
            Value collectionValue = collectionProperty.getValue();
            if (collectionValue instanceof Collection realCollectionValue) {
                if ( ! realCollectionValue.isOneToMany() ) {
                    if ( joinColumns.size() == realCollectionValue.getElement().getColumnSpan() ) {
                        isOtherSide = true;
                        for (Selectable selectable : realCollectionValue.getElement().getSelectables()) {
                            if (!joinColumns.contains(selectable)) {
                                isOtherSide = false;
                                break;
                            }
                        }
                        if (isOtherSide) {
                            mappedBy = collectionProperty.getName();
                        }
                    }
                }
            }
        }
        return mappedBy;
    }

    private String getOneToManyMappedBy(Metadata md, Collection collection) {
        String mappedBy;
        Iterator<Selectable> joinColumnsIt = collection.getKey().getSelectables().iterator();
        Set<Selectable> joinColumns = new HashSet<>();
        while ( joinColumnsIt.hasNext() ) {
            joinColumns.add( joinColumnsIt.next() );
        }
        OneToMany oneToMany = (OneToMany) collection.getElement();
        PersistentClass pc = md.getEntityBinding(oneToMany.getReferencedEntityName());
        Iterator<Property> properties = pc.getProperties().iterator();
        //TODO we should check the table too
        boolean isOtherSide = false;
        mappedBy = "unresolved";
        while ( ! isOtherSide && properties.hasNext() ) {
            Property manyProperty = properties.next();
            Value manyValue = manyProperty.getValue();
            if (manyValue instanceof ManyToOne) {
                if ( joinColumns.size() == manyValue.getColumnSpan() ) {
                    isOtherSide = true;
                    for (Selectable selectable : manyValue.getSelectables()) {
                        if (!joinColumns.contains(selectable)) {
                            isOtherSide = false;
                            break;
                        }
                    }
                    if (isOtherSide) {
                        mappedBy = manyProperty.getName();
                    }
                }

            }
        }
        return mappedBy;
    }

    private String getOneToOneMappedBy(Metadata md, OneToOne oneToOne) {
        String mappedBy;
        Iterator<Selectable> joinSelectablesIt = oneToOne.getSelectables().iterator();
        Set<Selectable> joinSelectables = new HashSet<>();
        while ( joinSelectablesIt.hasNext() ) {
            joinSelectables.add( joinSelectablesIt.next() );
        }
        PersistentClass pc = md.getEntityBinding(oneToOne.getReferencedEntityName());
        String referencedPropertyName = oneToOne.getReferencedPropertyName();
        if ( referencedPropertyName != null )
            return referencedPropertyName;

        Iterator<Property> properties = pc.getProperties().iterator();
        //TODO we should check the table too
        boolean isOtherSide = false;
        mappedBy = "unresolved";


        while ( ! isOtherSide && properties.hasNext() ) {
            Property oneProperty = properties.next();
            Value manyValue = oneProperty.getValue();
            if ((manyValue instanceof OneToOne || manyValue instanceof ManyToOne)) {
                if ( joinSelectables.size() == manyValue.getColumnSpan() ) {
                    isOtherSide = true;
                    for (Selectable selectable : manyValue.getSelectables()) {
                        if (!joinSelectables.contains(selectable)) {
                            isOtherSide = false;
                            break;
                        }
                    }
                    if (isOtherSide) {
                        mappedBy = oneProperty.getName();
                    }
                }

            }
        }
        return mappedBy;
    }

    public boolean isSubclass() {
        return clazz.getSuperclass()!=null;
    }

    public List<Property> getPropertyClosureForFullConstructor() {
        return getPropertyClosureForFullConstructor(clazz);
    }

    protected List<Property> getPropertyClosureForFullConstructor(PersistentClass pc) {
        List<Property> l = new ArrayList<>( getPropertyClosureForSuperclassFullConstructor( pc ) );
        l.addAll(getPropertiesForFullConstructor( pc ));
        return l;
    }

    public List<Property> getPropertiesForFullConstructor() {
        return getPropertiesForFullConstructor(clazz);
    }

    protected List<Property> getPropertiesForFullConstructor(PersistentClass pc) {
        List<Property> result = new ArrayList<>();

        for ( Iterator<Property> myFields = getAllPropertiesIterator(pc); myFields.hasNext() ; ) {
            Property field = myFields.next();
            // TODO: if(!field.isGenerated() ) ) {
            if(field.equals(pc.getIdentifierProperty()) && !isAssignedIdentifier(pc, field)) {
                continue; // dont add non assigned identifiers
            }
            else if(field.equals(pc.getVersion())) {
                continue; // version prop
            }
            else if(field.isBackRef()) {
                continue;
            }
            else if(isFormula(field)) {
                continue;
            }
            else if(!isToBeGenerated(field)) {
            	continue;
            }
            else {
                result.add( field );
            }
        }

        return result;
    }

    private boolean isFormula(Property field) {
        Value value = field.getValue();
        boolean foundFormula = false;

        if(value!=null && value.getColumnSpan()>0) {
            for (Selectable element : value.getSelectables()) {
                if (!(element instanceof Formula)) {
                    return false;
                }
                else {
                    foundFormula = true;
                }
            }
        }
        else {
            return false;
        }
        return foundFormula;
    }

    public List<Property> getPropertyClosureForSuperclassFullConstructor() {
        return getPropertyClosureForSuperclassFullConstructor(clazz);
    }

    public List<Property> getPropertyClosureForSuperclassFullConstructor(PersistentClass pc) {
        List<Property> result = new ArrayList<>();
        if ( pc.getSuperclass() != null ) {
            // The correct sequence is vital here, as the subclass should be
            // able to invoke the fullconstructor based on the sequence returned
            // by this method!
            result.addAll( getPropertyClosureForSuperclassFullConstructor( pc.getSuperclass() ) );
            result.addAll( getPropertiesForFullConstructor( pc.getSuperclass() ) );
        }

        return result;
    }


    public List<Property> getPropertyClosureForMinimalConstructor() {
        return getPropertyClosureForMinimalConstructor(clazz);
    }

    protected List<Property> getPropertyClosureForMinimalConstructor(PersistentClass pc) {
        List<Property> l = new ArrayList<>( getPropertyClosureForSuperclassMinConstructor( pc ) );
        l.addAll(getPropertiesForMinimalConstructor( pc ));
        return l;
    }

    public List<Property> getPropertiesForMinimalConstructor() {
        return getPropertiesForMinimalConstructor(clazz);
    }

    protected List<Property> getPropertiesForMinimalConstructor(PersistentClass pc) {
        List<Property> result = new ArrayList<>();

        for ( Iterator<Property> myFields = getAllPropertiesIterator(pc); myFields.hasNext() ; ) {
            Property property = myFields.next();
            if(property.equals(pc.getIdentifierProperty())) {
                if(isAssignedIdentifier(pc, property)) {
                    result.add(property);
                }
                else {
                    continue;
                }
            }
            else if (property.equals(pc.getVersion())) {
                continue; // the version property should not be in the result.
            }
            else if( isRequiredInConstructor(property) ) {
                result.add(property);
            }
        }

        return result;
    }

    protected boolean isAssignedIdentifier(PersistentClass pc, Property property) {
        if(property.equals(pc.getIdentifierProperty())) {
            if(property.getValue() instanceof EnhancedValue sv) {
                return "assigned".equals(sv.getIdentifierGeneratorStrategy());
            }
            else if (property.getValue().isSimpleValue()) {
                ValueUtil v = new ValueUtil((SimpleValue)property.getValue());
                return "assigned".equals(v.getIdentifierGeneratorStrategy());
            }
        }
        return false;
    }

    public List<Property> getPropertyClosureForSuperclassMinimalConstructor() {
        return getPropertyClosureForSuperclassMinConstructor(clazz);
    }

    protected List<Property> getPropertyClosureForSuperclassMinConstructor(PersistentClass pc) {
        List<Property> result = new ArrayList<>();
        if ( pc.getSuperclass() != null ) {
            // The correct sequence is vital here, as the subclass should be
            // able to invoke the fullconstructor based on the sequence returned
            // by this method!
            result.addAll( getPropertyClosureForSuperclassMinConstructor( pc.getSuperclass() ) );
            result.addAll( getPropertiesForMinimalConstructor( pc.getSuperclass() ) );
        }

        return result;
    }

    public POJOClass getSuperClass(){
        if (!isSubclass())
            return null;
        return new EntityPOJOClass(clazz.getSuperclass(),c2j);
    }


    public String toString() {
        return "Entity: " + (clazz==null?"<none>":clazz.getEntityName());
    }

    public boolean hasVersionProperty() {
        return clazz.isVersioned() && clazz instanceof RootClass;
    }

    /*
     * @see org.hibernate.tool.hbm2x.pojo.POJOClass#getVersionProperty()
     */
    public Property getVersionProperty()
    {
        return clazz.getVersion();
    }

}
