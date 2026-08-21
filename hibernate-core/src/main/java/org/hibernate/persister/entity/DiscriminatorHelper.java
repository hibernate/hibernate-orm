/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
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

	private static DiscriminatorValue parseDiscriminatorValue(PersistentClass persistentClass) {
		final BasicType<?> discriminatorType = getDiscriminatorType( persistentClass );
		final String discriminatorValue = persistentClass.getDiscriminatorValue();
		try {
			return new DiscriminatorValue.Literal(
					discriminatorType.getJavaTypeDescriptor().fromString( discriminatorValue )
			);
		}
		catch ( Exception e ) {
			throw new MappingException( "Could not parse discriminator value '" + discriminatorValue
							+ "' as discriminator type '" + discriminatorType.getName() + "'", e );
		}
	}

	public static DiscriminatorValue getDiscriminatorValue(PersistentClass persistentClass) {
		if ( persistentClass.isDiscriminatorValueNull() ) {
			return DiscriminatorValue.Special.NULL;
		}
		else if ( persistentClass.isDiscriminatorValueNotNull() ) {
			return DiscriminatorValue.Special.NOT_NULL;
		}
		else {
			return parseDiscriminatorValue( persistentClass );
		}
	}

	public static Object toRelationalValue(DiscriminatorValue discriminatorValue) {
		if ( discriminatorValue instanceof DiscriminatorValue.Literal literal ) {
			return literal.value();
		}
		else if ( discriminatorValue == DiscriminatorValue.Special.NULL ) {
			return null;
		}
		else {
			throw new IllegalStateException( "Cannot convert NOT_NULL discriminator marker to a relational literal" );
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
	public static <T> SqmBindableType<? super T> getDiscriminatorType(
			SqmPathSource<T> domainType, NodeBuilder nodeBuilder) {
		final SqmPathSource<?> subPathSource = domainType.findSubPathSource( DISCRIMINATOR_ROLE_NAME );
		final SqmBindableType<?> type = subPathSource != null
				? subPathSource.getPathType()
				: nodeBuilder.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.CLASS );
		//noinspection unchecked
		return (SqmBindableType<? super T>) type;
	}
}
