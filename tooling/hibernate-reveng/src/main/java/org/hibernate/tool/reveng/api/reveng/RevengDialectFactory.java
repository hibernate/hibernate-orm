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
package org.hibernate.tool.reveng.api.reveng;

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.tool.reveng.internal.dialect.H2RevengDialect;
import org.hibernate.tool.reveng.internal.dialect.HSQLRevengDialect;
import org.hibernate.tool.reveng.internal.dialect.JDBCRevengDialect;
import org.hibernate.tool.reveng.internal.dialect.MySQLRevengDialect;
import org.hibernate.tool.reveng.internal.dialect.OracleRevengDialect;
import org.hibernate.tool.reveng.internal.dialect.SQLServerRevengDialect;

public class RevengDialectFactory {

    private RevengDialectFactory() {}

    public static RevengDialect createMetaDataDialect(Dialect dialect, Properties cfg) {
        String property = cfg.getProperty( "hibernatetool.metadatadialect" );
        RevengDialect mdd = fromClassName(property);
        if(mdd==null) {
            mdd = fromDialect(dialect);
        }
        if(mdd==null) {
            mdd = fromDialectName(dialect.getClass().getName());
        }
        if(mdd==null) {
            mdd = new JDBCRevengDialect();
        }
        return mdd;
    }

    public static RevengDialect fromClassName(String property) {
        if ( property != null ) {
            try {
                Class<?> revengDialectClass = ReflectHelper.classForName(
                        property,
                        RevengDialectFactory.class );
                Constructor<?> revengDialectConstructor = revengDialectClass.getConstructor();
                return (RevengDialect)revengDialectConstructor.newInstance();
            }
            catch (Throwable e) {
                throw new RuntimeException(
                        "Could not load RevengDialect: " + property, e );
            }
        }
        else {
            return null;
        }
    }

    public static RevengDialect fromDialect(Dialect dialect) {
        if(dialect!=null) {
            if(dialect instanceof OracleDialect) {
                return new OracleRevengDialect();
            }
            else if (dialect instanceof H2Dialect) {
                return new H2RevengDialect();
            }
            else if (dialect instanceof MySQLDialect) {
                return new MySQLRevengDialect();
            }
            else if (dialect instanceof HSQLDialect) {
                return new HSQLRevengDialect();
            }
            else if (dialect instanceof SQLServerDialect) {
                return new SQLServerRevengDialect();
            }
        }
        return null;
    }

    public static RevengDialect fromDialectName(String dialect) {
        if (dialect.toLowerCase().contains("oracle")) {
            return new OracleRevengDialect();
        }
        if (dialect.toLowerCase().contains("mysql")) {
            return new MySQLRevengDialect();
        }
        if (dialect.toLowerCase().contains("h2")) {
            return new H2RevengDialect();
        }
        if (dialect.toLowerCase().contains("hsql")) {
            return new HSQLRevengDialect();
        }
        if (dialect.toLowerCase().contains("sqlserver")) {
            return new SQLServerRevengDialect();
        }
        return null;
    }


}
