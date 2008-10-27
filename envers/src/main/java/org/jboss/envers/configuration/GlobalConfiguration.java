/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.  
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.configuration;

import java.util.Properties;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Nicolas Doroskevich
 */
public class GlobalConfiguration {
    // Should a revision be generated when a not-owned relation field changes
    private final boolean generateRevisionsForCollections;

    // Should a warning, instead of an error and an exception, be logged, when an unsupported type is versioned
    private final boolean warnOnUnsupportedTypes;

    // Should the optimistic locking property of an entity be considered unversioned
    private final boolean unversionedOptimisticLockingField;

    /*
     Which operator to use in correlated subqueries (when we want a property to be equal to the result of
     a correlated subquery, for example: e.p <operator> (select max(e2.p) where e2.p2 = e.p2 ...).
     Normally, this should be "=". However, HSQLDB has an issue related to that, so as a workaround,
     "in" is used. See {@link org.jboss.envers.test.various.HsqlTest}.
     */
    private final String correlatedSubqueryOperator;

    public GlobalConfiguration(Properties properties) {
        String generateRevisionsForCollectionsStr = properties.getProperty("org.jboss.envers.revisionOnCollectionChange",
                "true");
        generateRevisionsForCollections = Boolean.parseBoolean(generateRevisionsForCollectionsStr);

        String warnOnUnsupportedTypesStr = properties.getProperty("org.jboss.envers.warnOnUnsupportedTypes",
                "false");
        warnOnUnsupportedTypes = Boolean.parseBoolean(warnOnUnsupportedTypesStr);

        String ignoreOptimisticLockingPropertyStr = properties.getProperty("org.jboss.envers.unversionedOptimisticLockingField",
                "false");
        unversionedOptimisticLockingField = Boolean.parseBoolean(ignoreOptimisticLockingPropertyStr);

        correlatedSubqueryOperator = "org.hibernate.dialect.HSQLDialect".equals(
                properties.getProperty("hibernate.dialect")) ? "in" : "=";
    }

    public boolean isGenerateRevisionsForCollections() {
        return generateRevisionsForCollections;
    }

    public boolean isWarnOnUnsupportedTypes() {
        return warnOnUnsupportedTypes;
    }

    public boolean isUnversionedOptimisticLockingField() {
        return unversionedOptimisticLockingField;
    }

    public String getCorrelatedSubqueryOperator() {
        return correlatedSubqueryOperator;
    }
}
