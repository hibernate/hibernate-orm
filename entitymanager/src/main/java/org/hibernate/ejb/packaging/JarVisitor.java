// $Id:$
package org.hibernate.ejb.packaging;

import java.util.Set;
import java.io.IOException;

/**
 * @author Emmanuel Bernard
 */
public interface JarVisitor {
	/**
	 * Get the unqualified Jar name (ie wo path and wo extension)
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
