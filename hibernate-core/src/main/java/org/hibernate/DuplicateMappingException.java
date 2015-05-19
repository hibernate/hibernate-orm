/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Raised whenever a duplicate for a certain type occurs.  Duplicate class, table, property name etc.
 * 
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class DuplicateMappingException extends MappingException {
	/**
	 * Enumeration of the types of things that can be duplicated.
	 */
	public static enum Type {
		/**
		 * A duplicate entity definition was encountered.
		 */
		ENTITY( "entity" ),
		/**
		 * A duplicate collection role was encountered
		 */
		COLLECTION( "collection" ),
		/**
		 * A duplicate table definition was encountered.
		 */
		TABLE( "table" ),
		/**
		 * A duplicate property/attribute definition was encountered.
		 */
		PROPERTY( "property" ),
		/**
		 * A duplicate column definition was encountered.
		 */
		COLUMN( "column" ),
		/**
		 * A duplicate column definition was encountered.
		 */
		COLUMN_BINDING( "column-binding" ),
		/**
		 * A duplicate named entity graph was encountered
		 */
		NAMED_ENTITY_GRAPH( "NamedEntityGraph" ),
		/**
		 * A duplicate named query (ql or native) was encountered
		 */
		QUERY( "query" ),
		/**
		 * A duplicate ResultSetMapping was encountered
		 */
		RESULT_SET_MAPPING( "ResultSetMapping" ),
		/**
		 * A duplicate NamedStoredProcedureQuery was encountered
		 */
		PROCEDURE( "NamedStoredProcedureQuery" );

		private final String text;

		Type(String text) {
			this.text = text;
		}
	}

	private final String name;
	private final String type;

	/**
	 * Creates a DuplicateMappingException using the given type and name.
	 *
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 */
	public DuplicateMappingException(Type type, String name) {
		this( type.text, name );
	}

	/**
	 * Creates a DuplicateMappingException using the given type and name.
	 *
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 *
	 * @deprecated Use the for taking {@link Type} instead.
	 */
	@Deprecated
	public DuplicateMappingException(String type, String name) {
		this( "Duplicate " + type + " mapping " + name, type, name );
	}

	/**
	 * Creates a DuplicateMappingException using the given customMessage, type and name.
	 *
	 * @param customMessage A custom exception message explaining the exception condition
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 */
	public DuplicateMappingException(String customMessage, Type type, String name) {
		this( customMessage, type.name(), name );
	}

	/**
	 * Creates a DuplicateMappingException using the given customMessage, type and name.
	 *
	 * @param customMessage A custom exception message explaining the exception condition
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 *
	 * @deprecated Use the for taking {@link Type} instead.
	 */
	@Deprecated
	public DuplicateMappingException(String customMessage, String type, String name) {
		super( customMessage );
		this.type=type;
		this.name=name;
	}

	public String getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
}
