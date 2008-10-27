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
package org.jboss.envers.entities;

import org.hibernate.usertype.UserType;
import org.hibernate.HibernateException;
import org.jboss.envers.RevisionType;

import java.sql.ResultSet;
import java.sql.Types;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.io.Serializable;

/**
 * A hibernate type for the {@link RevisionType} enum.
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionTypeType implements UserType {
    private static final int[] SQL_TYPES = { Types.TINYINT };

    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    public Class returnedClass() {
        return RevisionType.class;
    }

    public RevisionType nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
        byte representation = (byte) resultSet.getInt(names[0]);
        RevisionType result = null;

        if (!resultSet.wasNull()) {
            result = RevisionType.fromRepresentation(representation);
        }

        return result;
    }

    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
        if (null == value) {
            preparedStatement.setNull(index, Types.TINYINT);
        } else {
            preparedStatement.setInt(index, ((RevisionType) value).getRepresentation());
        }
    }

    public Object deepCopy(Object value) throws HibernateException{
        return value;
    }

    public boolean isMutable() {
        return false;
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable)value;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        //noinspection ObjectEquality
        if (x == y) {
            return true;
        }

        if (null == x || null == y) {
            return false;
        }

        return x.equals(y);
    }
}


