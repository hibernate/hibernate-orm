/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.custom;

import java.lang.reflect.Member;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.IdentifierGenerator;

/**
 * An example custom generator.
 */
//tag::identifiers-IdGeneratorType-example[]
public class CustomSequenceGenerator implements IdentifierGenerator {
//end::identifiers-IdGeneratorType-example[]
	public static int generationCount = 0;

	private final String sqlSelectFrag;

//tag::identifiers-IdGeneratorType-example[]

	public CustomSequenceGenerator(
			Sequence config,
			Member annotatedMember,
			GeneratorCreationContext context) {
		//...
//end::identifiers-IdGeneratorType-example[]
		final String name = config.name();

		// ignore the other config for now...

		final Database database = context.getDatabase();
		final IdentifierHelper identifierHelper = database.getJdbcEnvironment().getIdentifierHelper();

		final Identifier identifier = identifierHelper.toIdentifier( name );
		final Namespace defaultNamespace = database.getDefaultNamespace();
		org.hibernate.boot.model.relational.Sequence sequence = defaultNamespace.locateSequence( identifier );
		if ( sequence == null ) {
			sequence = defaultNamespace.createSequence(
					identifier,
					(physicalName) -> new org.hibernate.boot.model.relational.Sequence(
							null,
							defaultNamespace.getPhysicalName().catalog(),
							defaultNamespace.getPhysicalName().schema(),
							physicalName,
							1,
							50
					)
			);
		}

		this.sqlSelectFrag =
				database.getDialect().getSequenceSupport()
						.getSequenceNextValString( sequence.getName().getSequenceName().render( database.getDialect() ) );

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
