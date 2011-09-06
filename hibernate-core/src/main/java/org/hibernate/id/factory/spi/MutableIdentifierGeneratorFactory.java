package org.hibernate.id.factory.spi;

import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.service.Service;

/**
 * Let people register strategies
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface MutableIdentifierGeneratorFactory extends IdentifierGeneratorFactory, Service {
	public void register(String strategy, Class generatorClass);
}
