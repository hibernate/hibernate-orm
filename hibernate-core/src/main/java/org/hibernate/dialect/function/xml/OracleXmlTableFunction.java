/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.dialect.function.json.OracleJsonValueFunction.isEncodedBoolean;

/**
 * Oracle xmltable function.
 */
public class OracleXmlTableFunction extends XmlTableFunction {

	public OracleXmlTableFunction(TypeConfiguration typeConfiguration) {
		super( false, new OracleXmlTableSetReturningFunctionTypeResolver(), typeConfiguration );
	}

	@Override
	protected String determineColumnType(CastTarget castTarget, SqlAstTranslator<?> walker) {
		final String typeName = super.determineColumnType( castTarget, walker );
		return switch ( typeName ) {
			// clob is not supported as column type for xmltable
			case "clob" -> "varchar2(" + walker.getSessionFactory().getJdbcServices().getDialect().getMaxVarcharLength() + ")";
			case "number(1,0)" -> isEncodedBoolean( castTarget.getJdbcMapping() ) ? "varchar2(5)" : typeName;
			default -> typeName;
		};
	}

	private static class OracleXmlTableSetReturningFunctionTypeResolver extends XmlTableSetReturningFunctionTypeResolver {
		@Override
		protected void addSelectableMapping(List<SelectableMapping> selectableMappings, String name, JdbcMapping type, SqmToSqlAstConverter converter) {
			if ( isEncodedBoolean( type ) ) {
				//noinspection unchecked
				final JdbcLiteralFormatter<Object> jdbcLiteralFormatter = type.getJdbcLiteralFormatter();
				final SessionFactoryImplementor sessionFactory = converter.getCreationContext().getSessionFactory();
				final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
				final WrapperOptions wrapperOptions = sessionFactory.getWrapperOptions();
				final Object trueValue = type.convertToRelationalValue( true );
				final Object falseValue = type.convertToRelationalValue( false );
				final String trueFragment = jdbcLiteralFormatter.toJdbcLiteral( trueValue, dialect, wrapperOptions );
				final String falseFragment = jdbcLiteralFormatter.toJdbcLiteral( falseValue, dialect, wrapperOptions );
				selectableMappings.add( new SelectableMappingImpl(
						"",
						name,
						new SelectablePath( name ),
						"decode(" + Template.TEMPLATE + "." + name + ",'true'," + trueFragment + ",'false'," + falseFragment + ")",
						null,
						"varchar2(5)",
						null,
						null,
						null,
						null,
						false,
						false,
						false,
						false,
						false,
						false,
						type
				));
			}
			else {
				super.addSelectableMapping( selectableMappings, name, type, converter );
			}
		}
	}
}
