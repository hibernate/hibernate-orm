package org.hibernate.boot.jaxb.hbm.spi;

/**
 * @author Steve Ebersole
 */
public interface SimpleValueTypeInfo {
	String getTypeAttribute();
	JaxbHbmTypeSpecificationType getType();
}
