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
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.Type;
import org.hibernate.util.ReflectHelper;

/**
 * Factory and helper methods for <tt>IdentifierGenerator</tt> framework.
 *
 * @author Gavin King
 */
public final class IdentifierGeneratorFactory {

	private static final Logger log = LoggerFactory.getLogger( IdentifierGeneratorFactory.class );

	private static final HashMap GENERATORS = new HashMap();
	static {
		GENERATORS.put( "uuid", UUIDHexGenerator.class );
		GENERATORS.put( "hilo", TableHiLoGenerator.class );
		GENERATORS.put( "assigned", Assigned.class );
		GENERATORS.put( "identity", IdentityGenerator.class );
		GENERATORS.put( "select", SelectGenerator.class );
		GENERATORS.put( "sequence", SequenceGenerator.class );
		GENERATORS.put( "seqhilo", SequenceHiLoGenerator.class );
		GENERATORS.put( "increment", IncrementGenerator.class );
		GENERATORS.put( "foreign", ForeignGenerator.class );
		GENERATORS.put( "guid", GUIDGenerator.class );
		GENERATORS.put( "uuid.hex", UUIDHexGenerator.class ); 	// uuid.hex is deprecated
		GENERATORS.put( "sequence-identity", SequenceIdentityGenerator.class );
	}

	public static final Serializable SHORT_CIRCUIT_INDICATOR = new Serializable() {
		public String toString() {
			return "SHORT_CIRCUIT_INDICATOR";
		}
	};

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
		final Serializable id = IdentifierGeneratorFactory.get( rs, type );

		if ( log.isDebugEnabled() ) {
			log.debug( "Natively generated identity: " + id );
		}
		return id;
	}

	// unhappy about this being public ... is there a better way?
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

	public static IdentifierGenerator create(String strategy, Type type, Properties params, Dialect dialect)
			throws MappingException {
		try {
			Class clazz = getIdentifierGeneratorClass( strategy, dialect );
			IdentifierGenerator idgen = ( IdentifierGenerator ) clazz.newInstance();
			if ( idgen instanceof Configurable ) {
				( ( Configurable ) idgen ).configure( type, params, dialect );
			}
			return idgen;
		}
		catch ( Exception e ) {
			throw new MappingException(
					"could not instantiate id generator [entity-name=" + params.get(
							IdentifierGenerator.ENTITY_NAME
					) + "]", e
			);
		}
	}

	public static Class getIdentifierGeneratorClass(String strategy, Dialect dialect) {
		Class clazz = ( Class ) GENERATORS.get( strategy );
		if ( "native".equals( strategy ) ) {
			clazz = dialect.getNativeIdentifierGeneratorClass();
		}
		try {
			if ( clazz == null ) {
				clazz = ReflectHelper.classForName( strategy );
			}
		}
		catch ( ClassNotFoundException e ) {
			throw new MappingException( "could not interpret id generator strategy: " + strategy );
		}
		return clazz;
	}

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
	 * Disallow instantiation.
	 */
	private IdentifierGeneratorFactory() {
	}

}
