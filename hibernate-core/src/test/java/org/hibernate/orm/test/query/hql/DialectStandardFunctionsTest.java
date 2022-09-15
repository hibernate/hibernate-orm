/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardFunctions;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Christian Beikov
 */
@ServiceRegistry
@DomainModel
@SessionFactory
public class DialectStandardFunctionsTest {

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class)
	@SkipForDialect(dialectClass = HSQLDialect.class)
	@SkipForDialect(dialectClass = SybaseASEDialect.class)
	public void testStandardFunctionsRegistered(SessionFactoryScope scope) throws Exception {
		testStandardFunctionsRegistered( scope, new String[0] );
	}

	@Test
	@RequiresDialect(DerbyDialect.class)
	public void testStandardFunctionsRegisteredDerby(SessionFactoryScope scope) throws Exception {
		// We don't consider Derby to be a "real production" database, hence we ignore some missing functions.
		// Note that we could solve these issues by creating custom functions, but why bother?
		testStandardFunctionsRegistered(
				scope,
				// Apache Derby has no support for bitwise operators/functions
				StandardFunctions.BITAND,
				StandardFunctions.BITOR,
				StandardFunctions.BITXOR,
				StandardFunctions.BITNOT,

				// Apache Derby has no support for CHR/CHAR, ASCII and REPEAT
				StandardFunctions.CHR,
				StandardFunctions.CHAR,
				StandardFunctions.ASCII,
				StandardFunctions.REPEAT,

				// Apache Derby has no support for window functions
				StandardFunctions.ROW_NUMBER,
				StandardFunctions.LEAD,
				StandardFunctions.LAG,
				StandardFunctions.FIRST_VALUE,
				StandardFunctions.LAST_VALUE,
				StandardFunctions.NTH_VALUE,

				// Apache Derby has no support for the listagg aggregate
				StandardFunctions.LISTAGG
		);
	}

	@Test
	@RequiresDialect(HSQLDialect.class)
	public void testStandardFunctionsRegisteredHSQLDB(SessionFactoryScope scope) throws Exception {
		// We don't consider HSQLDB to be a "real production" database, hence we ignore some missing functions.
		testStandardFunctionsRegistered(
				scope,
				// HSQLDB has no support for window functions
				StandardFunctions.ROW_NUMBER,
				StandardFunctions.LEAD,
				StandardFunctions.LAG,
				StandardFunctions.FIRST_VALUE,
				StandardFunctions.LAST_VALUE,
				StandardFunctions.NTH_VALUE
		);
	}

	@Test
	@RequiresDialect(SybaseASEDialect.class)
	public void testStandardFunctionsRegisteredSybaseASE(SessionFactoryScope scope) throws Exception {
		// Even though we consider Sybase ASE to be a "real production" database,
		// the latest version still hasn't caught up with every other major database.
		// Maybe Sybase ASE 16.1 will finally add support for these features,
		// but for now, we'll just ignore these missing functions
		testStandardFunctionsRegistered(
				scope,
				// Sybase ASE has no support for window functions
				StandardFunctions.ROW_NUMBER,
				StandardFunctions.LEAD,
				StandardFunctions.LAG,
				StandardFunctions.FIRST_VALUE,
				StandardFunctions.LAST_VALUE,
				StandardFunctions.NTH_VALUE,

				// Sybase ASE has no support for the listagg aggregate
				StandardFunctions.LISTAGG
		);
	}

	private void testStandardFunctionsRegistered(SessionFactoryScope scope, String... ignoredFunctions) throws Exception {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final SqmFunctionRegistry sqmFunctionRegistry = sessionFactory.getQueryEngine().getSqmFunctionRegistry();
		final List<String> missingFunctions = new ArrayList<>();
		for ( Field field : StandardFunctions.class.getFields() ) {
			final String functionName = (String) field.get( null );
			final SqmFunctionDescriptor functionDescriptor = sqmFunctionRegistry.findFunctionDescriptor( functionName );
			if ( functionDescriptor == null ) {
				missingFunctions.add( functionName );
			}
		}
		final List<String> ignoredExistingFunctions = new ArrayList<>();
		for ( String ignoredFunction : ignoredFunctions ) {
			if ( !missingFunctions.remove( ignoredFunction ) ) {
				ignoredExistingFunctions.add( ignoredFunction );
			}
		}

		if ( !missingFunctions.isEmpty() || !ignoredExistingFunctions.isEmpty() ) {
			final StringBuilder sb = new StringBuilder();
			if ( !missingFunctions.isEmpty() ) {
				sb.append( "Dialect [" ).append( sessionFactory.getJdbcServices().getDialect().getClass().getName() )
						.append( "] is missing registrations for StandardFunctions:" );
				for ( String missingFunction : missingFunctions ) {
					sb.append( "\n * " ).append( missingFunction );
				}
			}
			if ( !ignoredExistingFunctions.isEmpty() ) {
				if ( sb.length() != 0 ) {
					sb.append( '\n' );
				}
				sb.append( "Dialect [" ).append( sessionFactory.getJdbcServices().getDialect().getClass().getName() )
						.append( "] has registrations for ignored StandardFunctions:" );
				for ( String ignoredExistingFunction : ignoredExistingFunctions ) {
					sb.append( "\n * " ).append( ignoredExistingFunction );
				}
			}
			Assertions.fail( sb.toString() );
		}
	}

}