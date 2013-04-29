/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.property.Getter;
import org.hibernate.type.IdentifierType;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * Helper for dealing with unsaved value handling
 *
 * @author Gavin King
 */
public class UnsavedValueFactory {

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
	 * reading it's id property, or if that is not possible, using the java default
	 * value for the type
	 *
	 * @param unsavedValue The mapping defined unsaved value
	 * @param identifierGetter The getter for the entity identifier attribute
	 * @param identifierType The mapping type for the identifier
	 * @param constructor The constructor for the entity
	 *
	 * @return The appropriate IdentifierValue
	 */
	public static IdentifierValue getUnsavedIdentifierValue(
			String unsavedValue,
			Getter identifierGetter,
			Type identifierType,
			Constructor constructor) {
		if ( unsavedValue == null ) {
			if ( identifierGetter != null && constructor != null ) {
				// use the id value of a newly instantiated instance as the unsaved-value
				final Serializable defaultValue = (Serializable) identifierGetter.get( instantiate( constructor ) );
				return new IdentifierValue( defaultValue );
			}
			else if ( identifierGetter != null && (identifierType instanceof PrimitiveType) ) {
				final Serializable defaultValue = ( (PrimitiveType) identifierType ).getDefaultValue();
				return new IdentifierValue( defaultValue );
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
			try {
				return new IdentifierValue( (Serializable) ( (IdentifierType) identifierType ).stringToObject( unsavedValue ) );
			}
			catch ( ClassCastException cce ) {
				throw new MappingException( "Bad identifier type: " + identifierType.getName() );
			}
			catch ( Exception e ) {
				throw new MappingException( "Could not parse identifier unsaved-value: " + unsavedValue );
			}
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
	public static VersionValue getUnsavedVersionValue(
			String versionUnsavedValue, 
			Getter versionGetter,
			VersionType versionType,
			Constructor constructor) {
		
		if ( versionUnsavedValue == null ) {
			if ( constructor!=null ) {
				final Object defaultValue = versionGetter.get( instantiate( constructor ) );
				// if the version of a newly instantiated object is not the same
				// as the version seed value, use that as the unsaved-value
				return versionType.isEqual( versionType.seed( null ), defaultValue )
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
