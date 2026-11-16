package org.hibernate.boot.internal;

import java.util.IdentityHashMap;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.type.descriptor.jdbc.internal.TemporalPreservationGuard;

import static java.util.Collections.newSetFromMap;
import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION;

/// Rejects, when temporal preservation is enabled, every mapped Java Time
/// value whose combination of Java type and JDBC type is not reported by the
/// [Dialect][Dialect#getTemporalPreservation] as preserving every part the
/// Java type requires.
///
/// Covers identifiers, versions, basic attributes, members of embeddables,
/// and the keys, indexes and elements of collections. Java Time values which
/// are members of an embeddable aggregated into a single column, such as a
/// `STRUCT`, JSON or XML column, are always rejected, as are arrays and
/// collections of Java Time values stored in a single column.
///
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION
///
/// @since 8.1
/// @author Steve Ebersole
final class TemporalPreservationValidator {
	private final Dialect dialect;
	private final Set<Value> visited = newSetFromMap( new IdentityHashMap<>() );

	private TemporalPreservationValidator(Dialect dialect) {
		this.dialect = dialect;
	}

	static void validate(MetadataImplementor metadata) {
		final var validator = new TemporalPreservationValidator( metadata.getDatabase().getDialect() );
		for ( var entityBinding : metadata.getEntityBindings() ) {
			validator.validateEntity( entityBinding );
		}
		for ( var collectionBinding : metadata.getCollectionBindings() ) {
			validator.validateValue( collectionBinding, collectionBinding.getRole(), false );
		}
	}

	private void validateEntity(PersistentClass entityBinding) {
		final String entityName = entityBinding.getEntityName();
		if ( entityBinding.getIdentifier() != null ) {
			validateValue( entityBinding.getIdentifier(), entityName + ".<id>", false );
		}
		if ( entityBinding.getIdentifierMapper() != null ) {
			validateValue( entityBinding.getIdentifierMapper(), entityName + ".<id>", false );
		}
		for ( var property : entityBinding.getPropertyClosure() ) {
			validateProperty( property, entityName, false );
		}
		for ( var join : entityBinding.getJoinClosure() ) {
			for ( var property : join.getProperties() ) {
				validateProperty( property, entityName, false );
			}
		}
	}

	private void validateProperty(Property property, String ownerRole, boolean aggregated) {
		validateValue( property.getValue(), ownerRole + "." + property.getName(), aggregated );
	}

	private void validateValue(Value value, String role, boolean aggregated) {
		if ( value == null || !visited.add( value ) ) {
			return;
		}
		if ( value instanceof BasicValue basicValue ) {
			validateBasicValue( basicValue, role, aggregated );
		}
		else if ( value instanceof Component component ) {
			final boolean aggregatedMembers = aggregated || component.getAggregateColumn() != null;
			for ( var property : component.getProperties() ) {
				validateProperty( property, role, aggregatedMembers );
			}
		}
		else if ( value instanceof Collection collection ) {
			validateValue( collection.getElement(), role + ".<element>", false );
			if ( collection instanceof IndexedCollection indexed ) {
				validateValue( indexed.getIndex(), role + ".<index>", false );
			}
			if ( collection instanceof IdentifierCollection identified ) {
				validateValue( identified.getIdentifier(), role + ".<collection-id>", false );
			}
		}
		// associations refer to identifiers, which are validated with their own entity
	}

	private void validateBasicValue(BasicValue basicValue, String role, boolean aggregated) {
		final var resolution = basicValue.resolve();
		final var javaType = resolution.getRelationalJavaType();
		if ( !TemporalPreservationGuard.appliesTo( javaType ) ) {
			return;
		}
		final String problem = aggregated
				? "with '" + TEMPORAL_PRESERVATION + "' enabled, Java Time values within an embeddable"
					+ " aggregated into a single column are not supported"
				: TemporalPreservationGuard.unsupportedReason( dialect, javaType, resolution.getJdbcType() );
		if ( problem != null ) {
			throw new MappingException(
					"Mapping '" + role + "' of type '" + javaType.getTypeName() + "' is not supported: " + problem
			);
		}
	}
}
