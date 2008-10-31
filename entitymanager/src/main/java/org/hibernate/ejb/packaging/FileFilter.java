//$
package org.hibernate.ejb.packaging;

/**
 * Filter use to match a file by its name
 *
 * @author Emmanuel Bernard
 */
public abstract class FileFilter extends Filter {

	/**
	 * @param retrieveStream Give back an open stream to the matching element or not
	 */
	public FileFilter(boolean retrieveStream) {
		super( retrieveStream );
	}

	/**
	 * Return true if the fully qualified file name match
	 */
	public abstract boolean accept(String name);
	}