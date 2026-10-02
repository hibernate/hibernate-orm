package org.hibernate.boot.jaxb.mapping.spi;

/**
 * @author Steve Ebersole
 */
public interface JaxbMappedSuperclass extends JaxbEntityOrMappedSuperclass {
	@Override
	JaxbAttributesContainerImpl getAttributes();
}
