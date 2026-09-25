package org.foo;

import org.hibernate.usertype.UserType;

public class Bar implements UserType<Integer> {

    @Override
    public int getSqlType() {
        return 0;
    }

    @Override
    public Class<Integer> returnedClass() {
        return null;
    }

    @Override
    public Integer deepCopy(Integer integer) {
        return 0;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

}
