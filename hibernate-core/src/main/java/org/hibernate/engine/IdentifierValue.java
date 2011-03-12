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
package org.hibernate.engine;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A strategy for determining if an identifier value is an identifier of
 * a new transient instance or a previously persistent transient instance.
 * The strategy is determined by the <tt>unsaved-value</tt> attribute in
 * the mapping file.
 * 
 * @author Gavin King
 */
public class IdentifierValue {

	private static final Logger log = LoggerFactory.getLogger(IdentifierValue.class);
	
	private final Serializable value;
	
	/**
	 * Always assume the transient instance is newly instantiated
	 */
	public static final IdentifierValue ANY = new IdentifierValue() {
		public final Boolean isUnsaved(Serializable id) {
			log.trace("id unsaved-value strategy ANY");
			return Boolean.TRUE;
		}
		public Serializable getDefaultValue(Serializable currentValue) {
			return currentValue;
		}
		public String toString() {
			return "SAVE_ANY";
		}
	};
	
	/**
	 * Never assume the transient instance is newly instantiated
	 */
	public static final IdentifierValue NONE = new IdentifierValue() {
		public final Boolean isUnsaved(Serializable id) {
			log.trace("id unsaved-value strategy NONE");
			return Boolean.FALSE;
		}
		public Serializable getDefaultValue(Serializable currentValue) {
			return currentValue;
		}
		public String toString() {
			return "SAVE_NONE";
		}
	};
	
	/**
	 * Assume the transient instance is newly instantiated if the identifier
	 * is null.
	 */
	public static final IdentifierValue NULL = new IdentifierValue() {
		public final Boolean isUnsaved(Serializable id) {
			log.trace("id unsaved-value strategy NULL");
			return id==null ? Boolean.TRUE : Boolean.FALSE;
		}
		public Serializable getDefaultValue(Serializable currentValue) {
			return null;
		}
		public String toString() {
			return "SAVE_NULL";
		}
	};
	
	/**
	 * Assume nothing.
	 */
	public static final IdentifierValue UNDEFINED = new IdentifierValue() {
		public final Boolean isUnsaved(Serializable id) {
			log.trace("id unsaved-value strategy UNDEFINED");
			return null;
		}
		public Serializable getDefaultValue(Serializable currentValue) {
			return null;
		}
		public String toString() {
			return "UNDEFINED";
		}
	};
	
	protected IdentifierValue() {
		this.value = null;
	}
	
	/**
	 * Assume the transient instance is newly instantiated if
	 * its identifier is null or equal to <tt>value</tt>
	 */
	public IdentifierValue(Serializable value) {
		this.value = value;
	}
	
	/**
	 * Does the given identifier belong to a new instance?
	 */
	public Boolean isUnsaved(Serializable id) {
		if ( log.isTraceEnabled() ) log.trace("id unsaved-value: " + value);
		return id==null || id.equals(value) ? Boolean.TRUE : Boolean.FALSE;
	}
	
	public Serializable getDefaultValue(Serializable currentValue) {
		return value;
	}
	
	public String toString() {
		return "identifier unsaved-value: " + value;
	}
}