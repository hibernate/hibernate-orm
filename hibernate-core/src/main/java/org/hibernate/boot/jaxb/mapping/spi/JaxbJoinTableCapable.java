package org.hibernate.boot.jaxb.mapping.spi;

/**
 * @author Steve Ebersole
 */
public interface JaxbJoinTableCapable {
	JaxbJoinTableImpl getJoinTable();
	void setJoinTable(JaxbJoinTableImpl value);
}
