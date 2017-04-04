/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.spi;

/**
 * @author Steve Ebersole
 */
public enum PermissibleAction {
	INSERT( "insert" ),
	UPDATE( "update" ),
	DELETE( "delete" ),
	READ( "read" ),
	ANY( "*" ) {
		@Override
		public String[] getImpliedActions() {
			return new String[] { INSERT.externalName, UPDATE.externalName, DELETE.externalName, READ.externalName };
		}
	};

	private final String externalName;
	private final String[] impliedActions;

	private PermissibleAction(String externalName) {
		this.externalName = externalName;
		this.impliedActions = buildImpliedActions( externalName );
	}

	private String[] buildImpliedActions(String externalName) {
		return new String[] { externalName };
	}

	public String getExternalName() {
		return externalName;
	}

	public String[] getImpliedActions() {
		return impliedActions;
	}

	public static PermissibleAction interpret(String action) {
		if ( INSERT.externalName.equals( action ) ) {
			return INSERT;
		}
		else if ( UPDATE.externalName.equals( action ) ) {
			return UPDATE;
		}
		else if ( DELETE.externalName.equals( action ) ) {
			return DELETE;
		}
		else if ( READ.externalName.equals( action ) ) {
			return READ;
		}
		else if ( ANY.externalName.equals( action ) ) {
			return ANY;
		}

		throw new IllegalArgumentException( "Unrecognized action : " + action );
	}
}
