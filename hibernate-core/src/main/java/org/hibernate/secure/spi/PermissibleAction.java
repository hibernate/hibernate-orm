/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
