package org.hibernate.boot.jaxb.mapping.spi;

/**
 * Mapping of a discriminator value to the corresponding entity-name
 *
 * @author Steve Ebersole
 */
public interface JaxbDiscriminatorMapping {
	String getDiscriminatorValue();
	String getCorrespondingEntityName();
}
