/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.sqm.domain.SqmDomainMetamodel;
import org.hibernate.sqm.domain.SqmExpressableTypeBasic;

/**
 * Helper for making the determination of an expression's "type" as sovered by the rules
 * defined in section 6.5.7.1 (Result Types of Expressions) of the JPA 2.1 spec
 *
 * @author Steve Ebersole
 */
public class ExpressionTypeHelper {
	private ExpressionTypeHelper() {
	}

	/**
	 * Determine the result type of an arithmetic operation as defined by the
	 * rules in section 6.5.7.1.
	 *
	 * @return The operation result type
	 */
	public static SqmExpressableTypeBasic resolveArithmeticType(
			SqmExpressableTypeBasic firstType,
			SqmExpressableTypeBasic secondType,
			boolean isDivision,
			SqmDomainMetamodel domainMetamodel) {
		if ( isDivision ) {
			// covered under the note in 6.5.7.1 discussing the unportable
			// "semantics of the SQL division operation"..
			return domainMetamodel.resolveBasicType( Number.class );
		}
		else if ( matchesJavaType( firstType, Double.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Double.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Float.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Float.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, BigDecimal.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, BigDecimal.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, BigInteger.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, BigInteger.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Long.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Long.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Integer.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Integer.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Short.class ) ) {
			return domainMetamodel.resolveBasicType( Integer.class );
		}
		else if ( matchesJavaType( secondType, Short.class ) ) {
			return domainMetamodel.resolveBasicType( Integer.class );
		}
		else {
			return domainMetamodel.resolveBasicType( Number.class );
		}
	}

	@SuppressWarnings("unchecked")
	private static boolean matchesJavaType(SqmExpressableTypeBasic type, Class javaType) {
		return type != null && javaType.isAssignableFrom( type.getExportedDomainType().getJavaType() );
	}

	public static SqmExpressableTypeBasic resolveSingleNumericType(
			SqmExpressableTypeBasic typeDescriptor,
			SqmDomainMetamodel domainMetamodel) {
		if ( matchesJavaType( typeDescriptor, Double.class ) ) {
			return typeDescriptor;
		}
		else if ( matchesJavaType( typeDescriptor, Float.class ) ) {
			return typeDescriptor;
		}
		else if ( matchesJavaType( typeDescriptor, BigDecimal.class ) ) {
			return typeDescriptor;
		}
		else if ( matchesJavaType( typeDescriptor, BigInteger.class ) ) {
			return typeDescriptor;
		}
		else if ( matchesJavaType( typeDescriptor, Long.class ) ) {
			return typeDescriptor;
		}
		else if ( matchesJavaType( typeDescriptor, Integer.class ) ) {
			return typeDescriptor;
		}
		else if ( matchesJavaType( typeDescriptor, Short.class ) ) {
			return domainMetamodel.resolveBasicType( Integer.class );
		}
		else {
			return domainMetamodel.resolveBasicType( Number.class );
		}
	}
}
