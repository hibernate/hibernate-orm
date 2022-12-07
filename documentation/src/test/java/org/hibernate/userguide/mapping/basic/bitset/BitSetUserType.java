/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic.bitset;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.BitSet;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import org.jboss.logging.Logger;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-custom-type-BitSetUserType-example[]
public class BitSetUserType implements UserType<BitSet> {

    public static final BitSetUserType INSTANCE = new BitSetUserType();

    private static final Logger log = Logger.getLogger(BitSetUserType.class);

    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public Class<BitSet> returnedClass() {
        return BitSet.class;
    }

    @Override
    public boolean equals(BitSet x, BitSet y)
			throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(BitSet x)
			throws HibernateException {
        return Objects.hashCode(x);
    }

    @Override
    public BitSet nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        String columnValue = (String) rs.getObject(position);
        if (rs.wasNull()) {
            columnValue = null;
        }

        log.debugv("Result set column {0} value is {1}", position, columnValue);
        return BitSetHelper.stringToBitSet(columnValue);
    }

    @Override
    public void nullSafeSet(
            PreparedStatement st, BitSet value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            log.debugv("Binding null to parameter {0} ",index);
            st.setNull(index, Types.VARCHAR);
        }
        else {
            String stringValue = BitSetHelper.bitSetToString((BitSet) value);
            log.debugv("Binding {0} to parameter {1} ", stringValue, index);
            st.setString(index, stringValue);
        }
    }

    @Override
    public BitSet deepCopy(BitSet value)
			throws HibernateException {
        return value == null ? null :
            BitSet.valueOf(BitSet.class.cast(value).toLongArray());
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Object disassemble(BitSet value)
			throws HibernateException {
        return deepCopy(value);
    }

    @Override
    public BitSet assemble(Object cached, Object owner)
			throws HibernateException {
        return deepCopy( (BitSet) cached );
    }

    @Override
    public BitSet replace(BitSet original, BitSet target, Object owner)
			throws HibernateException {
        return deepCopy(original);
    }
}
//end::basic-custom-type-BitSetUserType-example[]
