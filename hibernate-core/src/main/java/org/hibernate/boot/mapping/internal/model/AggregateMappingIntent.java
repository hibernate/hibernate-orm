/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;

/**
 * Internal aggregate-storage intent resolved by the new boot pipeline.
 * <p>
 * This intentionally remains a source/model object that is projected into the
 * legacy {@code Component}/{@code AggregateColumn} bridge.  It is not a runtime
 * aggregate storage descriptor.
 */
public record AggregateMappingIntent(
		ComponentSource source,
		AggregateKind aggregateKind,
		Integer jdbcTypeCode,
		Struct struct,
		boolean plural) {
	public enum AggregateKind {
		STRUCT,
		JSON,
		XML
	}

	public static AggregateMappingIntent from(ComponentSource source) {
		if ( !supportsAggregateBridge( source ) || source.sourceMember() == null ) {
			return none( source );
		}

		final Struct struct = resolveStruct( source );
		final Integer jdbcTypeCode = resolveAggregateJdbcTypeCode( source );
		final AggregateKind aggregateKind = aggregateKind( struct, jdbcTypeCode );
		return aggregateKind == null
				? none( source )
				: new AggregateMappingIntent(
						source,
						aggregateKind,
						jdbcTypeCode,
						struct,
						isPluralAggregate( source )
				);
	}

	private static AggregateMappingIntent none(ComponentSource source) {
		return new AggregateMappingIntent( source, null, null, null, false );
	}

	public boolean isAggregate() {
		return aggregateKind != null;
	}

	public QualifiedName structName(BindingState state) {
		return struct == null ? null : toQualifiedName( struct, state );
	}

	public String[] structAttributeNames() {
		return struct == null ? null : struct.attributes();
	}

	public ColumnSource aggregateColumnSource() {
		if ( source.kind() == ComponentSource.Kind.MAP_KEY ) {
			return ColumnSource.from( source.sourceMember().getDirectAnnotationUsage( jakarta.persistence.MapKeyColumn.class ) );
		}
		return ColumnSource.from( source.sourceMember().getDirectAnnotationUsage( jakarta.persistence.Column.class ) );
	}

	public static boolean isAggregateArray(MemberDetails member, TypeDetails memberType) {
		if ( memberType == null ) {
			return false;
		}
		final Class<?> javaClass = toJavaClassIfAvailable( memberType.determineRawClass() );
		return isArrayMember( member, memberType, javaClass )
				&& isEmbeddableArrayElement( member, javaClass )
				&& ( member.hasDirectAnnotationUsage( Struct.class )
					|| hasAggregateJdbcTypeCode( member )
					|| member.getElementType() != null
							&& member.getElementType().determineRawClass().hasDirectAnnotationUsage( Struct.class ) );
	}

	private static Class<?> toJavaClassIfAvailable(ClassDetails classDetails) {
		return classDetails.getClassName() == null ? null : classDetails.toJavaClass();
	}

	private static boolean isArrayMember(MemberDetails member, TypeDetails memberType, Class<?> javaClass) {
		return member.isArray()
				|| memberType.getTypeKind() == TypeDetails.Kind.ARRAY
				|| javaClass != null && javaClass.isArray()
				|| member.isPlural() && member.getElementType() != null
						&& !member.hasDirectAnnotationUsage( ElementCollection.class );
	}

	private static boolean isEmbeddableArrayElement(MemberDetails member, Class<?> javaClass) {
		if ( member.getElementType() != null
				&& member.getElementType().determineRawClass().hasDirectAnnotationUsage( Embeddable.class ) ) {
			return true;
		}
		return javaClass != null
				&& javaClass.isArray()
				&& javaClass.getComponentType().isAnnotationPresent( Embeddable.class );
	}

	public static boolean hasAggregateJdbcTypeCode(MemberDetails member) {
		return resolveAggregateJdbcTypeCode( member ) != null;
	}

	public static boolean hasExplicitPluralBasicJdbcType(MemberDetails member) {
		final JdbcTypeCode jdbcTypeCode = member.getDirectAnnotationUsage( JdbcTypeCode.class );
		return jdbcTypeCode != null && switch ( jdbcTypeCode.value() ) {
			case SqlTypes.ARRAY,
					SqlTypes.JSON_ARRAY,
					SqlTypes.XML_ARRAY,
					SqlTypes.VARBINARY,
					SqlTypes.LONGVARBINARY -> true;
			default -> false;
		};
	}

	private static boolean supportsAggregateBridge(ComponentSource source) {
		return source.kind() == ComponentSource.Kind.EMBEDDED_ATTRIBUTE
			|| source.kind() == ComponentSource.Kind.COLLECTION_ELEMENT
			|| source.kind() == ComponentSource.Kind.MAP_KEY;
	}

	private static boolean isPluralAggregate(ComponentSource source) {
		if ( source.kind() == ComponentSource.Kind.COLLECTION_ELEMENT ) {
			return true;
		}
		if ( source.kind() == ComponentSource.Kind.MAP_KEY ) {
			return false;
		}
		return source.sourceMember().isArray()
				|| source.sourceMember().isPlural();
	}

	private static Struct resolveStruct(ComponentSource source) {
		final Struct memberStruct = source.sourceMember().getDirectAnnotationUsage( Struct.class );
		return memberStruct == null
				? source.componentType().getDirectAnnotationUsage( Struct.class )
				: memberStruct;
	}

	private static Integer resolveAggregateJdbcTypeCode(ComponentSource source) {
		if ( source.kind() == ComponentSource.Kind.MAP_KEY ) {
			final MapKeyJdbcTypeCode jdbcTypeCode = source.sourceMember()
					.getDirectAnnotationUsage( MapKeyJdbcTypeCode.class );
			return jdbcTypeCode == null ? null : aggregateJdbcTypeCode( jdbcTypeCode.value() );
		}
		return resolveAggregateJdbcTypeCode( source.sourceMember() );
	}

	private static Integer resolveAggregateJdbcTypeCode(MemberDetails member) {
		final JdbcTypeCode jdbcTypeCode = member.getDirectAnnotationUsage( JdbcTypeCode.class );
		return jdbcTypeCode == null ? null : aggregateJdbcTypeCode( jdbcTypeCode.value() );
	}

	private static Integer aggregateJdbcTypeCode(int jdbcTypeCode) {
		return switch ( jdbcTypeCode ) {
			case SqlTypes.STRUCT,
					SqlTypes.JSON,
					SqlTypes.SQLXML,
					SqlTypes.STRUCT_ARRAY,
					SqlTypes.STRUCT_TABLE,
					SqlTypes.JSON_ARRAY,
					SqlTypes.XML_ARRAY -> jdbcTypeCode;
			default -> null;
		};
	}

	private static AggregateKind aggregateKind(Struct struct, Integer jdbcTypeCode) {
		if ( struct != null ) {
			return AggregateKind.STRUCT;
		}
		return jdbcTypeCode == null ? null : switch ( jdbcTypeCode ) {
			case SqlTypes.STRUCT,
					SqlTypes.STRUCT_ARRAY,
					SqlTypes.STRUCT_TABLE -> AggregateKind.STRUCT;
			case SqlTypes.JSON,
					SqlTypes.JSON_ARRAY -> AggregateKind.JSON;
			case SqlTypes.SQLXML,
					SqlTypes.XML_ARRAY -> AggregateKind.XML;
			default -> null;
		};
	}

	private static QualifiedName toQualifiedName(Struct struct, BindingState state) {
		final var database = state.getDatabase();
		return new QualifiedNameImpl(
				database.toIdentifier( struct.catalog() ),
				database.toIdentifier( struct.schema() ),
				database.toIdentifier( struct.name() )
		);
	}
}
