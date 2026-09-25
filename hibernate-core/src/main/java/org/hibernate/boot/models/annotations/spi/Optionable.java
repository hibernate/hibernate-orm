package org.hibernate.boot.models.annotations.spi;

/**
 * @author Steve Ebersole
 */
public interface Optionable {
	String options();

	void options(String value);
}
