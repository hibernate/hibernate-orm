/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.sql.AnyTypeDescriptor;
import org.hibernate.type.descriptor.java.NullTypeDescriptor;

/**
 * A type that maps {@link java.sql.Types#NULL NULL}
 *
 * @author Yordan Gigov
 */
public class NullType
        extends AbstractSingleColumnStandardBasicType<Object>
        implements DiscriminatorType<Object> {

    public static final NullType INSTANCE = new NullType();

    public NullType() {
        super( AnyTypeDescriptor.INSTANCE, NullTypeDescriptor.INSTANCE );
    }

    public String getName() {
        return "null";
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }

    public String objectToSQLString(Object value, Dialect dialect) throws Exception {
        return "NULL";
    }

    public String stringToObject(String xml) throws Exception {
        return xml;
    }

    public String toString(String value) {
        return value;
    }

}