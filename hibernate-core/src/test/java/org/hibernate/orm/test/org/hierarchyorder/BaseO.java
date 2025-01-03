/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.org.hierarchyorder;


/*
 * Created on 03/12/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class BaseO {
@Id
@GeneratedValue
private Integer id;

public Integer getId() {
	return id;
}
}
