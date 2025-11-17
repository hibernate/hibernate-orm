/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.pack.defaultpar_1_0;
import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Version1 {
	private static final String DOT = ".";
	private int major;
	private int minor;
	private int micro;

	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}

	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public int getMicro() {
		return micro;
	}

	public void setMicro(int micro) {
		this.micro = micro;
	}

	public String toString() {
		return new StringBuilder( major ).append( DOT ).append( minor ).append( DOT ).append( micro ).toString();
	}
}
