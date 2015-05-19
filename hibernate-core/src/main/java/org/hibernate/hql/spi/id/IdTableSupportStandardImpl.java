/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id;

/**
* @author Steve Ebersole
*/
public class IdTableSupportStandardImpl implements IdTableSupport {
	/**
	 * Singleton access
	 */
	public static final IdTableSupportStandardImpl INSTANCE = new IdTableSupportStandardImpl();

	@Override
	public String generateIdTableName(String baseName) {
		return "HT_" + baseName;
	}

	@Override
	public String getCreateIdTableCommand() {
		return "create table";
	}

	@Override
	public String getCreateIdTableStatementOptions() {
		return null;
	}

	@Override
	public String getDropIdTableCommand() {
		return "drop table";
	}
}
