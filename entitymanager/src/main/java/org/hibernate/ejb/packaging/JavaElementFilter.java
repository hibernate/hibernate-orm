//$
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