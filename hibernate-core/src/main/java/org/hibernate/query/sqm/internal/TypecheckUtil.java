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
import org.hibernate.metamodel.model.domain.TupleType;
import org.hibernate.metamodel.model.domain.internal.DiscriminatorSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

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
	 * every single user be able to do every single little wierd thing that used
	 * to work in Hibernate 5.
	 *
	 * @param lhsType the type of the expression on the LHS of the comparison operator
	 * @param rhsType the type of the expression on the RHS of the comparison operator
	 *
	 * @see #isTypeAssignable(SqmPathSource, SqmExpressible, SessionFactoryImplementor)
	 */
	private static boolean areTypesComparable(
			SqmExpressible<?> lhsType, SqmExpressible<?> rhsType,
			SessionFactoryImplementor factory) {

		if ( lhsType == null || rhsType == null || lhsType == rhsType ) {
			return true;
		}

		// since we can't so anything meaningful here, just allow
		// any comparison with multivalued parameters

		if ( lhsType instanceof SqmCriteriaNodeBuilder.MultiValueParameterType<?>
				|| rhsType instanceof SqmCriteriaNodeBuilder.MultiValueParameterType<?>) {
			// TODO: do something meaningful here
			return true;
		}

		// for embeddables, the embeddable class must match exactly

		if ( lhsType instanceof EmbeddedSqmPathSource && rhsType instanceof EmbeddedSqmPathSource ) {
			return areEmbeddableTypesComparable( (EmbeddedSqmPathSource<?>) lhsType, (EmbeddedSqmPathSource<?>) rhsType );
		}

		// for tuple constructors, we must check each element

		if ( lhsType instanceof TupleType && rhsType instanceof TupleType ) {
			return areTupleTypesComparable( factory, (TupleType<?>) lhsType, (TupleType<?>) rhsType );
		}

		// allow comparing an embeddable against a tuple literal

		if ( lhsType instanceof EmbeddedSqmPathSource<?> && rhsType instanceof TupleType
				|| rhsType instanceof EmbeddedSqmPathSource<?> && lhsType instanceof TupleType ) {
			// TODO: do something meaningful here
			return true;
		}

		// entities can be compared if they belong to the same inheritance hierarchy

		if ( lhsType instanceof EntityType && rhsType instanceof EntityType ) {
			return areEntityTypesComparable( (EntityType<?>) lhsType, (EntityType<?>) rhsType, factory );
		}

		// entities can be compared to discriminators if they belong to
		// the same inheritance hierarchy

		if ( lhsType instanceof DiscriminatorSqmPathSource) {
			return isDiscriminatorTypeComparable( (DiscriminatorSqmPathSource<?>) lhsType, rhsType, factory );
		}
		if ( rhsType instanceof DiscriminatorSqmPathSource ) {
			return isDiscriminatorTypeComparable( (DiscriminatorSqmPathSource<?>) rhsType, lhsType, factory );
		}

		// Treat the expressions as comparable if they belong to the same
		// "family" of JDBC type. One could object to this approach since
		// JDBC types can vary between databases, however it's the only
		// decent approach which allows comparison between literals and
		// enums, user-defined types, etc.

		DomainType<?> lhsDomainType = lhsType.getSqmType();
		DomainType<?> rhsDomainType = rhsType.getSqmType();
		if ( lhsDomainType instanceof JdbcMapping && rhsDomainType instanceof JdbcMapping ) {
			JdbcType lhsJdbcType = ((JdbcMapping) lhsDomainType).getJdbcType();
			JdbcType rhsJdbcType = ((JdbcMapping) rhsDomainType).getJdbcType();
			if ( lhsJdbcType.getJdbcTypeCode() == rhsJdbcType.getJdbcTypeCode()
					// "families" of implicitly-convertible JDBC types
					// (this list might need to be extended in future)
					|| lhsJdbcType.isStringLike() && rhsJdbcType.isStringLike()
					|| lhsJdbcType.isTemporal() && rhsJdbcType.isTemporal()
					|| lhsJdbcType.isNumber() && rhsJdbcType.isNumber() ) {
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

	private static boolean areEmbeddableTypesComparable(EmbeddedSqmPathSource<?> lhsType, EmbeddedSqmPathSource<?> rhsType) {
		// no polymorphism for embeddable types
		return rhsType.getNodeJavaType() == lhsType.getNodeJavaType();
	}

	private static boolean areTupleTypesComparable(SessionFactoryImplementor factory, TupleType<?> lhsTuple, TupleType<?> rhsTuple) {
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
			DiscriminatorSqmPathSource<?> lhsDiscriminator, SqmExpressible<?> rhsType,
			SessionFactoryImplementor factory) {
		String entityName = lhsDiscriminator.getEntityDomainType().getHibernateEntityName();
		EntityPersister lhsEntity = factory.getMappingMetamodel().getEntityDescriptor( entityName );
		if ( rhsType instanceof EntityType ) {
			String rhsEntityName = ((EntityType<?>) rhsType).getName();
			EntityPersister rhsEntity = getEntityDescriptor( factory, rhsEntityName );
			return lhsEntity.getRootEntityName().equals( rhsEntity.getRootEntityName() );
		}
		else if ( rhsType instanceof DiscriminatorSqmPathSource ) {
			DiscriminatorSqmPathSource<?> discriminator = (DiscriminatorSqmPathSource<?>) rhsType;
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
					|| lhsJdbcType.isFloat() && rhsJdbcType.isFloat()
					|| lhsJdbcType.isFloat() && rhsJdbcType.isInteger() ) {
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
		return leftType.getRelationalJavaType() == rightType.getRelationalJavaType()
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
		SqmExpression<?> left = (SqmExpression<?>) x;
		SqmExpression<?> right = (SqmExpression<?>) y;
		if (  left.getTupleLength() != null && right.getTupleLength() != null
				&& left.getTupleLength().intValue() != right.getTupleLength().intValue() ) {
			throw new SemanticException( "Cannot compare tuples of different lengths" );
		}
		// allow comparing literal null to things
		if ( !(left instanceof SqmLiteralNull) && !(right instanceof SqmLiteralNull) ) {
			final SqmExpressible<?> leftType = left.getNodeType();
			final SqmExpressible<?> rightType = right.getNodeType();
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
}
