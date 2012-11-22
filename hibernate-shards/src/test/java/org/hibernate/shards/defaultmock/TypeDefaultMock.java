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
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.relational.Size;
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

    @Override
    public boolean isAssociationType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCollectionType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEntityType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAnyType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isComponentType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getColumnSpan(Mapping mapping) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] sqlTypes(Mapping mapping) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Size[] dictatedSizes(Mapping mapping) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Size[] defaultSizes(Mapping mapping) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class getReturnedClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isXMLElement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSame(Object x, Object y) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEqual(Object x, Object y) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHashCode(Object x) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHashCode(Object x, SessionFactoryImplementor factory) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compare(Object x, Object y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDirty(Object old, Object current, SessionImplementor session) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SessionImplementor session) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isModified(Object dbState, Object currentState, boolean[] checkable, SessionImplementor session) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session) throws HibernateException, SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object deepCopy(Object value, SessionFactoryImplementor factory) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable disassemble(Object value, SessionImplementor session, Object owner) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeAssemble(Serializable cached, SessionImplementor session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object resolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object semiResolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getSemiResolvedType(SessionFactoryImplementor factory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache, ForeignKeyDirection foreignKeyDirection) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] toColumnNullness(Object value, Mapping mapping) {
        throw new UnsupportedOperationException();
    }
}
