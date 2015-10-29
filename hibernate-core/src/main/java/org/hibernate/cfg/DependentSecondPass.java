package org.hibernate.cfg;

/**
 * @author Matt Drees
 */
public interface DependentSecondPass extends SecondPass {

	boolean dependentUpon(SecondPass secondPass);
}
