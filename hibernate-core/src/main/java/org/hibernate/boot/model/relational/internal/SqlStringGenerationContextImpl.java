/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational.internal;

import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;

public class SqlStringGenerationContextImpl
		implements SqlStringGenerationContext {

	public static SqlStringGenerationContext forTests(JdbcEnvironment jdbcEnvironment) {
		return new SqlStringGenerationContextImpl( jdbcEnvironment );
	}

	private final Dialect dialect;
	private final QualifiedObjectNameFormatter qualifiedObjectNameFormatter;

	public SqlStringGenerationContextImpl(JdbcEnvironment jdbcEnvironment) {
		this.dialect = jdbcEnvironment.getDialect();
		this.qualifiedObjectNameFormatter = jdbcEnvironment.getQualifiedObjectNameFormatter();
	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public String format(QualifiedTableName qualifiedName) {
		return qualifiedObjectNameFormatter.format( qualifiedName, dialect );
	}

	@Override
	public String format(QualifiedSequenceName qualifiedName) {
		return qualifiedObjectNameFormatter.format( qualifiedName, dialect );
	}

	@Override
	public String format(QualifiedName qualifiedName) {
		return qualifiedObjectNameFormatter.format( qualifiedName, dialect );
	}

}
