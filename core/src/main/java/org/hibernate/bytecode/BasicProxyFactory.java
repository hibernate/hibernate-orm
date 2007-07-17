package org.hibernate.bytecode;

/**
 * A proxy factory for "basic proxy" generation
 *
 * @author Steve Ebersole
 */
public interface BasicProxyFactory {
	public Object getProxy();
}
