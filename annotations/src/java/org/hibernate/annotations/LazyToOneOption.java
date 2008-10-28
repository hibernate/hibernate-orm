//$Id$
package org.hibernate.annotations;

/**
 * Lazy options available for a ToOne association
 *
 * @author Emmanuel Bernard
 */
public enum LazyToOneOption {
	/** eagerly load the association */
	FALSE,
	/**
	 * Lazy, give back a proxy which will be loaded when the state is requested
	 * This should be the prefered option
	 */
	PROXY,
	/** Lazy, give back the real object loaded when a reference is requested
	 * (Bytecode enhancement is mandatory for this option, fall back to PROXY
	 * if the class is not enhanced)
	 * This option should be avoided unless you can't afford the use of proxies
	 */
	NO_PROXY
}
