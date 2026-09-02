/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

/// The complete, typed input supplied by Hibernate to create one SQL AST
/// translator.
///
/// A [SqlAstTranslatorFactory] uses the request subtype to select a supported
/// translator or translator family. The resulting translator is single-use and
/// must translate [#statement] using services from [#sessionFactory]. A factory
/// or translator must not retain this request beyond that translation.
///
/// @param <S> the SQL AST statement type
/// @param <O> the resulting JDBC operation type
///
/// @since 8.0
/// @author Steve Ebersole
public sealed interface SqlAstTranslationRequest<S extends Statement, O extends JdbcOperation>
		permits SqlAstTranslationRequest.Select,
				SqlAstTranslationRequest.QueryMutation,
				SqlAstTranslationRequest.ModelMutation {

	/// The SessionFactory whose services are used during translation.
	SessionFactoryImplementor sessionFactory();

	/// The statement to translate.
	S statement();

	/// Request to translate a query select into a [JdbcSelect].
	record Select(SessionFactoryImplementor sessionFactory, SelectStatement statement)
			implements SqlAstTranslationRequest<SelectStatement, JdbcSelect> {
		public Select {
			Objects.requireNonNull( sessionFactory, "sessionFactory" );
			Objects.requireNonNull( statement, "statement" );
		}
	}

	/// Request to translate a query-language update or delete into a
	/// [JdbcOperationQueryMutation].
	record QueryMutation(SessionFactoryImplementor sessionFactory, MutationStatement statement)
			implements SqlAstTranslationRequest<MutationStatement, JdbcOperationQueryMutation> {
		public QueryMutation {
			Objects.requireNonNull( sessionFactory, "sessionFactory" );
			Objects.requireNonNull( statement, "statement" );
		}
	}

	/// Request to translate a mapping-model table mutation into its matching JDBC
	/// mutation operation.
	record ModelMutation<O extends JdbcMutationOperation>(
			SessionFactoryImplementor sessionFactory,
			TableMutation<?> statement)
			implements SqlAstTranslationRequest<TableMutation<?>, O> {
		public ModelMutation {
			Objects.requireNonNull( sessionFactory, "sessionFactory" );
			Objects.requireNonNull( statement, "statement" );
		}
	}
}
