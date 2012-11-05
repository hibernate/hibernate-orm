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
import org.hibernate.engine.SessionImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Map;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ClassMetadataDefaultMock implements ClassMetadata {

  public String getEntityName() {
    throw new UnsupportedOperationException();
  }

  public String getIdentifierPropertyName() {
    throw new UnsupportedOperationException();
  }

  public String[] getPropertyNames() {
    throw new UnsupportedOperationException();
  }

  public Type getIdentifierType() {
    throw new UnsupportedOperationException();
  }

  public Type[] getPropertyTypes() {
    throw new UnsupportedOperationException();
  }

  public Type getPropertyType(String propertyName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean hasProxy() {
    throw new UnsupportedOperationException();
  }

  public boolean isMutable() {
    throw new UnsupportedOperationException();
  }

  public boolean isVersioned() {
    throw new UnsupportedOperationException();
  }

  public int getVersionProperty() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyNullability() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyLaziness() {
    throw new UnsupportedOperationException();
  }

  public boolean hasIdentifierProperty() {
    throw new UnsupportedOperationException();
  }

  public boolean hasNaturalIdentifier() {
    throw new UnsupportedOperationException();
  }

  public int[] getNaturalIdentifierProperties() {
    throw new UnsupportedOperationException();
  }

  public boolean hasSubclasses() {
    throw new UnsupportedOperationException();
  }

  public boolean isInherited() {
    throw new UnsupportedOperationException();
  }

  public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Class getMappedClass(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public Object instantiate(Serializable id, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object getPropertyValue(Object object, String propertyName,
      EntityMode entityMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object[] getPropertyValues(Object entity, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void setPropertyValue(Object object, String propertyName, Object value,
      EntityMode entityMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void setPropertyValues(Object object, Object[] values,
      EntityMode entityMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Serializable getIdentifier(Object entity, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void setIdentifier(Object object, Serializable id,
      EntityMode entityMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean implementsLifecycle(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public boolean implementsValidatable(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public Object getVersion(Object object, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }
}
