/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2015-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.reveng.binder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.FetchMode;
import org.hibernate.internal.util.collections.JoinedList;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Fetchable;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.RevengStrategy;

public class BinderUtils {

    public static Logger LOGGER = Logger.getLogger(BinderUtils.class.getName());

    public static String makeUnique(
            Iterator<Property> props,
            String originalPropertyName) {
        int cnt = 0;
        String propertyName = originalPropertyName;
        Set<String> uniqueNames = new HashSet<>();
        while ( props.hasNext() ) {
            Property element = props.next();
            uniqueNames.add( element.getName() );
        }
        while( uniqueNames.contains(propertyName) ) {
            cnt++;
            propertyName = originalPropertyName + "_" + cnt;
        }
        return propertyName;
    }

    public static String makeUnique(PersistentClass clazz, String propertyName) {
        List<Property> list = new ArrayList<>();
        if( clazz.hasIdentifierProperty() ) {
            list.add( clazz.getIdentifierProperty() );
        }
        if( clazz.isVersioned() ) {
            list.add( clazz.getVersion() );
        }
        JoinedList<Property> joinedList =
                new JoinedList<>(
                        list,
                        clazz.getProperties() );
        return BinderUtils.makeUnique(joinedList.iterator(), propertyName);
    }

    public static String makeUnique(Component clazz, String propertyName) {
        return BinderUtils.makeUnique(clazz.getProperties().iterator(), propertyName);
    }

    public static void checkColumnForMultipleBinding(Column column) {
        if(column.getValue()!=null) {
            LOGGER.log(Level.WARNING, "Binding column twice should not happen. " + column);
// TODO enable this next line and investigate why the tests fail
//			throw new RuntimeException("Binding column twice should not happen. " + column);
        }
    }

    static void updateFetchMode(Fetchable value, String fetchMode) {
        if(FetchMode.JOIN.toString().equalsIgnoreCase(fetchMode)) {
            value.setFetchMode(FetchMode.JOIN);
        }
        else {
            value.setFetchMode(FetchMode.SELECT);
        }
    }


    static AssociationInfo getAssociationInfo(
            RevengStrategy revengStrategy,
            ForeignKey foreignKey,
            boolean inverseProperty) {
        if (inverseProperty) {
            return revengStrategy.foreignKeyToInverseAssociationInfo(foreignKey);
        }
        else {
            return revengStrategy.foreignKeyToAssociationInfo(foreignKey);
        }
    }

}
