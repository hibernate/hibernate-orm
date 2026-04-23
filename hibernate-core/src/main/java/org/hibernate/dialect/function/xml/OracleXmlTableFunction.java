/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import org.hibernate.dialect.Dialect;
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

import static org.hibernate.dialect.function.array.DdlTypeHelper.getNarrowCastTypeName;
import static org.hibernate.dialect.function.array.DdlTypeHelper.removeUnresolvedTypeArguments;
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
		// Oracle's xmltable() columns clause rejects LOB types; use the narrow
		// cast name which maps CLOB/NCLOB/BLOB to sized VARCHAR2/NVARCHAR2/RAW
		final String typeName =
				removeUnresolvedTypeArguments( getNarrowCastTypeName( castTarget,
						walker.getSessionFactory().getTypeConfiguration() )
		);
		return switch ( typeName ) {
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
				final Dialect dialect = converter.getCreationContext().getDialect();
				final WrapperOptions wrapperOptions = converter.getCreationContext().getWrapperOptions();
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
						null,
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
