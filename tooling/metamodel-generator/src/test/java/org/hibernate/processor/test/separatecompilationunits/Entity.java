package org.hibernate.processor.test.separatecompilationunits;

import org.hibernate.processor.test.separatecompilationunits.superclass.MappedSuperclass;

/**
 * @author Hardy Ferentschik
 */
@jakarta.persistence.Entity
public class Entity extends MappedSuperclass {
	private String name;
}
