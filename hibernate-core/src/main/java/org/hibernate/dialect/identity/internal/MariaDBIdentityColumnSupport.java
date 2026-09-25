package org.hibernate.dialect.identity.internal;

/**
 * @author Marco Belladelli
 */
public class MariaDBIdentityColumnSupport extends MySQLIdentityColumnSupport {
	public static final MariaDBIdentityColumnSupport INSTANCE = new MariaDBIdentityColumnSupport();

	@Override
	public String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
		return insertString + " returning " + identityColumnName;
	}
}
