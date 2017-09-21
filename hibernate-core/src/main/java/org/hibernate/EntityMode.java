/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.metamodel.model.domain.RepresentationMode;

/**
 * Defines the representation modes available for entities.
 *
 * @author Steve Ebersole
 *
 * @deprecated Deprecated in favor of {@link RepresentationMode}.  Use
 * {@link #asRepresentation()} for an appropriate conversion.
 */
@Deprecated
public enum EntityMode {
	/**
	 * The {@code pojo} entity mode describes an entity model made up of entity classes (loosely) following
	 * the java bean convention.
	 */
	POJO( RepresentationMode.POJO ),

	/**
	 * The {@code dynamic-map} entity mode describes an entity model defined using {@link java.util.Map} references.
	 */
	MAP( RepresentationMode.MAP );

	private final RepresentationMode representation;

	EntityMode(RepresentationMode representation) {
		this.representation = representation;
	}

	public RepresentationMode asRepresentation() {
		return representation;
	}

	public String getExternalName() {
		return representation.getExternalName();
	}

	@Override
	public String toString() {
		return getExternalName();
	}

	/**
	 * Legacy-style entity-mode name parsing.  <b>Case insensitive</b>
	 *
	 * @param entityMode The entity mode name to evaluate
	 *
	 * @return The appropriate entity mode; {@code null} for incoming {@code entityMode} param is treated by returning
	 * {@link #POJO}.
	 */
	public static EntityMode parse(String entityMode) {
		return fromRepresentation( RepresentationMode.fromExternalName( entityMode ) );
	}

	public static EntityMode fromRepresentation(RepresentationMode representation) {
		if ( MAP.asRepresentation() == representation ) {
			return MAP;
		}

		return POJO;
	}
}
