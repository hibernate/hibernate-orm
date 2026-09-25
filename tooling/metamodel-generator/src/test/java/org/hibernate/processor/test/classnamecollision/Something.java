package org.hibernate.processor.test.classnamecollision;

import jakarta.persistence.Entity;

@Entity
public class Something extends org.hibernate.processor.test.classnamecollision.somewhere.Something {
	String alphaValue;
}
