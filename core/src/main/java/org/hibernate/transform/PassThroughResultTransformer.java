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
package org.hibernate.transform;

import java.io.Serializable;

/**
 * ???
 *
 * @author max
 */
public class PassThroughResultTransformer extends BasicTransformerAdapter implements Serializable {

	public static final PassThroughResultTransformer INSTANCE = new PassThroughResultTransformer();

	/**
	 * Instamtiate a PassThroughResultTransformer.
	 *
	 * @deprecated Use the {@link #INSTANCE} reference instead of explicitly creating a new one (to be removed in 3.4).
	 */
	public PassThroughResultTransformer() {
	}

	public Object transformTuple(Object[] tuple, String[] aliases) {
		return tuple.length==1 ? tuple[0] : tuple;
	}

	/**
	 * Serialization hook for ensuring singleton uniqueing.
	 *
	 * @return The singleton instance : {@link #INSTANCE}
	 */
	private Object readResolve() {
		return INSTANCE;
	}

	public int hashCode() {
		// todo : we can remove this once the deprecated ctor can be made private...
		return PassThroughResultTransformer.class.getName().hashCode();
	}

	public boolean equals(Object other) {
		// todo : we can remove this once the deprecated ctor can be made private...
		return other != null && PassThroughResultTransformer.class.isInstance( other );
	}

}
