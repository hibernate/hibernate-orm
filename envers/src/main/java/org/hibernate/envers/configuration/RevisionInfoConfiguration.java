/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.configuration;

import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.envers.Audited;
import org.hibernate.envers.*;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.configuration.metadata.MetadataTools;
import org.hibernate.envers.revisioninfo.DefaultRevisionInfoGenerator;
import org.hibernate.envers.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.tools.MutableBoolean;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionInfoConfiguration {
    private String revisionInfoEntityName;
    private PropertyData revisionInfoIdData;
    private PropertyData revisionInfoTimestampData;
    private String revisionInfoTimestampType;

    private String revisionPropType;

    public RevisionInfoConfiguration() {
        revisionInfoEntityName = "org.hibernate.envers.DefaultRevisionEntity";
        revisionInfoIdData = new PropertyData("id", "field", null);
        revisionInfoTimestampData = new PropertyData("timestamp", "field", null);
        revisionInfoTimestampType = "long";

        revisionPropType = "integer";
    }

    private Document generateDefaultRevisionInfoXmlMapping() {
        Document document = DocumentHelper.createDocument();

        Element class_mapping = MetadataTools.createEntity(document, null, null, null, null, null);

        class_mapping.addAttribute("name", revisionInfoEntityName);
        class_mapping.addAttribute("table", "REVINFO");

        Element idProperty = MetadataTools.addNativelyGeneratedId(class_mapping, revisionInfoIdData.getName(),
                revisionPropType);
        MetadataTools.addColumn(idProperty, "REV", null);

        Element timestampProperty = MetadataTools.addProperty(class_mapping, revisionInfoTimestampData.getName(),
                revisionInfoTimestampType, true, false);
        MetadataTools.addColumn(timestampProperty, "REVTSTMP", null);

        return document;
    }

    private Element generateRevisionInfoRelationMapping() {
        Document document = DocumentHelper.createDocument();
        Element rev_rel_mapping =document.addElement("key-many-to-one");
        rev_rel_mapping.addAttribute("type", revisionPropType);
        rev_rel_mapping.addAttribute("class", revisionInfoEntityName);

        return rev_rel_mapping;
    }

    private void searchForRevisionInfoCfgInProperties(XClass clazz, ReflectionManager reflectionManager,
                                    MutableBoolean revisionNumberFound, MutableBoolean revisionTimestampFound,
                                    String accessType) {
        for (XProperty property : clazz.getDeclaredProperties(accessType)) {
            RevisionNumber revisionNumber = property.getAnnotation(RevisionNumber.class);
            RevisionTimestamp revisionTimestamp = property.getAnnotation(RevisionTimestamp.class);

            if (revisionNumber != null) {
                if (revisionNumberFound.isSet()) {
                    throw new MappingException("Only one property may be annotated with @RevisionNumber!");
                }

                XClass revisionNumberClass = property.getType();
                if (reflectionManager.equals(revisionNumberClass, Integer.class) ||
                        reflectionManager.equals(revisionNumberClass, Integer.TYPE)) {
                    revisionInfoIdData = new PropertyData(property.getName(), accessType, null);
                    revisionNumberFound.set();
                } else if (reflectionManager.equals(revisionNumberClass, Long.class) ||
                        reflectionManager.equals(revisionNumberClass, Long.TYPE)) {
                    revisionInfoIdData = new PropertyData(property.getName(), accessType, null);
                    revisionNumberFound.set();

                    // The default is integer
                    revisionPropType = "long";
                } else {
                    throw new MappingException("The field annotated with @RevisionNumber must be of type " +
                            "int, Integer, long or Long");
                }
            }

            if (revisionTimestamp != null) {
                if (revisionTimestampFound.isSet()) {
                    throw new MappingException("Only one property may be annotated with @RevisionTimestamp!");
                }

                XClass revisionTimestampClass = property.getType();
                if (reflectionManager.equals(revisionTimestampClass, Long.class) ||
                        reflectionManager.equals(revisionTimestampClass, Long.TYPE)) {
                    revisionInfoTimestampData = new PropertyData(property.getName(), accessType, null);
                    revisionTimestampFound.set();
                } else {
                    throw new MappingException("The field annotated with @RevisionTimestamp must be of type " +
                            "long or Long");
                }
            }
        }
    }

    private void searchForRevisionInfoCfg(XClass clazz, ReflectionManager reflectionManager,
                                          MutableBoolean revisionNumberFound, MutableBoolean revisionTimestampFound) {
        XClass superclazz = clazz.getSuperclass();
        if (!"java.lang.Object".equals(superclazz.getName())) {
            searchForRevisionInfoCfg(superclazz, reflectionManager, revisionNumberFound, revisionTimestampFound);
        }

        searchForRevisionInfoCfgInProperties(clazz, reflectionManager, revisionNumberFound, revisionTimestampFound,
                "field");
        searchForRevisionInfoCfgInProperties(clazz, reflectionManager, revisionNumberFound, revisionTimestampFound,
                "property");
    }

    @SuppressWarnings({"unchecked"})
    public RevisionInfoConfigurationResult configure(Configuration cfg, ReflectionManager reflectionManager) {
        Iterator<PersistentClass> classes = (Iterator<PersistentClass>) cfg.getClassMappings();
        boolean revisionEntityFound = false;
        RevisionInfoGenerator revisionInfoGenerator = null;

        Class<?> revisionInfoClass = null;

        while (classes.hasNext()) {
            PersistentClass pc = classes.next();
            XClass clazz;
            try {
                clazz = reflectionManager.classForName(pc.getClassName(), this.getClass());
            } catch (ClassNotFoundException e) {
                throw new MappingException(e);
            }

            RevisionEntity revisionEntity = clazz.getAnnotation(RevisionEntity.class);
            if (revisionEntity != null) {
                if (revisionEntityFound) {
                    throw new MappingException("Only one entity may be annotated with @RevisionEntity!");
                }

                // Checking if custom revision entity isn't audited
                if (clazz.getAnnotation(Audited.class) != null) {
                    throw new MappingException("An entity annotated with @RevisionEntity cannot be audited!");
                }

                revisionEntityFound = true;

                MutableBoolean revisionNumberFound = new MutableBoolean();
                MutableBoolean revisionTimestampFound = new MutableBoolean();

                searchForRevisionInfoCfg(clazz, reflectionManager, revisionNumberFound, revisionTimestampFound);

                if (!revisionNumberFound.isSet()) {
                    throw new MappingException("An entity annotated with @RevisionEntity must have a field annotated " +
                            "with @RevisionNumber!");
                }

                if (!revisionTimestampFound.isSet()) {
                    throw new MappingException("An entity annotated with @RevisionEntity must have a field annotated " +
                            "with @RevisionTimestamp!");
                }

                revisionInfoEntityName = pc.getEntityName();

                revisionInfoClass = pc.getMappedClass();
                revisionInfoGenerator = new DefaultRevisionInfoGenerator(revisionInfoEntityName, revisionInfoClass,
                        revisionEntity.value(), revisionInfoTimestampData);
            }
        }

        // In case of a custom revision info generator, the mapping will be null.
        Document revisionInfoXmlMapping = null;

        if (revisionInfoGenerator == null) {
            revisionInfoClass = DefaultRevisionEntity.class;
            revisionInfoGenerator = new DefaultRevisionInfoGenerator(revisionInfoEntityName, revisionInfoClass,
                    RevisionListener.class, revisionInfoTimestampData);
            revisionInfoXmlMapping = generateDefaultRevisionInfoXmlMapping();
        }

        return new RevisionInfoConfigurationResult(
                revisionInfoGenerator, revisionInfoXmlMapping,
                new RevisionInfoQueryCreator(revisionInfoEntityName, revisionInfoIdData.getName(),
                        revisionInfoTimestampData.getName()),
                generateRevisionInfoRelationMapping(),
                new RevisionInfoNumberReader(revisionInfoClass, revisionInfoIdData), revisionInfoEntityName);
    }
}

class RevisionInfoConfigurationResult {
    private final RevisionInfoGenerator revisionInfoGenerator;
    private final Document revisionInfoXmlMapping;
    private final RevisionInfoQueryCreator revisionInfoQueryCreator;
    private final Element revisionInfoRelationMapping;
    private final RevisionInfoNumberReader revisionInfoNumberReader;
    private final String revisionInfoEntityName;

    RevisionInfoConfigurationResult(RevisionInfoGenerator revisionInfoGenerator,
                                    Document revisionInfoXmlMapping, RevisionInfoQueryCreator revisionInfoQueryCreator,
                                    Element revisionInfoRelationMapping,
                                    RevisionInfoNumberReader revisionInfoNumberReader, String revisionInfoEntityName) {
        this.revisionInfoGenerator = revisionInfoGenerator;
        this.revisionInfoXmlMapping = revisionInfoXmlMapping;
        this.revisionInfoQueryCreator = revisionInfoQueryCreator;
        this.revisionInfoRelationMapping = revisionInfoRelationMapping;
        this.revisionInfoNumberReader = revisionInfoNumberReader;
        this.revisionInfoEntityName = revisionInfoEntityName;
    }

    public RevisionInfoGenerator getRevisionInfoGenerator() {
        return revisionInfoGenerator;
    }

    public Document getRevisionInfoXmlMapping() {
        return revisionInfoXmlMapping;
    }

    public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
        return revisionInfoQueryCreator;
    }

    public Element getRevisionInfoRelationMapping() {
        return revisionInfoRelationMapping;
    }

    public RevisionInfoNumberReader getRevisionInfoNumberReader() {
        return revisionInfoNumberReader;
    }

    public String getRevisionInfoEntityName() {
        return revisionInfoEntityName;
    }
}