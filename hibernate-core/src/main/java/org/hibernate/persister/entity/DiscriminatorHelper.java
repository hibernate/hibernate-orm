/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.sql.InFragment;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import static org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;

/**
 * Operations needed by persisters for working with discriminators.
 *
 * @author Gavin King
 */
@Internal
public class DiscriminatorHelper {

	public static final Object NULL_DISCRIMINATOR = new MarkerObject( "<null discriminator>" );
	public static final Object NOT_NULL_DISCRIMINATOR = new MarkerObject( "<not null discriminator>" );

	/**
	 * The underlying BasicType as the "JDBC mapping" between the relational {@link org.hibernate.type.descriptor.java.JavaType}
	 * and the {@link org.hibernate.type.descriptor.jdbc.JdbcType}.
	 */
	static BasicType<?> getDiscriminatorType(PersistentClass persistentClass) {
		final Type discriminatorType = persistentClass.getDiscriminator().getType();
		if ( discriminatorType instanceof BasicType<?> basicType ) {
			return basicType;
		}
		else {
			throw new MappingException( "Illegal discriminator type: " + discriminatorType.getName() );
		}
	}

	public static BasicType<?> getDiscriminatorType(Component component) {
		final Type discriminatorType = component.getDiscriminator().getType();
		if ( discriminatorType instanceof BasicType<?> basicType ) {
			return basicType;
		}
		else {
			throw new MappingException( "Illegal discriminator type: " + discriminatorType.getName() );
		}
	}

	public static String getDiscriminatorSQLValue(PersistentClass persistentClass, Dialect dialect) {
		if ( persistentClass.isDiscriminatorValueNull() ) {
			return InFragment.NULL;
		}
		else if ( persistentClass.isDiscriminatorValueNotNull() ) {
			return InFragment.NOT_NULL;
		}
		else {
			return discriminatorSqlLiteral( getDiscriminatorType( persistentClass ), persistentClass, dialect );
		}
	}

	private static Object parseDiscriminatorValue(PersistentClass persistentClass) {
		final BasicType<?> discriminatorType = getDiscriminatorType( persistentClass );
		final String discriminatorValue = persistentClass.getDiscriminatorValue();
		try {
			return discriminatorType.getJavaTypeDescriptor().fromString( discriminatorValue );
		}
		catch ( Exception e ) {
			throw new MappingException( "Could not parse discriminator value '" + discriminatorValue
							+ "' as discriminator type '" + discriminatorType.getName() + "'", e );
		}
	}

	public static Object getDiscriminatorValue(PersistentClass persistentClass) {
		if ( persistentClass.isDiscriminatorValueNull() ) {
			return NULL_DISCRIMINATOR;
		}
		else if ( persistentClass.isDiscriminatorValueNotNull() ) {
			return NOT_NULL_DISCRIMINATOR;
		}
		else {
			return parseDiscriminatorValue( persistentClass );
		}
	}

	private static <T> String discriminatorSqlLiteral(
			BasicType<T> discriminatorType,
			PersistentClass persistentClass,
			Dialect dialect) {
		return jdbcLiteral(
				discriminatorType.getJavaTypeDescriptor().fromString( persistentClass.getDiscriminatorValue() ),
				discriminatorType.getJdbcLiteralFormatter(),
				dialect
		);
	}

	public static <T> String jdbcLiteral(T value, JdbcLiteralFormatter<T> formatter, Dialect dialect) {
		try {
			return formatter.toJdbcLiteral( value, dialect, null );
		}
		catch (Exception e) {
			throw new MappingException( "Could not format discriminator value to SQL string", e );
		}
	}

	/**
	 * Utility that computes the node type used in entity or embeddable type literals. Resolves to
	 * either the {@link org.hibernate.metamodel.mapping.DiscriminatorType}, for polymorphic
	 * domain types, or to {@link StandardBasicTypes#CLASS Class} for non-inherited ones.
	 */
	public static <T> SqmExpressible<? super T> getDiscriminatorType(
			SqmPathSource<T> domainType, NodeBuilder nodeBuilder) {
		final SqmPathSource<?> subPathSource = domainType.findSubPathSource( DISCRIMINATOR_ROLE_NAME );
		final SqmExpressible<?> type = subPathSource != null
				? subPathSource.getSqmPathType()
				: nodeBuilder.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.CLASS );
		//noinspection unchecked
		return (SqmExpressible<? super T>) type;
	}
}
