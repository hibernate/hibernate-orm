package org.hibernate.boot.jaxb.mapping.spi;

/**
 * @author Steve Ebersole
 */
public interface JaxbCascadableAttribute extends JaxbPersistentAttribute {
	JaxbCascadeTypeImpl getCascade();
	void setCascade(JaxbCascadeTypeImpl value);
}
