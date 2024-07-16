/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionDelegatorBaseImpl;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.mapping.KeyValue;
import org.hibernate.property.access.spi.Getter;
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
			Supplier<?> templateInstanceAccess) {
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
	 * Return the {@link org.hibernate.engine.spi.UnsavedValueStrategy} for determining
	 * whether an entity instance is unsaved based on the version.  If an explicit strategy
	 * is not specified, determine the unsaved value by instantiating an instance of the
	 * entity and reading the value of its version property, or if that is not possible,
	 * using the java default value for the type.
	 */
	public static <T> VersionValue getUnsavedVersionValue(
			KeyValue bootVersionMapping,
			VersionJavaType<T> jtd,
			Long length,
			Integer precision,
			Integer scale,
			Getter getter,
			Supplier<?> templateInstanceAccess,
			SessionFactoryImplementor sessionFactory) {
		final String unsavedValue = bootVersionMapping.getNullValue();
		if ( unsavedValue == null ) {
			if ( getter != null && templateInstanceAccess != null ) {
				Object templateInstance = templateInstanceAccess.get();
				@SuppressWarnings("unchecked")
				final T defaultValue = (T) getter.get( templateInstance );

				// if the version of a newly instantiated object is not the same
				// as the version seed value, use that as the unsaved-value
				final T seedValue = jtd.seed( length, precision, scale, mockSession( sessionFactory ) );
				return jtd.areEqual( seedValue, defaultValue )
						? VersionValue.UNDEFINED
						: new VersionValue( defaultValue );
			}
			else {
				return VersionValue.UNDEFINED;
			}
		}
		else {
			switch (unsavedValue) {
				case "undefined":
					return VersionValue.UNDEFINED;
				case "null":
					return VersionValue.NULL;
				case "negative":
					return VersionValue.NEGATIVE;
				default:
					// this should not happen since the DTD prevents it
					throw new MappingException("Could not parse version unsaved-value: " + unsavedValue);
			}
		}

	}

	private static SharedSessionDelegatorBaseImpl mockSession(SessionFactoryImplementor sessionFactory) {
		return new SharedSessionDelegatorBaseImpl(null) {

			@Override
			protected SharedSessionContract delegate() {
				throw new UnsupportedOperationException( "Operation not supported" );
			}

			@Override
			public SessionFactoryImplementor getFactory() {
				return sessionFactory;
			}

			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return sessionFactory;
			}

			@Override
			public JdbcServices getJdbcServices() {
				return sessionFactory.getJdbcServices();
			}
		};
	}

	private UnsavedValueFactory() {
	}
}
