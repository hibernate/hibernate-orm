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
		return getDialect().appendIdentitySelectToInsert( super.toStatementString() );
	}
}
