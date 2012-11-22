/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.defaultmock;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Map;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ClassMetadataDefaultMock implements ClassMetadata {

    @Override
    public String getEntityName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getIdentifierPropertyName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getPropertyNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getIdentifierType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type[] getPropertyTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getPropertyType(String propertyName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasProxy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVersioned() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getVersionProperty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getPropertyNullability() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getPropertyLaziness() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasIdentifierProperty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNaturalIdentifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] getNaturalIdentifierProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasSubclasses() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInherited() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SessionImplementor session) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class getMappedClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object instantiate(Serializable id, SessionImplementor session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getPropertyValue(Object object, String propertyName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getPropertyValues(Object entity) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPropertyValue(Object object, String propertyName, Object value) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPropertyValues(Object object, Object[] values) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Serializable getIdentifier(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getIdentifier(Object entity, SessionImplementor session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIdentifier(Object entity, Serializable id, SessionImplementor session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean implementsLifecycle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getVersion(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }
}
