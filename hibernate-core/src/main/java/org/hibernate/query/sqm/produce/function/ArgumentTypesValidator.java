/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.produce.function;

import java.lang.reflect.Type;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmCollation;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicType;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.COMPARABLE;
import static org.hibernate.type.descriptor.java.JavaTypeHelper.isUnknown;


/**
 * Typechecks the arguments of HQL functions based on the assigned JDBC types.
 *
 * @apiNote Originally, the main purpose for doing this was that we wanted to be
 *          able to check named queries at startup or build time, and we wanted
 *          to be able to check all queries in the IDE. But since Hibernate 6
 *          it's of more general importance.
 *
 * @implNote Access to the {@link MappingMetamodel} is very problematic here,
 *           since we are sometimes called in a context where we have not built
 *           a {@link org.hibernate.internal.SessionFactoryImpl}, and therefore
 *           we have no persisters.
 *
 * @author Gavin King
 */
public class ArgumentTypesValidator implements ArgumentsValidator {
	// a JDBC type code of an enum when we don't know if it's mapped STRING or ORDINAL
	// this number has to be distinct from every code in SqlTypes!
//	private static final int ENUM_UNKNOWN_JDBC_TYPE = -101977;

	final ArgumentsValidator delegate;
	private final FunctionParameterType[] types;

	public ArgumentTypesValidator(ArgumentsValidator delegate, FunctionParameterType... types) {
		this.types = types;
		if ( delegate == null ) {
			delegate = StandardArgumentsValidators.exactly(types.length);
		}
		this.delegate = delegate;
	}

	/**
	 * We do an initial validation phase with just the SQM tree, even though we don't
	 * have all typing information available here (in particular, we don't have the
	 * final JDBC type codes for things with converters) because this is the phase
	 * that is run at startup for named queries, and can be done in an IDE.
	 */
	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			TypeConfiguration typeConfiguration) {
		delegate.validate( arguments, functionName, typeConfiguration);
		int count = 0;
		for (SqmTypedNode<?> argument : arguments) {
//			JdbcTypeIndicators indicators = typeConfiguration.getCurrentBaseSqlTypeIndicators();
			SqmExpressible<?> nodeType = argument.getNodeType();
			FunctionParameterType type = count < types.length ? types[count++] : types[types.length - 1];
			if ( nodeType != null && type != FunctionParameterType.ANY ) {
				JavaType<?> javaType = nodeType.getRelationalJavaType();
				if (javaType != null) {
					checkArgumentType( functionName, count, argument, type, javaType );
				}
				switch (type) {
					case TEMPORAL_UNIT:
						if ( !(argument instanceof SqmExtractUnit) && !(argument instanceof SqmDurationUnit) ) {
							throwError(type, Object.class, null, functionName, count);
						}
						break;
					// the following are not really necessary for the functions we have today
					// since collations and trim specifications have special parser rules,
					// but at the very least this is an assertion that we don't get given
					// something crazy by the parser
					case TRIM_SPEC:
						if ( !(argument instanceof SqmTrimSpecification) ) {
							throwError(type, Object.class, null, functionName, count);
						}
						break;
					case COLLATION:
						if ( !(argument instanceof SqmCollation) ) {
							throwError(type, Object.class, null, functionName, count);
						}
						break;
					case NO_UNTYPED:
						if ( argument instanceof SqmLiteralNull<?> ) {
							throw new FunctionArgumentException(
									String.format(
											"Parameter %d of function '%s()' does not permit untyped expressions like null literals. Please cast the expression to a type",
											count,
											functionName
									)
							);
						}
						break;
				}
			}
			else {
				//TODO: appropriate error?
			}
		}
	}

	private void checkArgumentType(
			String functionName,
			int count,
			SqmTypedNode<?> argument,
			FunctionParameterType type,
			JavaType<?> javaType) {
		if ( !isUnknown( javaType ) ) {
			DomainType<?> domainType = argument.getExpressible().getSqmType();
			if ( domainType instanceof JdbcMapping ) {
				JdbcMapping jdbcMapping = (JdbcMapping) domainType;
				checkArgumentType(
						count, functionName, type,
						jdbcMapping.getJdbcType(),
						javaType.getJavaTypeClass()
				);
			}
//			else {
//				//TODO: this branch is now probably obsolete and can be deleted!
//				try {
//					checkArgumentType(
//							count, functionName, type,
//							getJdbcType( indicators, javaType ),
//							null,
//							javaType.getJavaTypeClass()
//					);
//				}
//				catch (JdbcTypeRecommendationException e) {
//					// it's a converter or something like that, and we will check it later
//				}
//			}
		}
	}

//	private int getJdbcType(JdbcTypeIndicators indicators, JavaType<?> javaType) {
//		if ( javaType.getJavaTypeClass().isEnum() ) {
//			// we can't tell if the enum is mapped STRING or ORDINAL
//			return ENUM_UNKNOWN_JDBC_TYPE;
//		}
//		else {
//			return javaType.getRecommendedJdbcType( indicators ).getDefaultSqlTypeCode();
//		}
//	}

	/**
	 * This is the final validation phase with the fully-typed SQL nodes. Note that these
	 * checks are much less useful, occurring "too late", right before we execute the
	 * query and get an error from the database. However, they help in the sense of (a)
	 * resulting in more consistent/understandable error messages, and (b) protecting the
	 * user from writing queries that depend on generally-unportable implicit type
	 * conversions happening at the database level. (Implicit type conversions between
	 * numeric types are portable, and are not prohibited here.)
	 */
	@Override
	public void validateSqlTypes(List<? extends SqlAstNode> arguments, String functionName) {
		int count = 0;
		for ( SqlAstNode argument : arguments ) {
			if ( argument instanceof Expression ) {
				final Expression expression = (Expression) argument;
				final JdbcMappingContainer expressionType = expression.getExpressionType();
				if (expressionType != null) {
					if ( isUnknownExpressionType( expressionType ) ) {
						count += expressionType.getJdbcTypeCount();
					}
					else {
						count = validateArgument( count, expressionType, functionName );
					}
				}
			}
		}
	}

	/**
	 * We can't validate some expressions involving parameters / unknown functions.
	 */
	private static boolean isUnknownExpressionType(JdbcMappingContainer expressionType) {
		return expressionType instanceof JavaObjectType
			|| expressionType instanceof BasicType
				&& isUnknown( ((BasicType<?>) expressionType).getJavaTypeDescriptor() );
	}

	private int validateArgument(int paramNumber, JdbcMappingContainer expressionType, String functionName) {
		final int jdbcTypeCount = expressionType.getJdbcTypeCount();
		for ( int i = 0; i < jdbcTypeCount; i++ ) {
			final JdbcMapping mapping = expressionType.getJdbcMapping( i );
			FunctionParameterType type = paramNumber < types.length ? types[paramNumber++] : types[types.length - 1];
			if ( type != null ) {
				checkArgumentType(
						paramNumber,
						functionName,
						type,
						mapping.getJdbcType(),
						mapping.getJavaTypeDescriptor().getJavaType()
				);
			}
		}
		return paramNumber;
	}

	private static void checkArgumentType(
			int paramNumber, String functionName, FunctionParameterType type, JdbcType jdbcType, Type javaType) {
		if ( !isCompatible( type, jdbcType )
				// as a special case, we consider a binary column
				// comparable when it is mapped by a Java UUID
				&& !( type == COMPARABLE && isBinaryUuid( jdbcType, javaType ) ) ) {
			throwError( type, javaType, jdbcType.getFriendlyName(), functionName, paramNumber );
		}
	}

	private static boolean isBinaryUuid(JdbcType jdbcType, Type javaType) {
		return javaType == java.util.UUID.class
			&& jdbcType.isBinary();
	}

	@Internal
	private static boolean isCompatible(FunctionParameterType type, JdbcType jdbcType) {
		switch ( type ) {
			case COMPARABLE:
				return jdbcType.isComparable();
			case STRING:
				return jdbcType.isStringLikeExcludingClob();
			case STRING_OR_CLOB:
				return jdbcType.isString(); // should it be isStringLike()
			case NUMERIC:
				return jdbcType.isNumber();
			case INTEGER:
				return jdbcType.isInteger();
			case BOOLEAN:
				return jdbcType.isBoolean()
					// some Dialects map Boolean to SMALLINT or TINYINT
					// TODO: check with Dialect.getPreferredSqlTypeCodeForBoolean
					|| jdbcType.isSmallInteger();
			case TEMPORAL:
				return jdbcType.isTemporal();
			case DATE:
				return jdbcType.hasDatePart();
			case TIME:
				return jdbcType.hasTimePart();
			case SPATIAL:
				return jdbcType.isSpatial();
			default:
				// TODO: should we throw here?
				return true;
		}
	}

	private static void throwError(
			FunctionParameterType type, Type javaType, String sqlType, String functionName, int paramNumber) {
		if ( sqlType == null ) {
			throw new FunctionArgumentException(
					String.format(
							"Parameter %d of function '%s()' has type '%s', but argument is of type '%s'",
							paramNumber,
							functionName,
							type,
							javaType.getTypeName()
					)
			);
		}
		else {
			throw new FunctionArgumentException(
					String.format(
							"Parameter %d of function '%s()' has type '%s', but argument is of type '%s' mapped to '%s'",
							paramNumber,
							functionName,
							type,
							javaType.getTypeName(),
							sqlType
					)
			);
		}
	}

	@Override
	public String getSignature() {
		String sig = delegate.getSignature();
		for (int i=0; i<types.length; i++) {
			String argName = types.length == 1 ? "arg" : "arg" + i;
			sig = sig.replace(argName, types[i] + " " + argName);
		}
		return sig;
	}
}
