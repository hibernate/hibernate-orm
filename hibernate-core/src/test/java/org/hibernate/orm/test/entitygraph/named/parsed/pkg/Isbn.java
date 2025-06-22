/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.pkg;

import jakarta.persistence.Embeddable;

@Embeddable
public class Isbn {
	private String prefix;
	private String element;
	private String registrationGroup;
	private String registrant;
	private String publication;
	private String checkDigit;
}
