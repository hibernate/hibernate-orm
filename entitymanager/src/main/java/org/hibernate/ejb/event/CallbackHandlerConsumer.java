//$Id$
package org.hibernate.ejb.event;

/**
 * @author Emmanuel Bernard
 */
public interface CallbackHandlerConsumer {
	void setCallbackHandler(EntityCallbackHandler callbackHandler);
}
