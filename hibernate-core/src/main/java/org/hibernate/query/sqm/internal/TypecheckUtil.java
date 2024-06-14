/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.TupleType;
import org.hibernate.metamodel.model.domain.internal.EntityDiscriminatorSqmPathSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.QueryParameterJavaObjectType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;

import static org.hibernate.type.descriptor.java.JavaTypeHelper.isUnknown;

/**
 * Functions for typechecking comparison expressions and assignments in the SQM tree.
 * A comparison expression is any predicate like {@code x = y} or {@code x > y}. An
 * assignment is an element of the {@code set} clause in an {@code update} query, or
 * an element of the {@code values} list in an {@code insert query}.
 * <p>
 * The rules here are not the same as the rules for the Java language, nor are they
 * identical to the rules for SQL. For example:
 * <ul>
 * <li>In Java, a comparison expression like {@code '1'.equals(1)} is well-typed, and
 *     evaluates to {@code false}. In most SQL dialects, this expression evaluates to
 *     {@code true}, via implicit type conversions. Here we <em>reject</em> such
 *     comparison expressions.
 * <li>In Java, if two classes are related by inheritance, then one is assignable to
 *     the other. But here, this assignment is only legal if the two classes are entity
 *     types belonging to the same mapped entity inheritance hierarchy.
 * <li>On the other hand, in Java, {@code java.sql.Date} and {@code java.time.LocalDate}
 *     may not be compared nor assigned. But here they're considered inter-comparable
 *     and inter-assignable.
 * </ul>
 * <p>
 * Two basic types are considered comparable if they map to the same "family" of
 * {@linkplain org.hibernate.type.SqlTypes JDBC type}s. Here we allow some latitude
 * so that different numeric types are comparable, and different string types are
 * comparable. However, we do not allow comparisons between types which involve more
 * questionable/unportable implicit type conversions (between integers and strings,
 * for example). This means that we accept comparisons between basic types which map
 * completely unrelated types in Java.
 * <p>
 * Entity types have identity equality. That is, two entities are considered equal if
 * their primary keys are equal.
 * <p>
 * Embeddable and tuple types have value equality. That is, they're considered equal
 * if their members are equal. For convenience, an embeddable object may be compared
 * directly to a tuple constructor.
 * <p>
 * Comparison of discriminators (that is, of literal entity types and {@code type()}
 * function application) is legal only when the entity types belong to the same mapped
 * entity hierarchy.
 *
 * @see #assertComparable(Expression, Expression, SessionFactoryImplementor)
 * @see #assertAssignable(String, SqmPath, SqmTypedNode, SessionFactoryImplementor)
 *
 * @author Gavin King
 */
public class TypecheckUtil {

	/**
	 * @implNote The code below will reject some things that are accepted in H5,
	 * and perhaps even a few things which were accepted in H6.1/6.2. Therefore,
	 * users will report "bugs". The correct resolution of such bugs is not to
	 * go naively inserting special cases and secret escape hatches in this code!
	 * Instead, it's important to practice saying "no" and rejecting the majority
	 * of such "bug" reports. In cases where a fix really is required, it should
	 * be done within the framework laid out below, not by adding ad-hoc special
	 * rules and holes which undermine the type system. It's much more important
	 * that HQL has simple, predictable, and understandable rules than it is that
	 * every single user be able to do every single little weird thing that used
	 * to work in Hibernate 5.
	 *
	 * @param lhsType the type of the expression on the LHS of the comparison operator
	 * @param rhsType the type of the expression on the RHS of the comparison operator
	 *
	 * @see #isTypeAssignable(SqmPathSource, SqmExpressible, SessionFactoryImplementor)
	 */
	public static boolean areTypesComparable(
			SqmExpressible<?> lhsType,
			SqmExpressible<?> rhsType,
			SessionFactoryImplementor factory) {
		if ( lhsType == null || rhsType == null || lhsType == rhsType ) {
			return true;
		}

		// for query with parameters we are unable to resolve the correct JavaType, especially for tuple of parameters

		if ( lhsType instanceof QueryParameterJavaObjectType || rhsType instanceof QueryParameterJavaObjectType) {
			return true;
		}

		// since we can't so anything meaningful here, just allow
		// any comparison with multivalued parameters

		if ( lhsType instanceof SqmCriteriaNodeBuilder.MultiValueParameterType<?>
				|| rhsType instanceof SqmCriteriaNodeBuilder.MultiValueParameterType<?>) {
			// TODO: do something meaningful here
			return true;
		}

		final DomainType<?> lhsDomainType = lhsType.getSqmType();
		final DomainType<?> rhsDomainType = rhsType.getSqmType();

		// for embeddables, the embeddable class must match exactly

		final EmbeddableDomainType<?> lhsEmbeddable = getEmbeddableType( lhsDomainType );
		final EmbeddableDomainType<?> rhsEmbeddable = getEmbeddableType( rhsDomainType );
		if ( lhsEmbeddable != null && rhsEmbeddable != null ) {
			return areEmbeddableTypesComparable( lhsEmbeddable, rhsEmbeddable );
		}

		// for tuple constructors, we must check each element

		if ( lhsDomainType instanceof TupleType && rhsDomainType instanceof TupleType ) {
			return areTupleTypesComparable( factory, (TupleType<?>) lhsDomainType, (TupleType<?>) rhsDomainType );
		}

		// allow comparing an embeddable against a tuple literal

		if ( lhsEmbeddable != null && rhsDomainType instanceof TupleType
				|| rhsEmbeddable != null && lhsDomainType instanceof TupleType ) {
			// TODO: do something meaningful here
			return true;
		}

		// entities can be compared if they belong to the same inheritance hierarchy

		if ( lhsDomainType instanceof EntityType && rhsDomainType instanceof EntityType ) {
			return areEntityTypesComparable( (EntityType<?>) lhsDomainType, (EntityType<?>) rhsDomainType, factory );
		}

		// entities can be compared to discriminators if they belong to
		// the same inheritance hierarchy

		if ( lhsDomainType instanceof EntityDiscriminatorSqmPathSource ) {
			return isDiscriminatorTypeComparable( (EntityDiscriminatorSqmPathSource<?>) lhsDomainType, rhsDomainType, factory );
		}
		if ( rhsDomainType instanceof EntityDiscriminatorSqmPathSource ) {
			return isDiscriminatorTypeComparable( (EntityDiscriminatorSqmPathSource<?>) rhsDomainType, lhsDomainType, factory );
		}

		// Treat the expressions as comparable if they belong to the same
		// "family" of JDBC type. One could object to this approach since
		// JDBC types can vary between databases, however it's the only
		// decent approach which allows comparison between literals and
		// enums, user-defined types, etc.

		if ( lhsDomainType instanceof JdbcMapping && rhsDomainType instanceof JdbcMapping ) {
			if ( areJdbcMappingsComparable(  (JdbcMapping) lhsDomainType, (JdbcMapping) rhsDomainType, factory ) ) {
				return true;
			}
		}

		// Workaround: these are needed for a handful of slightly "weird" cases
		// involving Java field literals and converters, where we don't have
		// access to the correct JDBC type above. However, this is exactly the
		// sort of hole warned about above, and accepts many things which are
		// not well-typed.

		// TODO: sort all this out, and remove this branch
		if ( isSameJavaType( lhsType, rhsType ) ) {
			return true;
		}

		return false;
	}

	private static boolean areJdbcMappingsComparable(
			JdbcMapping lhsJdbcMapping,
			JdbcMapping rhsJdbcMapping,
			SessionFactoryImplementor factory) {
		if ( areJdbcTypesComparable( lhsJdbcMapping.getJdbcType(), rhsJdbcMapping.getJdbcType() ) ) {
			return true;
		}
		// converters are implicitly applied to the other side when its domain type is compatible
		else if ( lhsJdbcMapping.getValueConverter() != null || rhsJdbcMapping.getValueConverter() != null ) {
			final JdbcMapping lhsDomainMapping = getDomainJdbcType( lhsJdbcMapping, factory );
			final JdbcMapping rhsDomainMapping = getDomainJdbcType( rhsJdbcMapping, factory );
			return lhsDomainMapping != null && rhsDomainMapping != null && areJdbcTypesComparable(
					lhsDomainMapping.getJdbcType(),
					rhsDomainMapping.getJdbcType()
			);
		}
		return false;
	}

	private static boolean areJdbcTypesComparable(JdbcType lhsJdbcType, JdbcType rhsJdbcType) {
		return lhsJdbcType.getJdbcTypeCode() == rhsJdbcType.getJdbcTypeCode()
				// "families" of implicitly-convertible JDBC types
				// (this list might need to be extended in future)
				|| lhsJdbcType.isStringLike() && rhsJdbcType.isStringLike()
				|| lhsJdbcType.isTemporal() && rhsJdbcType.isTemporal()
				|| lhsJdbcType.isNumber() && rhsJdbcType.isNumber();
	}

	private static JdbcMapping getDomainJdbcType(JdbcMapping jdbcMapping, SessionFactoryImplementor factory) {
		if ( jdbcMapping.getValueConverter() != null ) {
			final BasicType<?> basicType = factory.getTypeConfiguration().getBasicTypeForJavaType(
					jdbcMapping.getValueConverter().getDomainJavaType().getJavaType()
			);
			if ( basicType != null ) {
				return basicType.getJdbcMapping();
			}
		}
		return jdbcMapping;
	}

	private static EmbeddableDomainType<?> getEmbeddableType(SqmExpressible<?> expressible) {
		return expressible instanceof EmbeddableDomainType<?> ? (EmbeddableDomainType<?>) expressible : null;
	}

	private static boolean areEmbeddableTypesComparable(
			EmbeddableDomainType<?> lhsType,
			EmbeddableDomainType<?> rhsType) {
		if ( rhsType.getJavaType() == lhsType.getJavaType() ) {
			return true;
		}

		return lhsType.isPolymorphic() && getRootEmbeddableType( lhsType ) == getRootEmbeddableType( rhsType );
	}

	private static ManagedDomainType<?> getRootEmbeddableType(EmbeddableDomainType<?> embeddableType) {
		ManagedDomainType<?> rootType = embeddableType;
		while ( rootType.getSuperType() != null ) {
			rootType = rootType.getSuperType();
		}
		return rootType;
	}

	private static boolean areTupleTypesComparable(
			SessionFactoryImplementor factory,
			TupleType<?> lhsTuple,
			TupleType<?> rhsTuple) {
		if ( rhsTuple.componentCount() != lhsTuple.componentCount() ) {
			return false;
		}
		else {
			for ( int i = 0; i < lhsTuple.componentCount(); i++ ) {
				if ( !areTypesComparable( lhsTuple.get(i), rhsTuple.get(i), factory ) ) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean areEntityTypesComparable(
			EntityType<?> lhsType, EntityType<?> rhsType,
			SessionFactoryImplementor factory) {
		EntityPersister lhsEntity = getEntityDescriptor( factory, lhsType.getName() );
		EntityPersister rhsEntity = getEntityDescriptor( factory, rhsType.getName() );
		return lhsEntity.getRootEntityName().equals( rhsEntity.getRootEntityName() );
	}

	private static boolean isDiscriminatorTypeComparable(
			EntityDiscriminatorSqmPathSource<?> lhsDiscriminator, SqmExpressible<?> rhsType,
			SessionFactoryImplementor factory) {
		String entityName = lhsDiscriminator.getEntityDomainType().getHibernateEntityName();
		EntityPersister lhsEntity = factory.getMappingMetamodel().getEntityDescriptor( entityName );
		if ( rhsType instanceof EntityType ) {
			String rhsEntityName = ((EntityType<?>) rhsType).getName();
			EntityPersister rhsEntity = getEntityDescriptor( factory, rhsEntityName );
			return lhsEntity.getRootEntityName().equals( rhsEntity.getRootEntityName() );
		}
		else if ( rhsType instanceof EntityDiscriminatorSqmPathSource ) {
			EntityDiscriminatorSqmPathSource<?> discriminator = (EntityDiscriminatorSqmPathSource<?>) rhsType;
			String rhsEntityName = discriminator.getEntityDomainType().getHibernateEntityName();
			EntityPersister rhsEntity = factory.getMappingMetamodel().getEntityDescriptor( rhsEntityName );
			return rhsEntity.getRootEntityName().equals( lhsEntity.getRootEntityName() );
		}
		else  {
			BasicType<?> discriminatorType = (BasicType<?>)
					lhsDiscriminator.getEntityMapping().getDiscriminatorMapping().getMappedType();
			return areTypesComparable( discriminatorType, rhsType, factory );
		}
	}

	/**
	 * @param targetType the type of the path expression to which a value is assigned
	 * @param expressionType the type of the value expression being assigned to the path
	 *
	 * @see #areTypesComparable(SqmExpressible, SqmExpressible, SessionFactoryImplementor)
	 */
	private static boolean isTypeAssignable(
			SqmPathSource<?> targetType, SqmExpressible<?> expressionType,
			SessionFactoryImplementor factory) {

		if ( targetType == null || expressionType == null || targetType == expressionType ) {
			return true;
		}

		// entities can be assigned if they belong to the same inheritance hierarchy

		if ( targetType instanceof EntityType && expressionType instanceof EntityType ) {
			return isEntityTypeAssignable( (EntityType<?>) targetType, (EntityType<?>) expressionType, factory );
		}

		// Treat the expression as assignable to the target path if they belong
		// to the same "family" of JDBC type. One could object to this approach
		// since JDBC types can vary between databases, however it's the only
		// decent approach which allows comparison between literals and enums,
		// user-defined types, etc.

		DomainType<?> lhsDomainType = targetType.getSqmType();
		DomainType<?> rhsDomainType = expressionType.getSqmType();
		if ( lhsDomainType instanceof JdbcMapping && rhsDomainType instanceof JdbcMapping ) {
			JdbcType lhsJdbcType = ((JdbcMapping) lhsDomainType).getJdbcType();
			JdbcType rhsJdbcType = ((JdbcMapping) rhsDomainType).getJdbcType();
			if ( lhsJdbcType.getJdbcTypeCode() == rhsJdbcType.getJdbcTypeCode()
					// "families" of implicitly-convertible JDBC types
					// (this list might need to be extended in future)
					|| lhsJdbcType.isStringLike() && rhsJdbcType.isStringLike()
					|| lhsJdbcType.isInteger() && rhsJdbcType.isInteger()
					|| lhsJdbcType.isFloat() && rhsJdbcType.isNumber()
					|| lhsJdbcType.isDecimal() && rhsJdbcType.isNumber() ) {
				return true;
			}
		}

		// Workaround: these are needed for a handful of slightly "weird" cases
		// involving Java field literals and converters, where we don't have
		// access to the correct JDBC type above. However, this is exactly the
		// sort of hole warned about above, and accepts many things which are
		// not well-typed.

		// TODO: sort all this out, and remove this branch
		if ( isSameJavaType( targetType, expressionType ) ) {
			return true;
		}

		return false;
	}

	private static boolean isSameJavaType(SqmExpressible<?> leftType, SqmExpressible<?> rightType) {
		return isUnknown( leftType.getExpressibleJavaType() ) || isUnknown( rightType.getExpressibleJavaType() )
			|| leftType.getRelationalJavaType() == rightType.getRelationalJavaType()
			|| leftType.getExpressibleJavaType() == rightType.getExpressibleJavaType()
			|| leftType.getBindableJavaType() == rightType.getBindableJavaType();
	}

	private static boolean isEntityTypeAssignable(
			EntityType<?> lhsType, EntityType<?> rhsType,
			SessionFactoryImplementor factory) {
		EntityPersister lhsEntity = getEntityDescriptor( factory, lhsType.getName() );
		EntityPersister rhsEntity = getEntityDescriptor( factory, rhsType.getName() );
		return lhsEntity.isSubclassEntityName( rhsEntity.getEntityName() );
	}

	private static EntityPersister getEntityDescriptor(SessionFactoryImplementor factory, String name) {
		return factory.getMappingMetamodel()
				.getEntityDescriptor( factory.getJpaMetamodel().qualifyImportableName( name ) );
	}

	/**
	 * @see TypecheckUtil#assertAssignable(String, SqmPath, SqmTypedNode, SessionFactoryImplementor)
	 */
	public static void assertComparable(Expression<?> x, Expression<?> y, SessionFactoryImplementor factory) {
		final SqmExpression<?> left = (SqmExpression<?>) x;
		final SqmExpression<?> right = (SqmExpression<?>) y;
		final Integer leftTupleLength = left.getTupleLength();
		final Integer rightTupleLength;
		if (  leftTupleLength != null && ( rightTupleLength = right.getTupleLength() ) != null
				&& leftTupleLength.intValue() != rightTupleLength.intValue() ) {
			throw new SemanticException( "Cannot compare tuples of different lengths" );
		}

		// SqmMemerOfPredicate is the only one allowing multi-valued paths, its comparability is now evaluated in areTypesComparable
		// i.e. without calling this method, so we can check this here for other Predicates that do call this
		if ( left instanceof SqmPluralValuedSimplePath || right instanceof SqmPluralValuedSimplePath ) {
			throw new SemanticException( "Multi valued paths are only allowed for the member of operator" );
		}

		// allow comparing literal null to things
		if ( !( left instanceof SqmLiteralNull ) && !( right instanceof SqmLiteralNull ) ) {
			final SqmExpressible<?> leftType = left.getExpressible();
			final SqmExpressible<?> rightType = right.getExpressible();
			if ( !areTypesComparable( leftType, rightType, factory ) ) {
				throw new SemanticException(
						String.format(
								"Cannot compare left expression of type '%s' with right expression of type '%s'",
								leftType.getTypeName(),
								rightType.getTypeName()
						)
				);
			}
		}
	}

	/**
	 * @see TypecheckUtil#assertComparable(Expression, Expression, SessionFactoryImplementor)
	 */
	public static void assertAssignable(
			String hqlString,
			SqmPath<?> targetPath, SqmTypedNode<?> expression,
			SessionFactoryImplementor factory) {
		// allow assigning literal null to things
		if ( expression instanceof SqmLiteralNull ) {
			// TODO: check that the target path is nullable
		}
		else {
			SqmPathSource<?> targetType = targetPath.getNodeType();
			SqmExpressible<?> expressionType = expression.getNodeType();
			if ( !isTypeAssignable( targetType, expressionType, factory ) ) {
				throw new SemanticException(
						String.format(
								"Cannot assign expression of type '%s' to target path '%s' of type '%s'",
								expressionType.getTypeName(),
								targetPath.toHqlString(),
								targetType.getTypeName()
						),
						hqlString,
						null
				);
			}
		}
	}

	public static void assertOperable(SqmExpression<?> left, SqmExpression<?> right, BinaryArithmeticOperator op) {
		final SqmExpressible<?> leftNodeType = left.getExpressible();
		final SqmExpressible<?> rightNodeType = right.getExpressible();
		if ( leftNodeType != null && rightNodeType != null ) {
			final Class<?> leftJavaType = leftNodeType.getRelationalJavaType().getJavaTypeClass();
			final Class<?> rightJavaType = rightNodeType.getRelationalJavaType().getJavaTypeClass();
			if ( Number.class.isAssignableFrom( leftJavaType ) ) {
				// left operand is a number
				switch (op) {
					case MULTIPLY:
						if ( !Number.class.isAssignableFrom( rightJavaType )
								// we can scale a duration by a number
								&& !TemporalAmount.class.isAssignableFrom( rightJavaType ) ) {
							throw new SemanticException(
									"Operand of " + op.getOperatorSqlText() + " is of type '" + rightNodeType.getTypeName() +
											"' which is not a numeric type (it is not an instance of 'java.lang.Number' or 'java.time.TemporalAmount')"
							);
						}
						break;
					default:
						if ( !Number.class.isAssignableFrom( rightJavaType ) ) {
							throw new SemanticException(
									"Operand of " + op.getOperatorSqlText() + " is of type '" + rightNodeType.getTypeName() +
											"' which is not a numeric type (it is not an instance of 'java.lang.Number')"
							);
						}
						break;
				}
			}
			else if ( TemporalAmount.class.isAssignableFrom( leftJavaType ) ) {
				// left operand is a duration
				switch (op) {
					case ADD:
					case SUBTRACT:
						// we can add/subtract durations
						if ( !TemporalAmount.class.isAssignableFrom( rightJavaType ) ) {
							throw new SemanticException(
									"Operand of " + op.getOperatorSqlText() + " is of type '" + rightNodeType.getTypeName() +
											"' which is not a temporal amount (it is not an instance of 'java.time.TemporalAmount')"
							);
						}
						break;
					default:
						throw new SemanticException(
								"Operand of " + op.getOperatorSqlText() + " is of type '" + leftNodeType.getTypeName() +
										"' which is not a numeric type (it is not an instance of 'java.lang.Number')"
						);
				}
			}
			else if ( Temporal.class.isAssignableFrom( leftJavaType )
					|| java.util.Date.class.isAssignableFrom( leftJavaType ) ) {
				// left operand is a date, time, or datetime
				switch (op) {
					case ADD:
						// we can add a duration to date, time, or datetime
						if ( !TemporalAmount.class.isAssignableFrom( rightJavaType ) ) {
							throw new SemanticException(
									"Operand of " + op.getOperatorSqlText() + " is of type '" + rightNodeType.getTypeName() +
											"' which is not a temporal amount (it is not an instance of 'java.time.TemporalAmount')"
							);
						}
						break;
					case SUBTRACT:
						// we can subtract dates, times, or datetimes
						if ( !Temporal.class.isAssignableFrom( rightJavaType )
								&& !java.util.Date.class.isAssignableFrom( rightJavaType )
								// we can subtract a duration from a date, time, or datetime
								&& !TemporalAmount.class.isAssignableFrom( rightJavaType ) ) {
							throw new SemanticException(
									"Operand of " + op.getOperatorSqlText() + " is of type '" + rightNodeType.getTypeName() +
											"' which is not a temporal amount (it is not an instance of 'java.time.TemporalAmount')"
							);
						}
						break;
					default:
						throw new SemanticException(
								"Operand of " + op.getOperatorSqlText() + " is of type '" + leftNodeType.getTypeName() +
										"' which is not a numeric type (it is not an instance of 'java.lang.Number')"
						);
				}
			}
			else if ( isNumberArray( leftNodeType ) ) {
				// left operand is a number
				if ( !isNumberArray( rightNodeType ) ) {
					throw new SemanticException(
							"Operand of " + op.getOperatorSqlText() + " is of type '" + rightNodeType.getTypeName() +
									"' which is not a numeric array type" + " (it is not an instance of 'java.lang.Number[]')"
					);
				}
			}
			else {
				throw new SemanticException(
						"Operand of " + op.getOperatorSqlText()
								+ " is of type '" + leftNodeType.getTypeName() + "' which is not a numeric type"
								+ " (it is not an instance of 'java.lang.Number', 'java.time.Temporal', or 'java.time.TemporalAmount')"
				);
			}
		}
	}

	public static boolean isNumberArray(SqmExpressible<?> expressible) {
		final DomainType<?> domainType;
		if ( expressible != null && ( domainType = expressible.getSqmType() ) != null ) {
			return domainType instanceof BasicPluralType<?, ?> && Number.class.isAssignableFrom(
					( (BasicPluralType<?, ?>) domainType ).getElementType().getJavaType()
			);
		}
		return false;
	}

	public static void assertString(SqmExpression<?> expression) {
		final SqmExpressible<?> nodeType = expression.getNodeType();
		if ( nodeType != null ) {
			final DomainType<?> domainType = nodeType.getSqmType();
			if ( !( domainType instanceof JdbcMapping ) || !( (JdbcMapping) domainType ).getJdbcType().isStringLike() ) {
				throw new SemanticException(
						"Operand of 'like' is of type '" + nodeType.getTypeName() +
								"' which is not a string (its JDBC type code is not string-like)"
				);
			}
		}
	}

	public static void assertDuration(SqmExpression<?> expression) {
		final SqmExpressible<?> nodeType = expression.getNodeType();
		if ( nodeType != null ) {
			final DomainType<?> domainType = nodeType.getSqmType();
			if ( !( domainType instanceof JdbcMapping ) || !( (JdbcMapping) domainType ).getJdbcType().isDuration() ) {
				throw new SemanticException(
						"Operand of 'by' is of type '" + nodeType.getTypeName() +
								"' which is not a duration (its JDBC type code is not duration-like)"
				);
			}
		}
	}

	public static void assertNumeric(SqmExpression<?> expression, UnaryArithmeticOperator op) {
		final SqmExpressible<?> nodeType = expression.getExpressible();
		if ( nodeType != null ) {
			final DomainType<?> domainType = nodeType.getSqmType();
			if ( !( domainType instanceof JdbcMapping ) || !( (JdbcMapping) domainType ).getJdbcType().isNumber() ) {
				throw new SemanticException(
						"Operand of " + op.getOperatorChar() + " is of type '" + nodeType.getTypeName() +
								"' which is not a numeric type (its JDBC type code is not numeric)"
				);
			}
		}
	}
}
