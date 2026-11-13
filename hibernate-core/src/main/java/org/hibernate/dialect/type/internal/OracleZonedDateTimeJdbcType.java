package org.hibernate.dialect.type.internal;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.HibernateException;
import org.hibernate.TemporalValueException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ZonedDateTimeJdbcType;

/// Native Oracle region handling within a fixed supported timezone-rule environment.
/// Reject region identities that Oracle would replace with a fixed offset or alias.
/// Driver metadata methods belong to this descriptor's classloader context.
///
/// @since 8.0
/// @author Steve Ebersole
public final class OracleZonedDateTimeJdbcType extends ZonedDateTimeJdbcType {
	private final Method regionId;
	private final Method regionName;

	public OracleZonedDateTimeJdbcType(ClassLoaderService classLoader) {
		try {
			final var zoneMap = classLoader.classForName( "oracle.sql.ZONEIDMAP" );
			regionId = zoneMap.getMethod( "getID", String.class );
			regionName = zoneMap.getMethod( "getRegion", int.class );
		}
		catch (ReflectiveOperationException ex) {
			throw new HibernateException( "Could not resolve Oracle JDBC timezone metadata", ex );
		}
	}

	public boolean supportsZone(ZoneId zone) {
		if ( zone instanceof ZoneOffset offset ) {
			return offset.getTotalSeconds() % 60 == 0;
		}
		try {
			final int id = (Integer) regionId.invoke( null, zone.getId() );
			return id >= 0 && zone.getId().equals( regionName.invoke( null, id ) );
		}
		catch (ReflectiveOperationException ex) {
			throw new HibernateException( "Could not assess Oracle timezone identity '" + zone + "'", ex );
		}
	}

	private void check(ZonedDateTime value) {
		if ( value != null ) {
			OracleTemporalJdbcTypeSupport.checkOffset( value );
			if ( !supportsZone( value.getZone() ) ) {
				throw new TemporalValueException( "Oracle native ZonedDateTime handling cannot preserve zone identity '"
						+ value.getZone() + "'; use COLUMN or instant-oriented adjustment handling" );
			}
		}
	}

	@Override
	public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
		return OracleTemporalJdbcTypeSupport.binder( javaType, this, ZonedDateTime.class, this::check );
	}

	@Override
	public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
		return OracleTemporalJdbcTypeSupport.extractor( javaType, this, ZonedDateTime.class, this::check );
	}
}
