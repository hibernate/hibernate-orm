/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.Insert;
import org.hibernate.generator.OnExecutionGenerator;

/**
 * Specialized {@link IdentifierGeneratingInsert} which appends the database
 * specific clause which signifies to return generated {@code IDENTITY} values
 * to the end of the insert statement.
 * 
 * @author Steve Ebersole
 *
 * @deprecated This is not used anymore in any of the
 * {@link org.hibernate.generator.values.GeneratedValuesMutationDelegate} implementations.
 */
@Deprecated( since = "6.5" )
public class InsertSelectIdentityInsert extends IdentifierGeneratingInsert {
	protected String identityColumnName;

	public InsertSelectIdentityInsert(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	public Insert addIdentityColumn(String columnName) {
		identityColumnName = columnName;
		return super.addIdentityColumn( columnName );
	}

	@Override
	public Insert addGeneratedColumns(String[] columnNames, OnExecutionGenerator generator) {
		if ( columnNames.length != 1 ) {
			//TODO: Should this allow multiple columns? Would require changing
			//      IdentityColumnSupport.appendIdentitySelectToInsert()
			throw new MappingException("wrong number of generated columns");
		}
		identityColumnName = columnNames[0];
		return super.addGeneratedColumns( columnNames, generator );
	}

	public String toStatementString() {
		return getDialect().getIdentityColumnSupport()
				.appendIdentitySelectToInsert( identityColumnName, super.toStatementString() );
	}
}
