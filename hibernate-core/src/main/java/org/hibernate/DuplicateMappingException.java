/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Raised whenever a duplicate for a certain type occurs,
 * such as a duplicate class, table, or property name.
 *
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class DuplicateMappingException extends MappingException {
	/**
	 * Enumeration of the types of things that can be duplicated.
	 */
	public enum Type {
		/**
		 * A duplicate entity definition was encountered.
		 */
		ENTITY( "entity definition" ),
		/**
		 * A duplicate collection role was encountered
		 */
		COLLECTION( "collection definition" ),
		/**
		 * A duplicate table definition was encountered.
		 */
		TABLE( "table mapping" ),
		/**
		 * A duplicate property/attribute definition was encountered.
		 */
		PROPERTY( "property mapping" ),
		/**
		 * A duplicate column definition was encountered.
		 */
		COLUMN( "column mapping" ),
		/**
		 * A duplicate column definition was encountered.
		 */
		COLUMN_BINDING( "physical column name mapping for logical column name" ),
		/**
		 * A duplicate named entity graph was encountered
		 */
		NAMED_ENTITY_GRAPH( "named entity graph" ),
		/**
		 * A duplicate named query (ql or native) was encountered
		 */
		QUERY( "named query" ),
		/**
		 * A duplicate ResultSetMapping was encountered
		 */
		RESULT_SET_MAPPING( "SQL result set mapping" ),
		/**
		 * A duplicate NamedStoredProcedureQuery was encountered
		 */
		PROCEDURE( "named stored procedure" );

		private final String text;

		Type(String text) {
			this.text = text;
		}
	}

	private final String name;
	private final String type;

	/**
	 * Creates a {@code DuplicateMappingException} using the given type and name.
	 *
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 */
	public DuplicateMappingException(Type type, String name) {
		this ( "Duplicate " + type.text + " '" + name + "'", type, name );
	}

	/**
	 * Creates a {@code DuplicateMappingException} using the given customMessage, type and name.
	 *
	 * @param customMessage A custom exception message explaining the exception condition
	 * @param type The type of the duplicated thing.
	 * @param name The name of the duplicated thing.
	 */
	public DuplicateMappingException(String customMessage, Type type, String name) {
		super( customMessage );
		this.type = type.name();
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}
}
