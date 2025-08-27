/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.entity;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class Isbn {
	private String prefix;
	private String element;
	private String registrationGroup;
	private String registrant;
	private String publication;
	private String checkDigit;
}
