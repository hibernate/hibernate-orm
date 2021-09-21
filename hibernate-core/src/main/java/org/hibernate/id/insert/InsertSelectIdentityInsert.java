/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;
import org.hibernate.dialect.Dialect;

/**
 * Specialized IdentifierGeneratingInsert which appends the database
 * specific clause which signifies to return generated IDENTITY values
 * to the end of the insert statement.
 * 
 * @author Steve Ebersole
 */
public class InsertSelectIdentityInsert extends IdentifierGeneratingInsert {
	public InsertSelectIdentityInsert(Dialect dialect) {
		super( dialect );
	}

	public String toStatementString() {
		return getDialect().getIdentityColumnSupport().appendIdentitySelectToInsert( super.toStatementString() );
	}
}
