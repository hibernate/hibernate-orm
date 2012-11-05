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

import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author maxr@google.com (Max Ross)
 */
public class TypeDefaultMock implements Type {

  public boolean isAssociationType() {
    throw new UnsupportedOperationException();
  }

  public boolean isCollectionType() {
    throw new UnsupportedOperationException();
  }

  public boolean isComponentType() {
    throw new UnsupportedOperationException();
  }

  public boolean isEntityType() {
    throw new UnsupportedOperationException();
  }

  public boolean isAnyType() {
    throw new UnsupportedOperationException();
  }

  public boolean isXMLElement() {
    throw new UnsupportedOperationException();
  }

  public int[] sqlTypes(Mapping mapping) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public int getColumnSpan(Mapping mapping) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public Class getReturnedClass() {
    throw new UnsupportedOperationException();
  }

  public boolean isSame(Object x, Object y, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean isEqual(Object x, Object y, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean isEqual(Object x, Object y, EntityMode entityMode,
      SessionFactoryImplementor factory) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public int getHashCode(Object x, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public int getHashCode(Object x, EntityMode entityMode,
      SessionFactoryImplementor factory) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public int compare(Object x, Object y, EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public boolean isDirty(Object old, Object current, SessionImplementor session)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean isDirty(Object old, Object current, boolean[] checkable,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean isModified(Object oldHydratedState, Object currentState,
      boolean[] checkable, SessionImplementor session)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object nullSafeGet(ResultSet rs, String[] names,
      SessionImplementor session, Object owner)
      throws HibernateException, SQLException {
    throw new UnsupportedOperationException();
  }

  public Object nullSafeGet(ResultSet rs, String name,
      SessionImplementor session, Object owner)
      throws HibernateException, SQLException {
    throw new UnsupportedOperationException();
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index,
      boolean[] settable, SessionImplementor session)
      throws HibernateException, SQLException {
    throw new UnsupportedOperationException();
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index,
      SessionImplementor session) throws HibernateException, SQLException {
    throw new UnsupportedOperationException();
  }

  public void setToXMLNode(Node node, Object value,
      SessionFactoryImplementor factory) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public String toLoggableString(Object value,
      SessionFactoryImplementor factory) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object fromXMLNode(Node xml, Mapping factory)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public String getName() {
    throw new UnsupportedOperationException();
  }

  public Object deepCopy(Object value, EntityMode entityMode,
      SessionFactoryImplementor factory) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean isMutable() {
    throw new UnsupportedOperationException();
  }

  public Serializable disassemble(Object value, SessionImplementor session,
      Object owner) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object assemble(Serializable cached, SessionImplementor session,
      Object owner) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void beforeAssemble(Serializable cached, SessionImplementor session) {
    throw new UnsupportedOperationException();
  }

  public Object hydrate(ResultSet rs, String[] names,
      SessionImplementor session, Object owner)
      throws HibernateException, SQLException {
    throw new UnsupportedOperationException();
  }

  public Object resolve(Object value, SessionImplementor session, Object owner)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object semiResolve(Object value, SessionImplementor session,
      Object owner) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Type getSemiResolvedType(SessionFactoryImplementor factory) {
    throw new UnsupportedOperationException();
  }

  public Object replace(Object original, Object target,
      SessionImplementor session, Object owner, Map copyCache)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object replace(Object original, Object target,
      SessionImplementor session, Object owner, Map copyCache,
      ForeignKeyDirection foreignKeyDirection) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean[] toColumnNullness(Object value, Mapping mapping) {
    throw new UnsupportedOperationException();
  }
}
