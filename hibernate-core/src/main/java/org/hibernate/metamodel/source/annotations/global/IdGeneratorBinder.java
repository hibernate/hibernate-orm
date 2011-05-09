/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.source.annotations.global;

import java.util.Properties;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;

/**
 *
 */
public class IdGeneratorBinder {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, IdGeneratorBinder.class.getName());

    private static final boolean USE_NEW_GENERATOR_MAPPINGS = true;

    /**
     * @param metadata
     * @param index
     */
    public static void bind( MetadataImpl metadata,
                             Index index ) {
        for (AnnotationInstance generator : index.getAnnotations(JPADotNames.SEQUENCE_GENERATOR)) {
            String name = JandexHelper.asString(generator, SequenceGenerator.class, "name");
            String strategy;
            Properties prms = new Properties();
            JandexHelper.addString(generator, prms, SequenceGenerator.class, "sequenceName", SequenceStyleGenerator.SEQUENCE_PARAM);
            if (USE_NEW_GENERATOR_MAPPINGS) {
                strategy = SequenceStyleGenerator.class.getName();
                JandexHelper.addString(generator, prms, SequenceGenerator.class, "catalog", PersistentIdentifierGenerator.CATALOG);
                JandexHelper.addString(generator, prms, SequenceGenerator.class, "schema", PersistentIdentifierGenerator.SCHEMA);
                JandexHelper.addInteger(generator,
                                        prms,
                                        SequenceGenerator.class,
                                        "allocationSize",
                                        SequenceStyleGenerator.INCREMENT_PARAM);
                JandexHelper.addInteger(generator,
                                        prms,
                                        SequenceGenerator.class,
                                        "initialValue",
                                        SequenceStyleGenerator.INITIAL_PARAM);
            } else {
                strategy = "seqhilo";
                if (JandexHelper.asInteger(generator, SequenceGenerator.class, "initialValue") != 1) LOG.unsupportedInitialValue(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS);
                Integer size = JandexHelper.asInteger(generator, SequenceGenerator.class, "allocationSize");
                if (size != null) prms.put(SequenceHiLoGenerator.MAX_LO, String.valueOf(size - 1));
            }
            metadata.addIdGenerator(name, strategy, prms);
            LOG.tracef("Add sequence generator with name: %s", name);
        }
        for (AnnotationInstance generator : index.getAnnotations(JPADotNames.TABLE_GENERATOR)) {
            String name = JandexHelper.asString(generator, TableGenerator.class, "name");
            String strategy;
            Properties prms = new Properties();
            JandexHelper.addString(generator, prms, TableGenerator.class, "catalog", PersistentIdentifierGenerator.CATALOG);
            JandexHelper.addString(generator, prms, TableGenerator.class, "schema", PersistentIdentifierGenerator.SCHEMA);
            if (USE_NEW_GENERATOR_MAPPINGS) {
                strategy = org.hibernate.id.enhanced.TableGenerator.class.getName();
                prms.put(org.hibernate.id.enhanced.TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true");
                JandexHelper.addString(generator,
                                       prms,
                                       TableGenerator.class,
                                       "table",
                                       org.hibernate.id.enhanced.TableGenerator.TABLE_PARAM);
                JandexHelper.addString(generator,
                                       prms,
                                       TableGenerator.class,
                                       "pkColumnName",
                                       org.hibernate.id.enhanced.TableGenerator.SEGMENT_COLUMN_PARAM);
                JandexHelper.addString(generator,
                                       prms,
                                       TableGenerator.class,
                                       "pkColumnValue",
                                       org.hibernate.id.enhanced.TableGenerator.SEGMENT_VALUE_PARAM);
                JandexHelper.addString(generator,
                                       prms,
                                       TableGenerator.class,
                                       "valueColumnName",
                                       org.hibernate.id.enhanced.TableGenerator.VALUE_COLUMN_PARAM);
                JandexHelper.addInteger(generator,
                                        prms,
                                        TableGenerator.class,
                                        "allocationSize",
                                        org.hibernate.id.enhanced.TableGenerator.INCREMENT_PARAM);
                Integer val = JandexHelper.asInteger(generator, TableGenerator.class, "initialValue");
                if (val != null) prms.put(org.hibernate.id.enhanced.TableGenerator.INITIAL_PARAM, String.valueOf(val + 1));
            } else {
                strategy = MultipleHiLoPerTableGenerator.class.getName();
                JandexHelper.addString(generator, prms, TableGenerator.class, "table", MultipleHiLoPerTableGenerator.ID_TABLE);
                JandexHelper.addString(generator,
                                       prms,
                                       TableGenerator.class,
                                       "pkColumnName",
                                       MultipleHiLoPerTableGenerator.PK_COLUMN_NAME);
                JandexHelper.addString(generator,
                                       prms,
                                       TableGenerator.class,
                                       "pkColumnValue",
                                       MultipleHiLoPerTableGenerator.PK_VALUE_NAME);
                JandexHelper.addString(generator,
                                       prms,
                                       TableGenerator.class,
                                       "valueColumnName",
                                       MultipleHiLoPerTableGenerator.VALUE_COLUMN_NAME);
                Integer val = JandexHelper.asInteger(generator, TableGenerator.class, "allocationSize");
                if (val != null) prms.put(TableHiLoGenerator.MAX_LO, String.valueOf(val - 1));
            }
            if (JandexHelper.asArray(generator, "uniqueConstraints").length > 0) LOG.ignoringTableGeneratorConstraints(name);
            metadata.addIdGenerator(name, strategy, prms);
            LOG.tracef("Add table generator with name: %s", name);
        }
        for (AnnotationInstance generator : index.getAnnotations(HibernateDotNames.GENERIC_GENERATOR)) {
            bindGenericGenerator(metadata, generator);
        }
        for (AnnotationInstance generators : index.getAnnotations(HibernateDotNames.GENERIC_GENERATORS)) {
            for (AnnotationInstance generator : JandexHelper.asArray(generators, "value")) {
                bindGenericGenerator(metadata, generator);
            }
        }
    }

    private static void bindGenericGenerator( MetadataImpl metadata,
                                              AnnotationInstance generator ) {
        String name = JandexHelper.asString(generator, GenericGenerator.class, "name");
        Properties prms = new Properties();
        for (AnnotationInstance prmAnnotation : JandexHelper.asArray(generator, "parameters")) {
            prms.put(JandexHelper.asString(prmAnnotation, Parameter.class, "name"),
                     JandexHelper.asString(prmAnnotation, Parameter.class, "value"));
        }
        metadata.addIdGenerator(name, JandexHelper.asString(generator, GenericGenerator.class, "strategy"), prms);
        LOG.tracef("Add generic generator with name: %s", name);
    }

    private IdGeneratorBinder() {
    }
}
