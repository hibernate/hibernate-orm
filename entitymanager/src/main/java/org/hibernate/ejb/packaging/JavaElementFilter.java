/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.ejb.packaging;

/**
 * Filter a Java element (class or package per fully qualified name and annotation existence)
 * At least 1 annotation has to annotate the element and the accept method must match
 * If none annotations are passed, only the accept method must pass.
 *
 * @author Emmanuel Bernard
 */
public abstract class JavaElementFilter extends Filter {
	private Class[] annotations;

	/**
	 * @param retrieveStream Give back an open stream to the matching element or not
	 * @param annotations	Array of annotations that must be present to match (1 of them should annotate the element
	 */
	protected JavaElementFilter(boolean retrieveStream, Class[] annotations) {
		super( retrieveStream );
		this.annotations = annotations == null ? new Class[]{} : annotations;
	}

	public Class[] getAnnotations() {
		return annotations;
	}

	/**
	 * Return true if the fully qualified name match
	 */
	public abstract boolean accept(String javaElementName);
}