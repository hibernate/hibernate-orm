package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface JaxbBaseAttributesContainer {
	List<JaxbBasicImpl> getBasicAttributes();

	List<JaxbEmbeddedImpl> getEmbeddedAttributes();

	List<JaxbManyToOneImpl> getManyToOneAttributes();

	List<JaxbAnyMappingImpl> getAnyMappingAttributes();
}
