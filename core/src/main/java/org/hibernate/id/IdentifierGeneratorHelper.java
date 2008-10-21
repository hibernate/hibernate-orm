/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.id;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * Factory and helper methods for <tt>IdentifierGenerator</tt> framework.
 *
 * @author Gavin King
 */
public final class IdentifierGeneratorHelper {
	private static final Logger log = LoggerFactory.getLogger( IdentifierGeneratorHelper.class );

	/**
	 * Marker object returned from {@link IdentifierGenerator#generate} to indicate that we should short-circuit any
	 * continued generated id checking.  Currently this is only used in the case of the
	 * {@link org.hibernate.id.ForeignGenerator foreign} generator as a way to signal that we should use the associated
	 * entity's id value.
	 */
	public static final Serializable SHORT_CIRCUIT_INDICATOR = new Serializable() {
		public String toString() {
			return "SHORT_CIRCUIT_INDICATOR";
		}
	};

	/**
	 * Marker object returned from {@link IdentifierGenerator#generate} to indicate that the entity's identifier will
	 * be generated as part of the datbase insertion.
	 */
	public static final Serializable POST_INSERT_INDICATOR = new Serializable() {
		public String toString() {
			return "POST_INSERT_INDICATOR";
		}
	};


	/**
	 * Get the generated identifier when using identity columns
	 *
	 * @param rs The result set from which to extract the the generated identity.
	 * @param type The expected type mapping for the identity value.
	 * @return The generated identity value
	 * @throws SQLException Can be thrown while accessing the result set
	 * @throws HibernateException Indicates a problem reading back a generated identity value.
	 */
	public static Serializable getGeneratedIdentity(ResultSet rs, Type type) throws SQLException, HibernateException {
		if ( !rs.next() ) {
			throw new HibernateException( "The database returned no natively generated identity value" );
		}
		final Serializable id = IdentifierGeneratorHelper.get( rs, type );
		log.debug( "Natively generated identity: " + id );
		return id;
	}

	/**
	 * Extract the value from the result set (which is assumed to already have been positioned to the apopriate row)
	 * and wrp it in the appropriate Java numeric type.
	 *
	 * @param rs The result set from which to extract the value.
	 * @param type The expected type of the value.
	 * @return The extracted value.
	 * @throws SQLException Indicates problems access the result set
	 * @throws IdentifierGenerationException Indicates an unknown type.
	 */
	public static Serializable get(ResultSet rs, Type type) throws SQLException, IdentifierGenerationException {
		Class clazz = type.getReturnedClass();
		if ( clazz == Long.class ) {
			return new Long( rs.getLong( 1 ) );
		}
		else if ( clazz == Integer.class ) {
			return new Integer( rs.getInt( 1 ) );
		}
		else if ( clazz == Short.class ) {
			return new Short( rs.getShort( 1 ) );
		}
		else if ( clazz == String.class ) {
			return rs.getString( 1 );
		}
		else {
			throw new IdentifierGenerationException( "this id generator generates long, integer, short or string" );
		}

	}

	/**
	 * Wrap the given value in the given Java numeric class.
	 *
	 * @param value The primitive value to wrap.
	 * @param clazz The Java numeric type in which to wrap the value.
	 * @return The wrapped type.
	 * @throws IdentifierGenerationException Indicates an unhandled 'clazz'.
	 */
	public static Number createNumber(long value, Class clazz) throws IdentifierGenerationException {
		if ( clazz == Long.class ) {
			return new Long( value );
		}
		else if ( clazz == Integer.class ) {
			return new Integer( ( int ) value );
		}
		else if ( clazz == Short.class ) {
			return new Short( ( short ) value );
		}
		else {
			throw new IdentifierGenerationException( "this id generator generates long, integer, short" );
		}
	}

	/**
	 * Disallow instantiation of IdentifierGeneratorHelper.
	 */
	private IdentifierGeneratorHelper() {
	}

}
