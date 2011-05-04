/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.secure.internal;

import java.security.Permission;

/**
 * @author Gavin King
 */
public class HibernatePermission extends Permission {
	public static final String INSERT = "insert";
	public static final String UPDATE = "update";
	public static final String DELETE = "delete";
	public static final String READ = "read";
	public static final String ANY = "*";
	
	private final String actions;

	public HibernatePermission(String entityName, String actions) {
		super(entityName);
		this.actions = actions;
	}

	public boolean implies(Permission permission) {
		//TODO!
		return ( "*".equals( getName() ) || getName().equals( permission.getName() ) ) &&
			( "*".equals(actions) || actions.indexOf( permission.getActions() ) >= 0 );
	}

	public boolean equals(Object obj) {
		if ( !(obj instanceof HibernatePermission) ) return false;
		HibernatePermission permission = (HibernatePermission) obj;
		return permission.getName().equals( getName() ) && 
			permission.getActions().equals(actions);
	}

	public int hashCode() {
		return getName().hashCode() * 37 + actions.hashCode();
	}

	public String getActions() {
		return actions;
	}
	
	public String toString() {
		return "HibernatePermission(" + getName() + ':' + actions + ')';
	}

}
