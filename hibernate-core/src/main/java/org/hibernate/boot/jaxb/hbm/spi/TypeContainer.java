package org.hibernate.boot.jaxb.hbm.spi;

/**
 * @author Steve Ebersole
 */
public interface TypeContainer {
	String getTypeAttribute();
	JaxbHbmTypeSpecificationType getType();
}
