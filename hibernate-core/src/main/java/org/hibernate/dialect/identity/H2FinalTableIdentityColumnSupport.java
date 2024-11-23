/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.identity;

/**
 * Identity column support for H2 2+ versions
 * @author Jan Schatteman
 */
public class H2FinalTableIdentityColumnSupport extends H2IdentityColumnSupport {

	public static final H2FinalTableIdentityColumnSupport INSTANCE = new H2FinalTableIdentityColumnSupport();

	private H2FinalTableIdentityColumnSupport() {
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return true;
	}

	@Override
	public String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
		return "select " + identityColumnName + " from final table ( " + insertString + " )";
	}
}
