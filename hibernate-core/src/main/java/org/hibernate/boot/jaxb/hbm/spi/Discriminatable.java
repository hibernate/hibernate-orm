package org.hibernate.boot.jaxb.hbm.spi;

/**
 * @author Steve Ebersole
 */
public interface Discriminatable {
	String getDiscriminatorValue();
}
