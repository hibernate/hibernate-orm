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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.hibernate.envers.configuration.metadata.AnnotationsMetadataReader;
import org.hibernate.envers.configuration.metadata.EntityXmlMappingData;
import org.hibernate.envers.configuration.metadata.PersistentClassVersioningData;
import org.hibernate.envers.configuration.metadata.AuditMetadataGenerator;
import org.hibernate.envers.entities.EntitiesConfigurations;
import org.hibernate.envers.tools.StringTools;
import org.hibernate.envers.tools.graph.GraphTopologicalSort;
import org.hibernate.envers.tools.reflection.YReflectionManager;

import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntitiesConfigurator {
    public EntitiesConfigurations configure(Configuration cfg, YReflectionManager reflectionManager,
                                            GlobalConfiguration globalCfg, AuditEntitiesConfiguration verEntCfg,
                                            Document revisionInfoXmlMapping, Element revisionInfoRelationMapping) {
        AuditMetadataGenerator versionsMetaGen = new AuditMetadataGenerator(cfg, globalCfg, verEntCfg,
                revisionInfoRelationMapping);
        DOMWriter writer = new DOMWriter();

        // Sorting the persistent class topologically - superclass always before subclass
        Iterator<PersistentClass> classes = GraphTopologicalSort.sort(new PersistentClassGraphDefiner(cfg)).iterator();

        Map<PersistentClass, PersistentClassVersioningData> pcDatas =
                new HashMap<PersistentClass, PersistentClassVersioningData>();
        Map<PersistentClass, EntityXmlMappingData> xmlMappings = new HashMap<PersistentClass, EntityXmlMappingData>();

        // First pass
        while (classes.hasNext()) {
            PersistentClass pc = classes.next();
            // Collecting information from annotations on the persistent class pc
            AnnotationsMetadataReader annotationsMetadataReader =
                    new AnnotationsMetadataReader(globalCfg, reflectionManager, pc);
            PersistentClassVersioningData versioningData = annotationsMetadataReader.getVersioningData();

            if (versioningData.isVersioned()) {
                pcDatas.put(pc, versioningData);

                if (!StringTools.isEmpty(versioningData.versionsTable.value())) {
                    verEntCfg.addCustomVersionsTableName(pc.getEntityName(), versioningData.versionsTable.value());
                }

                EntityXmlMappingData xmlMappingData = new EntityXmlMappingData();
                versionsMetaGen.generateFirstPass(pc, versioningData, xmlMappingData);
                xmlMappings.put(pc, xmlMappingData);
            }
        }

        // Second pass
        for (Map.Entry<PersistentClass, PersistentClassVersioningData> pcDatasEntry : pcDatas.entrySet()) {
            EntityXmlMappingData xmlMappingData = xmlMappings.get(pcDatasEntry.getKey());

            versionsMetaGen.generateSecondPass(pcDatasEntry.getKey(), pcDatasEntry.getValue(), xmlMappingData);

            try {
                cfg.addDocument(writer.write(xmlMappingData.getMainXmlMapping()));
                // TODO
                //writeDocument(xmlMappingData.getMainXmlMapping());

                for (Document additionalMapping : xmlMappingData.getAdditionalXmlMappings()) {
                    cfg.addDocument(writer.write(additionalMapping));
                    // TODO
                    //writeDocument(additionalMapping);
                }
            } catch (DocumentException e) {
                throw new MappingException(e);
            }
        }

        // Only if there are any versioned classes
        if (pcDatas.size() > 0) {
            try {
                if (revisionInfoXmlMapping !=  null) {
                    // TODO
                    //writeDocument(revisionInfoXmlMapping);
                    cfg.addDocument(writer.write(revisionInfoXmlMapping));
                }
            } catch (DocumentException e) {
                throw new MappingException(e);
            }
        }

        return new EntitiesConfigurations(versionsMetaGen.getEntitiesConfigurations());
    }

    // todo
    private void writeDocument(Document e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer w = new PrintWriter(baos);

        try {
            XMLWriter xw = new XMLWriter(w, new OutputFormat(" ", true));
            xw.write(e);
            w.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        System.out.println("-----------");
        System.out.println(baos.toString());
        System.out.println("-----------");
    }
}
