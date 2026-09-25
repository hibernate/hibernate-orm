package org.hibernate.id.enhanced;

/**
 * @author Gavin King
 */
public interface OptimizerDescriptor {
	boolean isPooled();
	String getExternalName();
	Class<? extends Optimizer> getOptimizerClass()
			throws ClassNotFoundException;
}
