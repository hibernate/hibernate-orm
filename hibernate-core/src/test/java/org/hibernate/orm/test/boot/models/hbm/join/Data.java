/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.join;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class Data {
	private String first;
	private String second;
}
