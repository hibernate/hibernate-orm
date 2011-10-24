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

package hudson.util

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.hibernate.gradle.testing.matrix.MatrixNode
import org.hibernate.gradle.util.FileUtil

/**
 *
 * @author Strong Liu
 */
public class DBAllocationHelper {
    private static final Logger log = Logging.getLogger(DBAllocationHelper.class);

    private static final String DRIVER_PROP = "hibernate.connection.driver_class";
    private static final String URL_PROP = "hibernate.connection.url";
    private static final String USERNAME_PROP = "hibernate.connection.username";
    private static final String PASSWORD_PROP = "hibernate.connection.password";
    private static final String DB_ALLOCATION_URL_POSTFIX = "hibernate-matrix-dballocation-url-postfix";
    //DBAllocator supports the following DBs
    private static def supportedDB = ["oracle9i", "oracle10g", "oracle11gR1", "oracle11gR2",
            "oracle11gR2RAC", "oracle11gR1RAC",
            "postgresql82", "postgresql83", "postgresql84", "mysql50", "mysql51",
            "db2-91", "db2-97", "mssql2005", "mssql2008R1", "mssql2008R2", "sybase155"];
    private static final Map<MatrixNode, Map<String, String>> cache = new HashMap<MatrixNode, Map<String, String>>();

    public static Map<String, String> getProperties(MatrixNode node) {
        if ( !cache.containsKey(node) ) {
            Map<String, String> map = new HashMap<String, String>();
            cache.put(node, map);
            if ( FileUtil.isFile(node.hibernatePropertyFile) ) {
                Properties hibernateProperties = new Properties();
                hibernateProperties.load(new FileInputStream(node.hibernatePropertyFile));
                map.putAll(hibernateProperties);
            }
            if ( isDBAllocationEnabled(node.name) ) {
                log.lifecycle("using DBAllocator to get DB[${node.name}] connection info");
                try {
                    DBAllocation db = node.DBAllocation
                    db.allocate(node.name, 300);
                    Properties prop = db.properties
                    log.lifecycle("DBAllocating finished for DB[${node.name}], uuid is [${prop['uuid']}]")
                    map[DRIVER_PROP] = prop["db.jdbc_class"]
                    map[URL_PROP] = prop["db.jdbc_url"] + getURLPostfix(node.name)
                    map[USERNAME_PROP] = prop["db.username"]
                    map[PASSWORD_PROP] = prop["db.password"]
                    map["uuid"] = prop["uuid"];
                    db.clean();
                }
                catch (RuntimeException e) {
                    log.debug("DBAllocating error, ignore", e);
                }
            }

        }
        return cache.get(node);
    }

    private static String getURLPostfix(String dbName) {
        for ( String key: System.properties ) {
            if ( key.startsWith(DB_ALLOCATION_URL_POSTFIX) ) {
                String db = key.substring(DB_ALLOCATION_URL_POSTFIX.length() + 1, key.length())
                if ( db.equalsIgnoreCase(dbName) ) {
                    String postfix = System.properties[key];
                    log.debug("found URL postfix[%s] for DB[%s]", postfix, db );
                    return postfix;
                }
                else {
                    continue;
                }
            }
        }
    }
    /**
     * use -Dhibernate-matrix-dballocation=all to enable DBAllocation for all matrix node
     * or
     * add systemProp.hibernate-matrix-dballocation=all to ${user.home}/.gradle/gradle.properties
     */
    public static boolean isDBAllocationEnabled(String name) {
        if ( !supportedDB.contains(name) ) return false;
        String value = System.properties["hibernate-matrix-dballocation"]
        return value != null && (value.contains(name) || value.equals("all"));
    }
}
