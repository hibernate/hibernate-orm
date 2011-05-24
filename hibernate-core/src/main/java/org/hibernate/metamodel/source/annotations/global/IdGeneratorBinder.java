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

import java.util.HashMap;
import java.util.Map;
import javax.persistence.SequenceGenerator;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.IdGenerator;
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

    private static void addStringParameter( AnnotationInstance annotation,
                                            String element,
                                            Map<String, String> parameters,
                                            String parameter ) {
        String string = JandexHelper.getValueAsString(annotation, element);
        if (StringHelper.isNotEmpty(string)) parameters.put(parameter, string);
    }

    /**
     * Binds all {@link SequenceGenerator}, {@link javax.persistence.TableGenerator}, {@link GenericGenerator}, and {
     * {@link GenericGenerators} annotations to the supplied metadata.
     *
     * @param metadata the global metadata
     * @param jandex the jandex index
     */
    public static void bind( MetadataImpl metadata,
                             Index jandex ) {
        for (AnnotationInstance generator : jandex.getAnnotations(JPADotNames.SEQUENCE_GENERATOR)) {
            bindSequenceGenerator(metadata, generator);
        }
        for (AnnotationInstance generator : jandex.getAnnotations(JPADotNames.TABLE_GENERATOR)) {
            bindTableGenerator(metadata, generator);
        }
        for (AnnotationInstance generator : jandex.getAnnotations(HibernateDotNames.GENERIC_GENERATOR)) {
            bindGenericGenerator(metadata, generator);
        }
        for (AnnotationInstance generators : jandex.getAnnotations(HibernateDotNames.GENERIC_GENERATORS)) {
            for (AnnotationInstance generator : JandexHelper.getValueAsArray(generators, "value")) {
                bindGenericGenerator(metadata, generator);
            }
        }
    }

    private static void bindGenericGenerator( MetadataImpl metadata,
                                              AnnotationInstance generator ) {
        String name = JandexHelper.getValueAsString(generator, "name");
        Map<String, String> prms = new HashMap<String, String>();
        for (AnnotationInstance prm : JandexHelper.getValueAsArray(generator, "parameters")) {
            prms.put(JandexHelper.getValueAsString(prm, "name"), JandexHelper.getValueAsString(prm, "value"));
        }
        metadata.addIdGenerator(new IdGenerator(name, JandexHelper.getValueAsString(generator, "strategy"), prms));
        LOG.tracef("Add generic generator with name: %s", name);
    }

    private static void bindSequenceGenerator( MetadataImpl metadata,
                                               AnnotationInstance generator ) {
        String name = JandexHelper.getValueAsString(generator, "name");
        String strategy;
        Map<String, String> prms = new HashMap<String, String>();
        addStringParameter(generator, "sequenceName", prms, SequenceStyleGenerator.SEQUENCE_PARAM);
        if (metadata.getOptions().useNewIdentifierGenerators()) {
            strategy = SequenceStyleGenerator.class.getName();
            addStringParameter(generator, "catalog", prms, PersistentIdentifierGenerator.CATALOG);
            addStringParameter(generator, "schema", prms, PersistentIdentifierGenerator.SCHEMA);
            prms.put(SequenceStyleGenerator.INCREMENT_PARAM,
                     String.valueOf(JandexHelper.getValueAsInt(generator, "allocationSize")));
            prms.put(SequenceStyleGenerator.INITIAL_PARAM,
                     String.valueOf(JandexHelper.getValueAsInt(generator, "initialValue")));
        } else {
            strategy = "seqhilo";
            if (JandexHelper.getValueAsInt(generator, "initialValue") != 1) LOG.unsupportedInitialValue(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS);
            prms.put(SequenceHiLoGenerator.MAX_LO,
                     String.valueOf(JandexHelper.getValueAsInt(generator, "allocationSize") - 1));
        }
        metadata.addIdGenerator(new IdGenerator(name, strategy, prms));
        LOG.tracef("Add sequence generator with name: %s", name);
    }

    private static void bindTableGenerator( MetadataImpl metadata,
                                            AnnotationInstance generator ) {
        String name = JandexHelper.getValueAsString(generator, "name");
        String strategy;
        Map<String, String> prms = new HashMap<String, String>();
        addStringParameter(generator, "catalog", prms, PersistentIdentifierGenerator.CATALOG);
        addStringParameter(generator, "schema", prms, PersistentIdentifierGenerator.SCHEMA);
        if (metadata.getOptions().useNewIdentifierGenerators()) {
            strategy = TableGenerator.class.getName();
            prms.put(TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true");
            addStringParameter(generator, "table", prms, TableGenerator.TABLE_PARAM);
            addStringParameter(generator, "pkColumnName", prms, TableGenerator.SEGMENT_COLUMN_PARAM);
            addStringParameter(generator, "pkColumnValue", prms, TableGenerator.SEGMENT_VALUE_PARAM);
            addStringParameter(generator, "valueColumnName", prms, TableGenerator.VALUE_COLUMN_PARAM);
            prms.put(TableGenerator.INCREMENT_PARAM,
                     String.valueOf(JandexHelper.getValueAsInt(generator, "allocationSize")));
            prms.put(TableGenerator.INITIAL_PARAM,
                     String.valueOf(JandexHelper.getValueAsInt(generator, "initialValue") + 1));
        } else {
            strategy = MultipleHiLoPerTableGenerator.class.getName();
            addStringParameter(generator, "table", prms, MultipleHiLoPerTableGenerator.ID_TABLE);
            addStringParameter(generator, "pkColumnName", prms, MultipleHiLoPerTableGenerator.PK_COLUMN_NAME);
            addStringParameter(generator, "pkColumnValue", prms, MultipleHiLoPerTableGenerator.PK_VALUE_NAME);
            addStringParameter(generator, "valueColumnName", prms, MultipleHiLoPerTableGenerator.VALUE_COLUMN_NAME);
            prms.put(TableHiLoGenerator.MAX_LO, String.valueOf(JandexHelper.getValueAsInt(generator, "allocationSize") - 1));
        }
        if (JandexHelper.getValueAsArray(generator, "uniqueConstraints").length > 0) LOG.ignoringTableGeneratorConstraints(name);
        metadata.addIdGenerator(new IdGenerator(name, strategy, prms));
        LOG.tracef("Add table generator with name: %s", name);
    }

    private IdGeneratorBinder() {
    }
}
