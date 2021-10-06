/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;
import org.hibernate.usertype.StaticUserTypeSupport;
import org.hibernate.usertype.UserTypeSupport;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringsMapType extends StaticUserTypeSupport {
    public CommaDelimitedStringsMapType() {
        super(
                new CommaDelimitedStringMapJavaTypeDescriptor(),
                VarcharTypeDescriptor.INSTANCE
        );
    }

    @Override
    public Object nullSafeGet(
            ResultSet rs,
            int position,
            SharedSessionContractImplementor session,
            Object owner) throws SQLException {
        final Object extracted = getJdbcValueExtractor().extract( rs, position, session );
        if ( extracted == null ) {
            return null;
        }

        return getJavaType().fromString( (String) extracted );
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws SQLException {
        final String stringValue = getJavaType().toString( value );
        getJdbcValueBinder().bind( st, stringValue, index, session );
    }
}