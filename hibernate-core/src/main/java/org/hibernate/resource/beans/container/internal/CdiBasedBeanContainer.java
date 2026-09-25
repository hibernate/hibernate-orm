package org.hibernate.resource.beans.container.internal;

import jakarta.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.BeanContainer;

/**
 * @author Steve Ebersole
 */
public interface CdiBasedBeanContainer extends BeanContainer {
	BeanManager getUsableBeanManager();
}
