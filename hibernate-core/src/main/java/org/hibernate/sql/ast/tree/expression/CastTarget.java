/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Gavin King
 */
public class CastTarget implements Expression, SqlAstNode, SqlTypedMapping {
	private final JdbcMapping type;
	private final String sqlType;
	private final Long length;
	private final Integer precision;
	private final Integer scale;

	public CastTarget(JdbcMapping type) {
		this( type, null, null, null );
	}

	public CastTarget(JdbcMapping type, Long length, Integer precision, Integer scale) {
		this( type, null, length, precision, scale );
	}

	public CastTarget(JdbcMapping type, String sqlType, Long length, Integer precision, Integer scale) {
		this.type = type;
		this.sqlType = sqlType;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
	}

	public String getSqlType() {
		return sqlType;
	}

	@Override
	public String getColumnDefinition() {
		return sqlType;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return type;
	}

	public Long getLength() {
		return length;
	}

	public Integer getPrecision() {
		return precision;
	}

	@Override
	public Integer getTemporalPrecision() {
		return null;
	}

	public Integer getScale() {
		return scale;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCastTarget( this );
	}

}
