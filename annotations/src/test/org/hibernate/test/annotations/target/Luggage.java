//$Id$
package org.hibernate.test.annotations.target;

/**
 * @author Emmanuel Bernard
 */
public interface Luggage {
	double getHeight();
	double getWidth();

	void setHeight(double height);
	void setWidth(double width);

	Owner getOwner();

	void setOwner(Owner owner);
}
