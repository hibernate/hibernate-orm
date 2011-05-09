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
import org.hibernate.AnnotationException;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.util.StringUtil;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;

/**
 *
 */
public class TypeDefBinder {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, TypeDefBinder.class.getName());

    private static void bind( String name,
                              String typeClass,
                              Properties prms,
                              MetadataImpl metadata ) {
        LOG.debugf("Binding type definition: %s", name);
        metadata.addTypeDef(name, typeClass, prms);
    }

    private static void bind( MetadataImpl metadata,
                              AnnotationInstance typeDef ) {
        String name = JandexHelper.asString(typeDef, TypeDef.class, "name");
        String defaultForType = JandexHelper.asString(typeDef, TypeDef.class, "defaultForType");
        String typeClass = JandexHelper.asString(typeDef, TypeDef.class, "typeClass");
        boolean noName = StringUtil.isEmpty(name);
        boolean noDefaultForType = defaultForType == null || defaultForType.equals(void.class.getName());
        if (noName && noDefaultForType) throw new AnnotationException(
                                                                      "Either name or defaultForType (or both) attribute should be set in TypeDef having typeClass "
                                                                      + typeClass);
        Properties prms = new Properties();
        for (AnnotationInstance prm : JandexHelper.asArray(typeDef, "parameters")) {
            prms.put(JandexHelper.asString(prm, Parameter.class, "name"), JandexHelper.asString(prm, Parameter.class, "value"));
        }
        if (!noName) bind(name, typeClass, prms, metadata);
        if (!noDefaultForType) bind(defaultForType, typeClass, prms, metadata);
    }

    /**
     * @param metadata
     * @param index
     */
    public static void bind( MetadataImpl metadata,
                             Index index ) {
        for (AnnotationInstance typeDef : index.getAnnotations(HibernateDotNames.TYPE_DEF)) {
            bind(metadata, typeDef);
        }
        for (AnnotationInstance typeDefs : index.getAnnotations(HibernateDotNames.TYPE_DEFS)) {
            for (AnnotationInstance typeDef : JandexHelper.asArray(typeDefs, "value")) {
                bind(metadata, typeDef);
            }
        }
    }

    private TypeDefBinder() {
    }
}
