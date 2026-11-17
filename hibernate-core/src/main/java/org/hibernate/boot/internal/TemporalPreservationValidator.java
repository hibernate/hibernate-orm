package org.hibernate.boot.internal;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.internal.TemporalPolicyResolver;
import org.hibernate.boot.model.internal.TimeZoneStorageHelper;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.spi.TemporalPreservation.Part;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.type.descriptor.converter.internal.TimeZoneAdjustmentConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ZoneIdJavaType;
import org.hibernate.type.descriptor.java.ZoneOffsetJavaType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.TemporalPreservationGuard;
import org.hibernate.type.internal.TemporalPolicySettings;
import org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType;
import org.hibernate.usertype.internal.OffsetDateTimeCompositeUserType;
import org.hibernate.usertype.internal.OffsetDateTimeOffsetCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;
import org.hibernate.usertype.internal.ZonedDateTimeCompositeUserType;
import org.hibernate.usertype.internal.ZonedDateTimeZoneIdCompositeUserType;

import static java.util.Collections.newSetFromMap;
import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION;
import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION_POLICY;
import static org.hibernate.cfg.MappingSettings.TIMEZONE_DEFAULT_STORAGE;

/// Rejects, when temporal preservation is enabled, every mapped Java Time
/// value whose combination of Java type and JDBC type is not reported by the
/// [Dialect][Dialect#getTemporalPreservation] as preserving every part the
/// Java type requires.
///
/// What an `OffsetDateTime` or `ZonedDateTime` must preserve is decided by its
/// [TemporalPreservationPolicy]; every other type must preserve all of its parts.
///
/// Covers identifiers, versions, basic attributes, members of embeddables,
/// and the keys, indexes and elements of collections. Java Time values which
/// are members of an embeddable aggregated into a single column, such as a
/// `STRUCT`, JSON or XML column, or of an array or collection of such
/// embeddables, are always rejected, as are arrays and collections of Java
/// Time values stored in a single column, and `@TimeZoneStorage`, in favor of
/// `@TimeZoneRepresentation`. A `@TemporalPreservationPolicy` or
/// `@TimeZoneRepresentation` on a field or property it does not apply to is
/// rejected too, as is a `@TimeZoneAdjustment` on one it is not applied to.
///
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION
///
/// @since 8.1
/// @author Steve Ebersole
final class TemporalPreservationValidator {
	private static final Map<String, Class<?>> TIME_ZONE_COLUMN_TYPES = Map.of(
			OffsetDateTimeCompositeUserType.class.getName(), OffsetDateTime.class,
			OffsetDateTimeOffsetCompositeUserType.class.getName(), OffsetDateTime.class,
			ZonedDateTimeCompositeUserType.class.getName(), ZonedDateTime.class,
			ZonedDateTimeZoneIdCompositeUserType.class.getName(), ZonedDateTime.class,
			OffsetTimeCompositeUserType.class.getName(), OffsetTime.class
	);

	private final Dialect dialect;
	private final Map<?, ?> settings;
	private final ModelsContext modelsContext;
	private final Set<Value> visited = newSetFromMap( new IdentityHashMap<>() );

	private TemporalPreservationValidator(Dialect dialect, Map<?, ?> settings, ModelsContext modelsContext) {
		this.dialect = dialect;
		this.settings = settings;
		this.modelsContext = modelsContext;
	}

	static void validate(MetadataImplementor metadata, ModelsContext modelsContext) {
		final Map<?, ?> settings = metadata.getMetadataBuildingOptions()
				.getServiceRegistry()
				.requireService( ConfigurationService.class )
				.getSettings();
		TemporalPolicySettings.validate( settings );
		final var validator = new TemporalPreservationValidator(
				metadata.getDatabase().getDialect(),
				settings,
				modelsContext
		);
		for ( var entityBinding : metadata.getEntityBindings() ) {
			validator.validateEntity( entityBinding );
		}
		for ( var collectionBinding : metadata.getCollectionBindings() ) {
			validator.validateValue( collectionBinding, collectionBinding.getRole(), null, null, false );
		}
	}

	private void validateEntity(PersistentClass entityBinding) {
		final String entityName = entityBinding.getEntityName();
		final String className = entityBinding.getClassName();
		if ( entityBinding.getIdentifier() != null ) {
			if ( entityBinding.getIdentifierProperty() != null ) {
				checkDeclarations(
						entityBinding.getIdentifier(),
						entityName + ".<id>",
						findMember( className, entityBinding.getIdentifierProperty().getName() )
				);
			}
			validateValue( entityBinding.getIdentifier(), entityName + ".<id>", null, null, false );
		}
		if ( entityBinding.getIdentifierMapper() != null ) {
			validateValue( entityBinding.getIdentifierMapper(), entityName + ".<id>", null, null, false );
		}
		for ( var property : entityBinding.getPropertyClosure() ) {
			validateProperty( property, entityName, className, false );
		}
		for ( var join : entityBinding.getJoinClosure() ) {
			for ( var property : join.getProperties() ) {
				validateProperty( property, entityName, className, false );
			}
		}
	}

	private void validateProperty(Property property, String ownerRole, String ownerClassName, boolean aggregated) {
		validateValue(
				property.getValue(),
				ownerRole + "." + property.getName(),
				ownerClassName,
				property.getName(),
				aggregated
		);
	}

	/// @param ownerClassName the name of the class which has the attribute,
	///        or `null` when the value is not that of an attribute
	/// @param attributeName the name of the attribute, likewise
	private void validateValue(
			Value value,
			String role,
			String ownerClassName,
			String attributeName,
			boolean aggregated) {
		if ( value == null || !visited.add( value ) ) {
			return;
		}
		if ( attributeName != null ) {
			checkDeclarations( value, role, findMember( ownerClassName, attributeName ) );
		}
		if ( value instanceof BasicValue basicValue ) {
			final var elementAggregate = aggregateOfElements( basicValue );
			if ( elementAggregate != null ) {
				// an array or collection of embeddables, aggregated into a single column
				for ( var property : elementAggregate.getProperties() ) {
					validateProperty( property, role + ".<element>", elementAggregate.getComponentClassName(), true );
				}
			}
			else {
				validateBasicValue( basicValue, role, aggregated );
			}
		}
		else if ( value instanceof Component component ) {
			if ( isStoredInTwoColumns( component ) ) {
				if ( aggregated ) {
					throw new MappingException(
							"Mapping '" + role + "' is not supported: with '" + TEMPORAL_PRESERVATION
							+ "' enabled, Java Time values within an embeddable aggregated into a single column"
							+ " are not supported"
					);
				}
				validateOffsetColumn( component, role, findMember( ownerClassName, attributeName ) );
				return;
			}
			final boolean aggregatedMembers = aggregated || component.getAggregateColumn() != null;
			for ( var property : component.getProperties() ) {
				validateProperty( property, role, component.getComponentClassName(), aggregatedMembers );
			}
		}
		else if ( value instanceof Collection collection ) {
			validateValue( collection.getElement(), role + ".<element>", null, null, false );
			if ( collection instanceof IndexedCollection indexed ) {
				validateValue( indexed.getIndex(), role + ".<index>", null, null, false );
			}
			if ( collection instanceof IdentifierCollection identified ) {
				validateValue( identified.getIdentifier(), role + ".<collection-id>", null, null, false );
			}
		}
		// associations refer to identifiers, which are validated with their own entity
	}

	/// Reject a declaration on the field or property which is not applied to its value.
	private static void checkDeclarations(Value value, String role, MemberDetails member) {
		if ( !acceptsDeclarations( value ) ) {
			rejectDeclarations( member, role );
		}
		if ( member != null
				&& TimeZoneStorageHelper.declaresTimeZoneAdjustment( member )
				&& !isAdjusted( value )
				// which has a message of its own
				&& !( value instanceof Component component && isStoredInTwoColumns( component ) ) ) {
			throw new MappingException(
					"Mapping '" + role + "' is not supported: with '" + TEMPORAL_PRESERVATION + "' enabled,"
					+ " @TimeZoneAdjustment applies only to an OffsetDateTime or ZonedDateTime stored in a"
					+ " single column, which is not an identifier or version and has no converter or custom"
					+ " type, which this is not"
			);
		}
	}

	private static boolean isStoredInTwoColumns(Component component) {
		return component.getTypeName() != null && TIME_ZONE_COLUMN_TYPES.containsKey( component.getTypeName() );
	}

	/// The embeddable whose instances are the elements of the value, when the
	/// value is an array or collection of embeddables aggregated into a single
	/// column, and `null` otherwise.
	private static Component aggregateOfElements(BasicValue basicValue) {
		for ( var selectable : basicValue.getSelectables() ) {
			if ( selectable instanceof AggregateColumn aggregateColumn ) {
				return aggregateColumn.getComponent();
			}
		}
		return null;
	}

	/// Whether a `@TemporalPreservationPolicy` or `@TimeZoneRepresentation`
	/// on the attribute which has the value means anything: the value, or
	/// each element or key of it, is an `OffsetDateTime` or `ZonedDateTime`.
	private static boolean acceptsDeclarations(Value value) {
		if ( value instanceof BasicValue basicValue ) {
			return aggregateOfElements( basicValue ) == null
				&& TemporalPolicySettings.appliesTo( basicValue.resolve().getRelationalJavaType().getJavaTypeClass() );
		}
		else if ( value instanceof Component component ) {
			return isStoredInTwoColumns( component );
		}
		else if ( value instanceof Collection collection ) {
			return acceptsDeclarations( collection.getElement() )
				|| collection instanceof IndexedCollection indexed && acceptsDeclarations( indexed.getIndex() );
		}
		else {
			return false;
		}
	}

	/// Whether the zone declared with `@TimeZoneAdjustment` is applied to the
	/// value, or to each element of it.
	private static boolean isAdjusted(Value value) {
		if ( value instanceof BasicValue basicValue ) {
			final var converter = basicValue.getJpaAttributeConverterDescriptor();
			return converter != null
				&& TimeZoneAdjustmentConverter.class.isAssignableFrom( converter.getAttributeConverterClass() );
		}
		else {
			return value instanceof Collection collection && isAdjusted( collection.getElement() );
		}
	}

	/// A declaration on a field or property which it does not apply to would
	/// be ignored, leaving the author to believe it had been honored.
	private static void rejectDeclarations(MemberDetails member, String role) {
		if ( member == null ) {
			return;
		}
		final String declared;
		if ( TemporalPolicyResolver.declaresPolicy( member ) ) {
			declared = "@TemporalPreservationPolicy";
		}
		else if ( member.hasDirectAnnotationUsage( TimeZoneRepresentation.class ) ) {
			declared = "@TimeZoneRepresentation";
		}
		else {
			return;
		}
		throw new MappingException(
				"Mapping '" + role + "' is not supported: with '" + TEMPORAL_PRESERVATION + "' enabled, "
				+ declared + " applies only to a value stored as an OffsetDateTime or ZonedDateTime,"
				+ " which this is not"
		);
	}

	/// The value is stored as an instant in one column and, in a second, the
	/// offset of an `OffsetDateTime` in seconds or the identifier of the zone
	/// of a `ZonedDateTime`, by way of `@TimeZoneRepresentation`. Neither
	/// loses anything. The instant column is held to what the Dialect reports
	/// for an `Instant` stored that way.
	private void validateOffsetColumn(Component component, String role, MemberDetails member) {
		final Class<?> javaClass = TIME_ZONE_COLUMN_TYPES.get( component.getTypeName() );
		rejectTimeZoneStorage( member, role );
		if ( !TemporalPolicySettings.appliesTo( javaClass ) ) {
			throw new MappingException(
					"Mapping '" + role + "' of type '" + javaClass.getName() + "' is not supported: with '"
					+ TEMPORAL_PRESERVATION + "' enabled, values of this type are not supported"
			);
		}

		final boolean zoneIdColumn = ZonedDateTimeZoneIdCompositeUserType.class.getName().equals( component.getTypeName() );
		final var instantValue = (BasicValue) component.getProperty(
				AbstractTimeZoneStorageCompositeUserType.INSTANT_NAME
		).getValue();
		final var companionValue = (BasicValue) component.getProperty(
				zoneIdColumn
						? ZonedDateTimeZoneIdCompositeUserType.ZONE_ID_NAME
						: AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME
		).getValue();
		visited.add( instantValue );
		visited.add( companionValue );

		final var instantResolution = instantValue.resolve();
		final String instantProblem = TemporalPreservationGuard.unsupportedReason(
				dialect,
				instantResolution.getRelationalJavaType(),
				instantResolution.getJdbcType()
		);
		if ( instantProblem != null ) {
			throw new MappingException(
					"Mapping '" + role + "' of type '" + javaClass.getName()
					+ "' is not supported: for the column holding its instant, " + instantProblem
			);
		}
		final var companionResolution = companionValue.resolve();
		final boolean companionAsExpected = zoneIdColumn
				? companionResolution.getRelationalJavaType().getClass() == ZoneIdJavaType.class
						&& companionResolution.getJdbcType().getClass() == VarcharJdbcType.class
				: companionResolution.getRelationalJavaType().getClass() == ZoneOffsetJavaType.class
						&& companionResolution.getJdbcType().getClass() == IntegerJdbcType.class;
		if ( !companionAsExpected ) {
			throw new MappingException(
					"Mapping '" + role + "' of type '" + javaClass.getName() + "' is not supported: with '"
					+ TEMPORAL_PRESERVATION + "' enabled, the column holding its "
					+ ( zoneIdColumn ? "zone must be a character column" : "offset must be an integer column" )
					+ ", which " + companionResolution.getJdbcType().getClass().getSimpleName()
					+ " has not been established to be"
			);
		}

		// an offset column does not hold the zone of a ZonedDateTime
		if ( member != null && TimeZoneStorageHelper.resolveTimeZoneAdjustment( member ) != null ) {
			throw new MappingException(
					"Mapping '" + role + "' of type '" + javaClass.getName() + "' is not supported: with '"
					+ TEMPORAL_PRESERVATION + "' enabled, a zone declared with @TimeZoneAdjustment cannot be"
					+ " combined with a second column, which stores the zone or offset of each value; remove"
					+ " @TimeZoneRepresentation(COLUMN), or cancel the declared zone with"
					+ " @TimeZoneAdjustment(TimeZoneAdjustment.NONE)"
			);
		}
		final var selection = TemporalPolicyResolver.resolve( member, javaClass, settings, role );
		if ( javaClass == ZonedDateTime.class
				&& !zoneIdColumn
				&& selection.policy() == TemporalPreservationPolicy.Type.VALUE ) {
			throw new MappingException(
					"Mapping '" + role + "' of type '" + javaClass.getName() + "' is not supported: with '"
					+ TEMPORAL_PRESERVATION + "' enabled, storing only the offset in a separate column is known"
					+ " to lose the ZONE_ID part of the value, which the VALUE policy selected by "
					+ selection.source() + " requires"
			);
		}
	}

	/// With temporal preservation enabled, where the offset is stored is
	/// selected with `@TimeZoneRepresentation`, and `@TimeZoneStorage`, which
	/// selects several things at once, is not accepted.
	private void rejectTimeZoneStorage(MemberDetails member, String role) {
		if ( member != null && member.hasDirectAnnotationUsage( TimeZoneStorage.class ) ) {
			throw new MappingException(
					"Mapping '" + role + "' is not supported: with '" + TEMPORAL_PRESERVATION
					+ "' enabled, @TimeZoneStorage is not supported; select where the offset is stored with"
					+ " @TimeZoneRepresentation, and what must be preserved with @TemporalPreservationPolicy"
			);
		}
		final Object defaultStorage = settings.get( TIMEZONE_DEFAULT_STORAGE );
		if ( defaultStorage != null
				&& !defaultStorage.toString().trim().equalsIgnoreCase( TimeZoneStorageType.DEFAULT.name() ) ) {
			throw new MappingException(
					"Mapping '" + role + "' is not supported: with '" + TEMPORAL_PRESERVATION
					+ "' enabled, setting '" + TIMEZONE_DEFAULT_STORAGE + "' is not supported; select where the"
					+ " offset is stored with @TimeZoneRepresentation, and what must be preserved with"
					+ " @TemporalPreservationPolicy or '" + TEMPORAL_PRESERVATION_POLICY + "'"
			);
		}
	}

	/// The field or property which maps the attribute, looked for in the
	/// class and then in its superclasses, or `null` when there is none.
	private MemberDetails findMember(String className, String attributeName) {
		if ( className == null || attributeName == null ) {
			return null;
		}
		final var classDetails = modelsContext.getClassDetailsRegistry().findClassDetails( className );
		for ( var type = classDetails; type != null; type = type.getSuperClass() ) {
			MemberDetails fallback = null;
			for ( var field : type.getFields() ) {
				if ( attributeName.equals( field.resolveAttributeName() ) ) {
					if ( !field.getDirectAnnotationUsages().isEmpty() ) {
						return field;
					}
					fallback = field;
				}
			}
			for ( var method : type.getMethods() ) {
				if ( attributeName.equals( method.resolveAttributeName() ) ) {
					if ( !method.getDirectAnnotationUsages().isEmpty() ) {
						return method;
					}
					if ( fallback == null ) {
						fallback = method;
					}
				}
			}
			if ( fallback != null ) {
				return fallback;
			}
		}
		return null;
	}

	private void validateBasicValue(BasicValue basicValue, String role, boolean aggregated) {
		final var resolution = basicValue.resolve();
		final var javaType = resolution.getRelationalJavaType();
		if ( !TemporalPreservationGuard.appliesTo( javaType ) ) {
			return;
		}
		if ( aggregated ) {
			throw unsupported(
					role,
					javaType,
					"with '" + TEMPORAL_PRESERVATION + "' enabled, Java Time values within an embeddable"
						+ " aggregated into a single column are not supported"
			);
		}

		final Class<?> javaClass = javaType.getJavaTypeClass();
		final var jdbcType = resolution.getJdbcType();
		if ( !TemporalPolicySettings.appliesTo( javaClass ) ) {
			final String problem = TemporalPreservationGuard.unsupportedReason( dialect, javaType, jdbcType );
			if ( problem != null ) {
				throw unsupported( role, javaType, problem );
			}
			return;
		}

		rejectTimeZoneStorage( basicValue.getMemberDetails(), role );

		// what an OffsetDateTime or ZonedDateTime must preserve is a matter of policy
		final var selection = TemporalPolicyResolver.resolve( basicValue.getMemberDetails(), javaClass, settings, role );
		// with a zone declared by @TimeZoneAdjustment, the zone and offset are part of
		// the mapping, and a value which has another is rejected or converted when written
		final boolean adjusted = isAdjusted( basicValue );
		final boolean normalized = adjusted || selection.policy() == TemporalPreservationPolicy.Type.NORMALIZED;
		final String problem = TemporalPreservationGuard.unsupportedReason(
				dialect,
				javaType,
				jdbcType,
				normalized ? Part.requiredOfNormalized( javaClass ) : Part.requiredBy( javaClass )
		);
		if ( problem != null ) {
			final boolean normalizedWouldDo = !normalized && TemporalPreservationGuard.unsupportedReason(
					dialect,
					javaType,
					jdbcType,
					Part.requiredOfNormalized( javaClass )
			) == null;
			throw unsupported(
					role,
					javaType,
					problem + ", which "
						+ ( adjusted
								? "is required even with a zone declared by @TimeZoneAdjustment"
								: "the " + selection.policy() + " policy selected by " + selection.source()
									+ " requires" )
						+ ( normalizedWouldDo
								? "; the instant is preserved, so the mapping is supported with the NORMALIZED"
									+ " policy, selected with @TemporalPreservationPolicy or '"
									+ TEMPORAL_PRESERVATION_POLICY + "'"
								: "" )
			);
		}
	}

	private static MappingException unsupported(String role, JavaType<?> javaType, String problem) {
		return new MappingException(
				"Mapping '" + role + "' of type '" + javaType.getTypeName() + "' is not supported: " + problem
		);
	}
}
