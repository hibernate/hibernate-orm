package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface JaxbQueryHintContainer {
	String getName();
	String getDescription();
	List<? extends JaxbQueryHint> getHints();
}
