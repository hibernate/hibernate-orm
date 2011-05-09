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
package org.hibernate.annotations;

/**
 *
 */
public class QueryHints {

    public static final String CACHE_MODE = "org.hibernate.cacheMode";
    public static final String CACHE_REGION = "org.hibernate.cacheRegion";
    public static final String CACHEABLE = "org.hibernate.cacheable";
    public static final String CALLABLE = "org.hibernate.callable";
    public static final String COMMENT = "org.hibernate.comment";
    public static final String FETCH_SIZE = "org.hibernate.fetchSize";
    public static final String FLUSH_MODE = "org.hibernate.flushMode";
    public static final String READ_ONLY = "org.hibernate.readOnly";
    public static final String TIMEOUT_HIBERNATE = "org.hibernate.timeout";
    public static final String TIMEOUT_JPA = "javax.persistence.query.timeout";

    private QueryHints() {
    }
}
