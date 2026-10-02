package org.hibernate.processor.test.mixedmode;

import jakarta.persistence.Embeddable;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class Coordinates {
	public float longitude;
	public float latitude;
}
