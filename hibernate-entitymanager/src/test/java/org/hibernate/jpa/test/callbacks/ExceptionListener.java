//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.PrePersist;

/**
 * @author Emmanuel Bernard
 */
public class ExceptionListener {
	@PrePersist
	public void raiseException(Object e) {
		throw new ArithmeticException( "1/0 impossible" );
	}
}
