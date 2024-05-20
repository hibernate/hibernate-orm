/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.id.custom;

import java.lang.reflect.Member;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.generator.CustomIdGeneratorCreationContext;

/**
 * An example custom generator.
 */
//tag::identifiers-IdGeneratorType-example[]
public class CustomSequenceGenerator implements IdentifierGenerator {
//end::identifiers-IdGeneratorType-example[]
	public static int generationCount = 0;

	private final Identifier sequenceName;
	private final String sqlSelectFrag;

//tag::identifiers-IdGeneratorType-example[]

	public CustomSequenceGenerator(
			Sequence config,
			Member annotatedMember,
			CustomIdGeneratorCreationContext context) {
		//...
//end::identifiers-IdGeneratorType-example[]
		final String name = config.name();

		// ignore the other config for now...

		final Database database = context.getDatabase();
		final IdentifierHelper identifierHelper = database.getJdbcEnvironment().getIdentifierHelper();

		final org.hibernate.boot.model.relational.Sequence sequence = database.getDefaultNamespace().createSequence(
				identifierHelper.toIdentifier( name ),
				(physicalName) -> new org.hibernate.boot.model.relational.Sequence(
						null,
						database.getDefaultNamespace().getPhysicalName().getCatalog(),
						database.getDefaultNamespace().getPhysicalName().getSchema(),
						physicalName,
						1,
						50
				)
		);
		this.sequenceName = sequence.getName().getSequenceName();

		this.sqlSelectFrag = database
				.getDialect()
				.getSequenceSupport()
				.getSequenceNextValString( sequenceName.render( database.getDialect() ) );

//tag::identifiers-IdGeneratorType-example[]
	}

	@Override
	public Object generate(
			SharedSessionContractImplementor session,
			Object object) {
		//...
//end::identifiers-IdGeneratorType-example[]
		generationCount++;
		try {
			final PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sqlSelectFrag );
			try {
				final ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st, sqlSelectFrag );
				try {
					rs.next();
					return rs.getInt( 1 );
				}
				finally {
					try {
						session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, st );
					}
					catch( Throwable ignore ) {
						// intentionally empty
					}
				}
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}

		}
		catch ( SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not get next sequence value",
					sqlSelectFrag
			);
		}
	}
//tag::identifiers-IdGeneratorType-example[]
}
//end::identifiers-IdGeneratorType-example[]
