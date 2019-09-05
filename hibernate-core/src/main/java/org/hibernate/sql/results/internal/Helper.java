/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.results.SqlResultsLogger;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.JdbcValues;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static <R> RowReader<R> createRowReader(
			SessionFactoryImplementor sessionFactory,
			Callback callback,
			RowTransformer<R> rowTransformer,
			JdbcValues jdbcValues) {
		final List<Initializer> initializers = new ArrayList<>();

		final List<DomainResultAssembler> assemblers = jdbcValues.getValuesMapping().resolveAssemblers(
				getInitializerConsumer( initializers ),
				() -> sessionFactory
		);

		return new StandardRowReader<>(
				assemblers,
				initializers,
				rowTransformer,
				callback
		);
	}

	private static Consumer<Initializer> getInitializerConsumer(List<Initializer> initializers) {
		if ( SqlResultsLogger.INSTANCE.isDebugEnabled() ) {
			return initializer -> {
				SqlResultsLogger.INSTANCE.debug( "Adding initializer : " + initializer );
				initializers.add( initializer );
			};
		}
		else {
			return initializer -> {
				// noinspection Convert2MethodRef
				initializers.add( initializer );
			};
		}
	}

}
