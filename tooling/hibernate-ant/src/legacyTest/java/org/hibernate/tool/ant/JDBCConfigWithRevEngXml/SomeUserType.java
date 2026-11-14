package org.hibernate.tool.ant.JDBCConfigWithRevEngXml;

import java.sql.Types;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.usertype.UserType;

public class SomeUserType implements UserType<Something> {

	@Override public int getSqlType() { return Types.VARCHAR; }
	@Override public @Nonnull Class<Something> returnedClass() { return Something.class; }
	@Override public @Nullable Something deepCopy(@Nullable Something value) { return value; }
	@Override public boolean isMutable() { return false; }

}
