package org.hibernate.tool.jdbc2cfg.OverrideBinder;

import org.hibernate.usertype.UserType;

public class SomeUserType implements UserType<String>{

	@Override public int getSqlType() { return 0; }
	@Override public Class<String> returnedClass() { return null; }
	@Override public String deepCopy(String value) { return null; }
	@Override public boolean isMutable() { return false; }

}
