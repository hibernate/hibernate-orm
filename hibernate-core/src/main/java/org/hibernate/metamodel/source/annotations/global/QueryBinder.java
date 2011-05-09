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
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;
import org.hibernate.AnnotationException;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.util.StringUtil;

/**
 * @author Hardy Ferentschik
 */
public class QueryBinder {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, QueryBinder.class.getName());

    private static boolean asBoolean( AnnotationInstance[] hints,
                                      String key,
                                      String query ) {
        String val = asString(hints, key);
        if (val == null || val.equalsIgnoreCase("false")) return false;
        if (val.equalsIgnoreCase("true")) return true;
        throw new AnnotationException("Not a boolean in hint: " + query + ":" + key);
    }

    private static CacheMode asCacheMode( AnnotationInstance[] hints,
                                          String key,
                                          String query ) {
        String val = asString(hints, key);
        if (val == null) return null;
        if (val.equalsIgnoreCase(CacheMode.GET.toString())) return CacheMode.GET;
        if (val.equalsIgnoreCase(CacheMode.IGNORE.toString())) return CacheMode.IGNORE;
        if (val.equalsIgnoreCase(CacheMode.NORMAL.toString())) return CacheMode.NORMAL;
        if (val.equalsIgnoreCase(CacheMode.PUT.toString())) return CacheMode.PUT;
        if (val.equalsIgnoreCase(CacheMode.REFRESH.toString())) return CacheMode.REFRESH;
        throw new AnnotationException("Unknown CacheMode in hint: " + query + ":" + key);
    }

    private static FlushMode asFlushMode( AnnotationInstance[] hints,
                                          String key,
                                          String query ) {
        String val = asString(hints, key);
        if (val == null) return null;
        if (val.equalsIgnoreCase(FlushMode.ALWAYS.toString())) return FlushMode.ALWAYS;
        else if (val.equalsIgnoreCase(FlushMode.AUTO.toString())) return FlushMode.AUTO;
        else if (val.equalsIgnoreCase(FlushMode.COMMIT.toString())) return FlushMode.COMMIT;
        else if (val.equalsIgnoreCase(FlushMode.NEVER.toString())) return FlushMode.MANUAL;
        else if (val.equalsIgnoreCase(FlushMode.MANUAL.toString())) return FlushMode.MANUAL;
        else throw new AnnotationException("Unknown FlushMode in hint: " + query + ":" + key);

    }

    private static Integer asInteger( AnnotationInstance[] hints,
                                      String key,
                                      String query ) {
        String val = asString(hints, key);
        if (val == null) return null;
        try {
            return Integer.decode(val);
        } catch (NumberFormatException nfe) {
            throw new AnnotationException("Not an integer in hint: " + query + ":" + key, nfe);
        }
    }

    private static String asString( AnnotationInstance[] hints,
                                    String key ) {
        for (AnnotationInstance hint : hints) {
            if (key.equals(JandexHelper.asString(hint, QueryHint.class, "name"))) return JandexHelper.asString(hint,
                                                                                                               QueryHint.class,
                                                                                                               "value");
        }
        return null;
    }

    private static Integer asTimeout( AnnotationInstance[] hints,
                                      String key,
                                      String query ) {
        Integer timeout = asInteger(hints, "javax.persistence.query.timeout", query);
        if (timeout == null) return asInteger(hints, "org.hibernate.timeout", query); // timeout is already in seconds
        return new Integer((int)Math.round(timeout.doubleValue() / 1000.0)); // convert milliseconds to seconds
    }

    public static void bind( MetadataImpl metadata,
                             Index index ) {
        for (AnnotationInstance query : index.getAnnotations(JPADotNames.NAMED_QUERY)) {
            bindNamedQuery(metadata, query, NamedQuery.class);
        }
        for (AnnotationInstance queries : index.getAnnotations(JPADotNames.NAMED_QUERIES)) {
            for (AnnotationInstance query : JandexHelper.asArray(queries, "value")) {
                bindNamedQuery(metadata, query, NamedQuery.class);
            }
        }
        for (AnnotationInstance query : index.getAnnotations(JPADotNames.NAMED_NATIVE_QUERY)) {
            bindNamedNativeQuery(metadata, query, NamedNativeQuery.class);
        }
        for (AnnotationInstance queries : index.getAnnotations(JPADotNames.NAMED_NATIVE_QUERIES)) {
            for (AnnotationInstance query : JandexHelper.asArray(queries, "value")) {
                bindNamedNativeQuery(metadata, query, NamedNativeQuery.class);
            }
        }
        for (AnnotationInstance query : index.getAnnotations(HibernateDotNames.NAMED_QUERY)) {
            bindNamedQuery(metadata, query, org.hibernate.annotations.NamedQuery.class);
        }
        for (AnnotationInstance queries : index.getAnnotations(HibernateDotNames.NAMED_QUERIES)) {
            for (AnnotationInstance query : JandexHelper.asArray(queries, "value")) {
                bindNamedQuery(metadata, query, org.hibernate.annotations.NamedQuery.class);
            }
        }
        for (AnnotationInstance query : index.getAnnotations(HibernateDotNames.NAMED_NATIVE_QUERY)) {
            bindNamedNativeQuery(metadata, query, NamedNativeQuery.class);
        }
        for (AnnotationInstance queries : index.getAnnotations(HibernateDotNames.NAMED_NATIVE_QUERIES)) {
            for (AnnotationInstance query : JandexHelper.asArray(queries, "value")) {
                bindNamedNativeQuery(metadata, query, NamedNativeQuery.class);
            }
        }
    }

    private static void bindNamedQuery( MetadataImpl metadata,
                                        AnnotationInstance annotation,
                                        Class<?> annotationClass ) {
        String name = JandexHelper.asString(annotation, annotationClass, "name");
        if (StringUtil.isEmpty(name)) throw new AnnotationException(
                                                                    "A named query must have a name when used in class or package level");
        String query = JandexHelper.asString(annotation, annotationClass, "query");
        AnnotationInstance[] hints = JandexHelper.asArray(annotation, "hints");
        String cacheRegion = asString(hints, "org.hibernate.cacheRegion");
        if (StringUtil.isEmpty(cacheRegion)) cacheRegion = null;
        Integer timeout = asTimeout(hints, "javax.persistence.query.timeout", query);
        if (timeout != null && timeout < 0) timeout = null;
        Integer fetchSize = asInteger(hints, "org.hibernate.fetchSize", name);
        if (fetchSize != null && fetchSize < 0) fetchSize = null;
        String comment = asString(hints, "org.hibernate.comment");
        if (StringUtil.isEmpty(comment)) comment = null;
        metadata.addNamedQuery(name,
                               query,
                               asBoolean(hints, "org.hibernate.cacheable", name),
                               cacheRegion,
                               timeout,
                               fetchSize,
                               asFlushMode(hints, "org.hibernate.flushMode", name),
                               asCacheMode(hints, "org.hibernate.cacheMode", name),
                               asBoolean(hints, "org.hibernate.readOnly", name),
                               comment);
        LOG.debugf("Binding named query: %s => %s", name, query);
    }

    private static void bindNamedNativeQuery( MetadataImpl metadata,
                                              AnnotationInstance annotation,
                                              Class<?> annotationClass ) {
        String name = JandexHelper.asString(annotation, annotationClass, "name");
        if (StringUtil.isEmpty(name)) throw new AnnotationException(
                                                                    "A named native query must have a name when used in class or package level");
        String query = JandexHelper.asString(annotation, annotationClass, "query");
        String resultSetMapping = JandexHelper.asString(annotation, annotationClass, "resultSetMapping");
        AnnotationInstance[] hints = JandexHelper.asArray(annotation, "hints");
        boolean cacheable = asBoolean(hints, "org.hibernate.cacheable", name);
        String cacheRegion = asString(hints, "org.hibernate.cacheRegion");
        if (StringUtil.isEmpty(cacheRegion)) cacheRegion = null;
        Integer timeout = asTimeout(hints, "javax.persistence.query.timeout", query);
        if (timeout != null && timeout < 0) timeout = null;
        Integer fetchSize = asInteger(hints, "org.hibernate.fetchSize", name);
        if (fetchSize != null && fetchSize < 0) fetchSize = null;
        FlushMode flushMode = asFlushMode(hints, "org.hibernate.flushMode", name);
        CacheMode cacheMode = asCacheMode(hints, "org.hibernate.cacheMode", name);
        boolean readOnly = asBoolean(hints, "org.hibernate.readOnly", name);
        String comment = asString(hints, "org.hibernate.comment");
        if (StringUtil.isEmpty(comment)) comment = null;
        boolean callable = asBoolean(hints, "org.hibernate.callable", name);
        NamedSQLQueryDefinition def;
        if (!StringUtil.isEmpty(resultSetMapping)) def = new NamedSQLQueryDefinition(query, resultSetMapping, null, cacheable,
                                                                                     cacheRegion, timeout, fetchSize, flushMode,
                                                                                     cacheMode, readOnly, comment, null, callable);
        else {
            String resultClass = JandexHelper.asString(annotation, NamedNativeQuery.class, "resultClass");
            if (void.class.equals(resultClass)) throw new NotYetImplementedException(
                                                                                     "Pure native scalar queries are not yet supported");
            def = new NamedSQLQueryDefinition(query, new NativeSQLQueryRootReturn[] {new NativeSQLQueryRootReturn("alias1",
                                                                                                                  resultClass,
                                                                                                                  new HashMap(),
                                                                                                                  LockMode.READ)},
                                              null, cacheable, cacheRegion, timeout, fetchSize, flushMode, cacheMode, readOnly,
                                              comment, null, callable);

        }
        metadata.addNamedNativeQuery(name, def);
        LOG.debugf("Binding named native query: %s => %s", name, query);
    }

    private QueryBinder() {
    }
}
