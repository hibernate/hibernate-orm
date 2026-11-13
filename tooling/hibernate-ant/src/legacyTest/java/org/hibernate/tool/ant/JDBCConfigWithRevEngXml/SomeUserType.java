package org.hibernate.tool.ant.JDBCConfigWithRevEngXml;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.usertype.UserType;

public class SomeUserType implements UserType<String>{

	@Override public int getSqlType() { return 0; }
	@Override public @Nonnull Class<String> returnedClass() { return String.class; }
	@Override public @Nullable String deepCopy(@Nullable String value) { return null; }
	@Override public boolean isMutable() { return false; }

}
