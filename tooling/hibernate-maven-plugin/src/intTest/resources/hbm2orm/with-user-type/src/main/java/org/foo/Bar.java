package org.foo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.usertype.UserType;

public class Bar implements UserType<Integer> {

    @Override
    public int getSqlType() {
        return 0;
    }

    @Override
    @Nonnull
    public Class<Integer> returnedClass() {
        return Integer.class;
    }

    @Override
    @Nullable
    public Integer deepCopy(@Nullable Integer integer) {
        return integer;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

}
