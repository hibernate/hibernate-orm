/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@DiscriminatorValue("A320")
@Entity()
public class A320 extends Plane {
	private String javaEmbeddedVersion;

	public String getJavaEmbeddedVersion() {
		return javaEmbeddedVersion;
	}

	public void setJavaEmbeddedVersion(String string) {
		javaEmbeddedVersion = string;
	}

}
