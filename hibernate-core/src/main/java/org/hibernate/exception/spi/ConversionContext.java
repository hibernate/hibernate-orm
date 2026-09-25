package org.hibernate.exception.spi;

/**
 * @author Steve Ebersole
 */
public interface ConversionContext {
	ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor();
}
