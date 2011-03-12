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

import java.util.Set;
import java.io.IOException;

/**
 * @author Emmanuel Bernard
 */
public interface JarVisitor {
	/**
	 * Get the unqualified Jar name (ie wo path and wo extension)
	 *
	 * @return the unqualified jar name.
	 */
	String getUnqualifiedJarName();

	Filter[] getFilters();

	/**
	 * Return the matching entries for each filter in the same order the filter where passed
	 *
	 * @return array of Set of JarVisitor.Entry
	 * @throws java.io.IOException if something went wrong
	 */
	Set[] getMatchingEntries() throws IOException;
}
