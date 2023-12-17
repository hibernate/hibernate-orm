/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.produce.function;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.List;

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
import org.hibernate.type.descriptor.java.spi.JdbcTypeRecommendationException;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.hasDatePart;
import static org.hibernate.type.SqlTypes.hasTimePart;
import static org.hibernate.type.SqlTypes.isCharacterOrClobType;
import static org.hibernate.type.SqlTypes.isCharacterType;
import static org.hibernate.type.SqlTypes.isEnumType;
import static org.hibernate.type.SqlTypes.isIntegral;
import static org.hibernate.type.SqlTypes.isNumericType;
import static org.hibernate.type.SqlTypes.isSpatialType;
import static org.hibernate.type.SqlTypes.isTemporalType;
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
	private static final int ENUM_UNKNOWN_JDBC_TYPE = -101977;

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
			JdbcTypeIndicators indicators = typeConfiguration.getCurrentBaseSqlTypeIndicators();
			SqmExpressible<?> nodeType = argument.getNodeType();
			FunctionParameterType type = count < types.length ? types[count++] : types[types.length - 1];
			if ( nodeType != null && type != FunctionParameterType.ANY ) {
				JavaType<?> javaType = nodeType.getRelationalJavaType();
				if (javaType != null) {
					checkArgumentType( functionName, count, argument, indicators, type, javaType );
				}
				switch (type) {
					case TEMPORAL_UNIT:
						if ( !(argument instanceof SqmExtractUnit) && !(argument instanceof SqmDurationUnit) ) {
							throwError(type, Object.class, functionName, count);
						}
						break;
					// the following are not really necessary for the functions we have today
					// since collations and trim specifications have special parser rules,
					// but at the very least this is an assertion that we don't get given
					// something crazy by the parser
					case TRIM_SPEC:
						if ( !(argument instanceof SqmTrimSpecification) ) {
							throwError(type, Object.class, functionName, count);
						}
						break;
					case COLLATION:
						if ( !(argument instanceof SqmCollation) ) {
							throwError(type, Object.class, functionName, count);
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
			JdbcTypeIndicators indicators,
			FunctionParameterType type,
			JavaType<?> javaType) {
		if ( !isUnknown( javaType ) ) {
			DomainType<?> domainType = argument.getExpressible().getSqmType();
			if ( domainType instanceof JdbcMapping ) {
				checkArgumentType(
						count, functionName, type,
						((JdbcMapping) domainType).getJdbcType().getDefaultSqlTypeCode(),
						javaType.getJavaTypeClass()
				);
			}
			else {
				//TODO: this branch is now probably obsolete and can be deleted!
				try {
					checkArgumentType(
							count, functionName, type,
							getJdbcType( indicators, javaType ),
							javaType.getJavaTypeClass()
					);
				}
				catch (JdbcTypeRecommendationException e) {
					// it's a converter or something like that, and we will check it later
				}
			}
		}
	}

	private int getJdbcType(JdbcTypeIndicators indicators, JavaType<?> javaType) {
		if ( javaType.getJavaTypeClass().isEnum() ) {
			// we can't tell if the enum is mapped STRING or ORDINAL
			return ENUM_UNKNOWN_JDBC_TYPE;
		}
		else {
			return javaType.getRecommendedJdbcType( indicators ).getDefaultSqlTypeCode();
		}
	}

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
						mapping.getJdbcType().getDefaultSqlTypeCode(),
						mapping.getJavaTypeDescriptor().getJavaType()
				);
			}
		}
		return paramNumber;
	}

	private static void checkArgumentType(int paramNumber, String functionName, FunctionParameterType type, int code, Type javaType) {
		switch (type) {
			case COMPARABLE:
				if ( !isCharacterType(code) && !isTemporalType(code) && !isNumericType(code) && !isEnumType( code )
						// both Java and the database consider UUIDs
						// comparable, so go ahead and accept them
						&& code != UUID
						// as a special case, we consider a binary column
						// comparable when it is mapped by a Java UUID
						&& !( javaType == java.util.UUID.class && code == Types.BINARY ) ) {
					throwError(type, javaType, functionName, paramNumber);
				}
				break;
			case STRING:
				if ( !isCharacterType(code) && !isEnumType(code) ) {
					throwError(type, javaType, functionName, paramNumber);
				}
				break;
			case STRING_OR_CLOB:
				if ( !isCharacterOrClobType(code) ) {
					throwError(type, javaType, functionName, paramNumber);
				}
				break;
			case NUMERIC:
				if ( !isNumericType(code) ) {
					throwError(type, javaType, functionName, paramNumber);
				}
				break;
			case INTEGER:
				if ( !isIntegral(code) ) {
					throwError(type, javaType, functionName, paramNumber);
				}
				break;
			case BOOLEAN:
				// ugh, need to be careful here, need to accept all the
				// JDBC type codes that a Dialect might use for BOOLEAN
				if ( code != BOOLEAN && code != BIT && code != TINYINT && code != SMALLINT ) {
					throwError(type, javaType, functionName, paramNumber);
				}
				break;
			case TEMPORAL:
				if ( !isTemporalType(code) ) {
					throwError(type, javaType, functionName, paramNumber);
				}
				break;
			case DATE:
				if ( !hasDatePart(code) ) {
					throwError(type, javaType, functionName, paramNumber);
				}
				break;
			case TIME:
				if ( !hasTimePart(code) ) {
					throwError(type, javaType, functionName, paramNumber);
				}
				break;
			case SPATIAL:
				if ( !isSpatialType( code ) ) {
					throwError( type, javaType, functionName, paramNumber );
				}
		}
	}

	private static void throwError(FunctionParameterType type, Type javaType, String functionName, int paramNumber) {
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
