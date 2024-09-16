/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.identity.joinedSubClass;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Andrey Vlasov
 * @author Steve Ebersole
 */
@Entity
@PrimaryKeyJoinColumn(name = "super_id")
public class Sub extends Super {
}
