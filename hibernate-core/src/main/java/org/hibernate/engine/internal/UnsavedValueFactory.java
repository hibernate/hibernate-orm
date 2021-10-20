/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import org.hibernate.InstantiationException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.mapping.KeyValue;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.VersionJavaType;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;

/**
 * Helper for dealing with unsaved value handling
 *
 * @author Gavin King
 */
public class UnsavedValueFactory {

	/**
	 * Return the UnsavedValueStrategy for determining whether an entity instance is
	 * unsaved based on the identifier.  If an explicit strategy is not specified, determine
	 * the unsaved value by instantiating an instance of the entity and reading the value of
	 * its id property, or if that is not possible, using the java default value for the type
	 */
	public static IdentifierValue getUnsavedIdentifierValue(
			KeyValue bootIdMapping,
			JavaType<?> idJtd,
			Getter getter,
			Supplier<?> templateInstanceAccess,
			SessionFactoryImplementor sessionFactory) {
		final String unsavedValue = bootIdMapping.getNullValue();

		if ( unsavedValue == null ) {
			if ( getter != null && templateInstanceAccess != null ) {
				// use the id value of a newly instantiated instance as the unsaved-value
				final Object templateInstance = templateInstanceAccess.get();
				final Object defaultValue = getter.get( templateInstance );
				return new IdentifierValue( defaultValue );
			}
			else if ( idJtd instanceof PrimitiveJavaType ) {
				return new IdentifierValue( ( (PrimitiveJavaType<?>) idJtd ).getDefaultValue() );
			}
			else {
				return IdentifierValue.NULL;
			}
		}
		else if ( "null".equals( unsavedValue ) ) {
			return IdentifierValue.NULL;
		}
		else if ( "undefined".equals( unsavedValue ) ) {
			return IdentifierValue.UNDEFINED;
		}
		else if ( "none".equals( unsavedValue ) ) {
			return IdentifierValue.NONE;
		}
		else if ( "any".equals( unsavedValue ) ) {
			return IdentifierValue.ANY;
		}
		else {
			return new IdentifierValue( idJtd.fromString( unsavedValue ) );
		}
	}

	/**
	 * Return the UnsavedValueStrategy for determining whether an entity instance is
	 * unsaved based on the version.  If an explicit strategy is not specified, determine the
	 * unsaved value by instantiating an instance of the entity and reading the value of its
	 * version property, or if that is not possible, using the java default value for the type
	 */
	public static VersionValue getUnsavedVersionValue(
			KeyValue bootVersionMapping,
			VersionJavaType jtd,
			Getter getter,
			Supplier<?> templateInstanceAccess,
			SessionFactoryImplementor sessionFactory) {
		final String unsavedValue = bootVersionMapping.getNullValue();
		if ( unsavedValue == null ) {
			if ( getter != null && templateInstanceAccess != null ) {
				final Object templateInstance = templateInstanceAccess.get();
				final Object defaultValue = getter.get( templateInstance );

				// if the version of a newly instantiated object is not the same
				// as the version seed value, use that as the unsaved-value
				final Object seedValue = jtd.seed( null );
				return jtd.areEqual( seedValue, defaultValue )
						? VersionValue.UNDEFINED
						: new VersionValue( defaultValue );
			}
			else {
				return VersionValue.UNDEFINED;
			}
		}
		else if ( "undefined".equals( unsavedValue ) ) {
			return VersionValue.UNDEFINED;
		}
		else if ( "null".equals( unsavedValue ) ) {
			return VersionValue.NULL;
		}
		else if ( "negative".equals( unsavedValue ) ) {
			return VersionValue.NEGATIVE;
		}
		else {
			// this should not happen since the DTD prevents it
			throw new MappingException( "Could not parse version unsaved-value: " + unsavedValue );
		}

	}

	/**
	 * Instantiate a class using the provided Constructor
	 *
	 * @param constructor The constructor
	 *
	 * @return The instantiated object
	 *
	 * @throws InstantiationException if something went wrong
	 */
	private static Object instantiate(Constructor constructor) {
		try {
			return constructor.newInstance();
		}
		catch (Exception e) {
			throw new InstantiationException( "could not instantiate test object", constructor.getDeclaringClass(), e );
		}
	}

	/**
	 * Return an IdentifierValue for the specified unsaved-value. If none is specified,
	 * guess the unsaved value by instantiating a test instance of the class and
	 * reading it's version property value, or if that is not possible, using the java default
	 * value for the type
	 *
	 * @param versionUnsavedValue The mapping defined unsaved value
	 * @param versionGetter The version attribute getter
	 * @param versionType The mapping type for the version
	 * @param constructor The constructor for the entity
	 *
	 * @return The appropriate VersionValue
	 */
	public static <X> VersionValue getUnsavedVersionValue(
			String versionUnsavedValue,
			Getter versionGetter,
			VersionJavaType<X> versionType,
			Constructor constructor) {
		
		if ( versionUnsavedValue == null ) {
			if ( constructor!=null ) {
				@SuppressWarnings("unchecked")
				final X defaultValue = (X) versionGetter.get( instantiate( constructor ) );
				// if the version of a newly instantiated object is not the same
				// as the version seed value, use that as the unsaved-value
				return versionType.areEqual( versionType.seed( null ), defaultValue )
						? VersionValue.UNDEFINED
						: new VersionValue( defaultValue );
			}
			else {
				return VersionValue.UNDEFINED;
			}
		}
		else if ( "undefined".equals( versionUnsavedValue ) ) {
			return VersionValue.UNDEFINED;
		}
		else if ( "null".equals( versionUnsavedValue ) ) {
			return VersionValue.NULL;
		}
		else if ( "negative".equals( versionUnsavedValue ) ) {
			return VersionValue.NEGATIVE;
		}
		else {
			// this should not happen since the DTD prevents it
			throw new MappingException( "Could not parse version unsaved-value: " + versionUnsavedValue );
		}
	}

	private UnsavedValueFactory() {
	}
}
