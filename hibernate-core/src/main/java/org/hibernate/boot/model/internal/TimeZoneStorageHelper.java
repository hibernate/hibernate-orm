package org.hibernate.boot.model.internal;


import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import jakarta.persistence.Convert;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.MappingException;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.type.spi.TemporalPreservation.Part;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.converter.internal.TimeZoneAdjustmentConverter;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.internal.OffsetDateTimeCompositeUserType;
import org.hibernate.usertype.internal.OffsetDateTimeOffsetCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeOffsetCompositeUserType;
import org.hibernate.usertype.internal.ZonedDateTimeCompositeUserType;
import org.hibernate.usertype.internal.ZonedDateTimeZoneIdCompositeUserType;

import static org.hibernate.type.TimeZoneStorageStrategy.COLUMN;
import static org.hibernate.dialect.type.spi.TimeZoneSupport.NATIVE;

public class TimeZoneStorageHelper {

	private static final String OFFSET_TIME_CLASS = OffsetTime.class.getName();
	private static final String OFFSET_DATETIME_CLASS = OffsetDateTime.class.getName();
	private static final String ZONED_DATETIME_CLASS = ZonedDateTime.class.getName();

	static Class<? extends CompositeUserType<?>> resolveTimeZoneStorageCompositeUserType(
			MemberDetails attributeMember,
			ClassDetails returnedClass,
			MetadataBuildingContext context) {
		if ( useColumnForTimeZoneStorage( attributeMember, context ) ) {
			final String returnedClassName = returnedClass.getName();
			if ( OFFSET_DATETIME_CLASS.equals( returnedClassName ) ) {
				return isPreserved( attributeMember, context )
						? OffsetDateTimeOffsetCompositeUserType.class
						: OffsetDateTimeCompositeUserType.class;
			}
			else if ( ZONED_DATETIME_CLASS.equals( returnedClassName ) ) {
				return usesZoneIdColumn( attributeMember, context )
						? ZonedDateTimeZoneIdCompositeUserType.class
						: ZonedDateTimeCompositeUserType.class;
			}
			else if ( OFFSET_TIME_CLASS.equals( returnedClassName ) ) {
				return isPreserved( attributeMember, context )
						? OffsetTimeOffsetCompositeUserType.class
						: OffsetTimeCompositeUserType.class;
			}
		}
		return null;
	}

	/// Whether [TimeZoneRepresentation] applies to the Java class.
	private static boolean hasTimeZoneRepresentation(String className) {
		return OFFSET_TIME_CLASS.equals( className )
			|| OFFSET_DATETIME_CLASS.equals( className )
			|| ZONED_DATETIME_CLASS.equals( className );
	}

	/// The [TimeZoneRepresentation] of a mapped member: from the member, the
	/// class which declares it, each superclass of that class in turn, the
	/// package of that class, or the module of that class, whichever declares
	/// one first, and otherwise [TimeZoneRepresentation.Type#INLINE].
	public static TimeZoneRepresentation.Type resolveTimeZoneRepresentation(MemberDetails member) {
		var declared = member.getDirectAnnotationUsage( TimeZoneRepresentation.class );
		final var declaring = member.getDeclaringType();
		for ( var type = declaring; declared == null && type != null; type = type.getSuperClass() ) {
			declared = type.getDirectAnnotationUsage( TimeZoneRepresentation.class );
		}
		if ( declared == null && declaring.getPackage() != null ) {
			declared = declaring.getPackage().getDirectAnnotationUsage( TimeZoneRepresentation.class );
		}
		if ( declared == null && declaring.getModule() != null ) {
			declared = declaring.getModule().getDirectAnnotationUsage( TimeZoneRepresentation.class );
		}
		return declared == null ? TimeZoneRepresentation.Type.DEFAULT : declared.value();
	}

	/// The zone declared for the member with `@TimeZoneAdjustment`, on the
	/// member itself, the class which declares it, each superclass of that
	/// class in turn, the package of that class, or the module of that
	/// class, whichever declares one first, and `null` when there is none or
	/// it is [TimeZoneAdjustment#NONE].
	public static ZoneId resolveTimeZoneAdjustment(MemberDetails member) {
		var declared = member.getDirectAnnotationUsage( TimeZoneAdjustment.class );
		String source = "'" + member.getDeclaringType().getName() + "." + member.getName() + "'";
		final var declaring = member.getDeclaringType();
		for ( var type = declaring; declared == null && type != null; type = type.getSuperClass() ) {
			declared = type.getDirectAnnotationUsage( TimeZoneAdjustment.class );
			source = "class '" + type.getName() + "'";
		}
		if ( declared == null && declaring.getPackage() != null ) {
			declared = declaring.getPackage().getDirectAnnotationUsage( TimeZoneAdjustment.class );
			source = "the package of '" + declaring.getName() + "'";
		}
		if ( declared == null && declaring.getModule() != null ) {
			declared = declaring.getModule().getDirectAnnotationUsage( TimeZoneAdjustment.class );
			source = "the module of '" + declaring.getName() + "'";
		}
		if ( declared == null || TimeZoneAdjustment.NONE.equals( declared.value() ) ) {
			return null;
		}
		try {
			return ZoneId.of( declared.value() );
		}
		catch (DateTimeException | NullPointerException e) {
			throw new MappingException(
					"Invalid @TimeZoneAdjustment on " + source + ": '" + declared.value() + "' is not a time zone",
					e
			);
		}
	}

	/// Whether the member itself declares a zone with `@TimeZoneAdjustment`.
	public static boolean declaresTimeZoneAdjustment(MemberDetails member) {
		final var declared = member.getDirectAnnotationUsage( TimeZoneAdjustment.class );
		return declared != null && !TimeZoneAdjustment.NONE.equals( declared.value() );
	}

	/// The converter which applies the zone declared with `@TimeZoneAdjustment`
	/// to the values of the member, or `null` when temporal preservation is
	/// disabled, the values are of another type, no zone is declared, or the
	/// member is one a declared zone is not applied to.
	///
	/// @param javaClassName the name of the class of the values, which for a
	///        collection is that of its elements
	static ConverterDescriptor<?, ?> timeZoneAdjustmentConverter(
			MemberDetails member,
			String javaClassName,
			MetadataBuildingContext context) {
		final Class<?> javaClass;
		if ( OFFSET_TIME_CLASS.equals( javaClassName ) ) {
			javaClass = OffsetTime.class;
		}
		else if ( OFFSET_DATETIME_CLASS.equals( javaClassName ) ) {
			javaClass = OffsetDateTime.class;
		}
		else if ( ZONED_DATETIME_CLASS.equals( javaClassName ) ) {
			javaClass = ZonedDateTime.class;
		}
		else {
			return null;
		}
		final var serviceRegistry = context.getBootstrapContext().getServiceRegistry();
		if ( !MetadataBuildingContext.isTemporalPreservationEnabled( serviceRegistry )
				|| member.hasDirectAnnotationUsage( Id.class )
				|| member.hasDirectAnnotationUsage( Version.class )
				|| member.hasDirectAnnotationUsage( JavaType.class )
				|| member.hasDirectAnnotationUsage( Type.class ) ) {
			return null;
		}
		final var zone = resolveTimeZoneAdjustment( member );
		if ( zone == null ) {
			return null;
		}
		final String role = member.getDeclaringType().getName() + "." + member.getName();
		if ( javaClass == OffsetTime.class && !( zone instanceof ZoneOffset ) ) {
			// without a date, the offset of a region-based zone cannot be determined
			throw new MappingException(
					"Invalid @TimeZoneAdjustment for '" + role + "': the zone of an OffsetTime must be a fixed"
					+ " offset, such as '+02:00' or 'Z', which '" + zone + "' is not"
			);
		}
		final var policy = TemporalPolicyResolver.resolve(
				member,
				javaClass,
				serviceRegistry.requireService( ConfigurationService.class ).getSettings(),
				role
		).policy();
		return ConverterDescriptors.of( TimeZoneAdjustmentConverter.forClass(
				javaClass,
				zone,
				policy == TemporalPreservationPolicy.Type.VALUE,
				role
		) );
	}

	/// A member of an embeddable which is aggregated into a single column,
	/// such as a `STRUCT`, JSON or XML column, cannot be given a second
	/// column by temporal preservation, and is left as a single value.
	///
	/// @param compositeUserType the type selected for the member, or `null`
	/// @return the type to use, or `null` for none
	static Class<? extends CompositeUserType<?>> withinAggregate(
			Class<? extends CompositeUserType<?>> compositeUserType,
			PropertyHolder propertyHolder) {
		return propertyHolder instanceof ComponentPropertyHolder componentHolder
				&& componentHolder.getAggregateColumn() != null
				&& ( compositeUserType == OffsetTimeOffsetCompositeUserType.class
					|| compositeUserType == OffsetDateTimeOffsetCompositeUserType.class
					|| compositeUserType == ZonedDateTimeZoneIdCompositeUserType.class )
				? null
				: compositeUserType;
	}

	/// Whether the member is mapped with temporal preservation enabled and
	/// stores the zone of a `ZonedDateTime` in its second column, as opposed
	/// to an offset.
	static boolean usesZoneIdColumn(AnnotationTarget element, MetadataBuildingContext context) {
		return element instanceof MemberDetails member
			&& ZONED_DATETIME_CLASS.equals( member.getType().getName() )
			&& isPreserved( element, context );
	}

	/// Whether the element is mapped by `@TimeZoneRepresentation`, with
	/// temporal preservation enabled, and not by `@TimeZoneStorage`.
	private static boolean isPreserved(AnnotationTarget element, MetadataBuildingContext context) {
		return element.getDirectAnnotationUsage( TimeZoneStorage.class ) == null
			&& MetadataBuildingContext.isTemporalPreservationEnabled(
					context.getBootstrapContext().getServiceRegistry() );
	}

	/// Whether the value of the member is generated by the database, which
	/// generates a single value and so cannot fill a second column.
	private static boolean isGeneratedByDatabase(MemberDetails member) {
		final var currentTimestamp = member.getDirectAnnotationUsage( CurrentTimestamp.class );
		final var creationTimestamp = member.getDirectAnnotationUsage( CreationTimestamp.class );
		final var updateTimestamp = member.getDirectAnnotationUsage( UpdateTimestamp.class );
		return member.hasDirectAnnotationUsage( Generated.class )
			|| currentTimestamp != null && currentTimestamp.source() == SourceType.DB
			|| creationTimestamp != null && creationTimestamp.source() == SourceType.DB
			|| updateTimestamp != null && updateTimestamp.source() == SourceType.DB;
	}

	/// Whether storing the member in a single column preserves every part
	/// which its policy requires, according to the Dialect. A member whose
	/// mapping is customized, or which cannot be given a second column, is
	/// left in a single column; whether that suffices is checked when the
	/// factory is built.
	private static boolean inlineMeetsPolicy(MemberDetails member, MetadataBuildingContext context) {
		if ( member.hasDirectAnnotationUsage( Id.class )
				|| member.hasDirectAnnotationUsage( Version.class )
				|| member.hasDirectAnnotationUsage( Convert.class )
				|| member.hasDirectAnnotationUsage( JdbcTypeCode.class )
				|| member.hasDirectAnnotationUsage( JdbcType.class )
				|| member.hasDirectAnnotationUsage( JavaType.class )
				|| member.hasDirectAnnotationUsage( Type.class )
				|| isGeneratedByDatabase( member ) ) {
			return true;
		}
		if ( resolveTimeZoneAdjustment( member ) != null ) {
			// the zone is part of the mapping, so a single column need only hold the instant
			return true;
		}
		final Class<?> javaClass = member.getType().determineRawClass().toJavaClass();
		final var typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final var javaType = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( javaClass );
		final var jdbcType = javaType.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
		final var policy = TemporalPolicyResolver.resolve(
				member,
				javaClass,
				context.getBootstrapContext()
						.getServiceRegistry()
						.requireService( ConfigurationService.class )
						.getSettings(),
				member.getDeclaringType().getName() + "." + member.getName()
		).policy();
		final var required = policy == TemporalPreservationPolicy.Type.NORMALIZED
				? Part.requiredOfNormalized( javaClass )
				: Part.requiredBy( javaClass );
		return context.getMetadataCollector()
				.getDatabase()
				.getDialect()
				.getTemporalPreservation( javaType, jdbcType )
				.firstUnsupportedPart( required ) == null;
	}

	private static boolean isTemporalWithTimeZoneClass(String returnedClassName) {
		return OFFSET_DATETIME_CLASS.equals( returnedClassName )
			|| ZONED_DATETIME_CLASS.equals( returnedClassName )
			|| isOffsetTimeClass( returnedClassName );
	}

	public static boolean isOffsetTimeClass(AnnotationTarget element) {
		return element instanceof MemberDetails memberDetails
			&& isOffsetTimeClass( memberDetails );
	}

	public static boolean isOffsetTimeClass(MemberDetails element) {
		final var type = element.getType();
		return type != null
			&& isOffsetTimeClass( type.determineRawClass().getClassName() );

	}

	private static boolean isOffsetTimeClass(String returnedClassName) {
		return OFFSET_TIME_CLASS.equals( returnedClassName );
	}

	static boolean useColumnForTimeZoneStorage(AnnotationTarget element, MetadataBuildingContext context) {
		final var timeZoneStorage = element.getDirectAnnotationUsage( TimeZoneStorage.class );
		if ( timeZoneStorage == null
				&& element instanceof MemberDetails attributeMember
				&& hasTimeZoneRepresentation( attributeMember.getType().getName() )
				&& MetadataBuildingContext.isTemporalPreservationEnabled(
						context.getBootstrapContext().getServiceRegistry() ) ) {
			// with temporal preservation enabled, @TimeZoneRepresentation decides
			return switch ( resolveTimeZoneRepresentation( attributeMember ) ) {
				case COLUMN -> true;
				case INLINE -> false;
				case DEFAULT -> !inlineMeetsPolicy( attributeMember, context );
			};
		}
		if ( timeZoneStorage == null ) {
			return element instanceof MemberDetails attributeMember
				&& isTemporalWithTimeZoneClass( attributeMember.getType().getName() )
				//no @TimeZoneStorage annotation, so we need to use the default storage strategy
				&& context.getBuildingOptions().getDefaultTimeZoneStorage() == COLUMN;
		}
		else {
			return switch ( timeZoneStorage.value() ) {
				case COLUMN -> true;
				// if the db has native support for timezones, we use that, not a column
				case AUTO -> context.getBuildingOptions().getTimeZoneSupport() != NATIVE;
				default -> false;
			};
		}
	}
}
