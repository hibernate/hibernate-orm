/*
 * SPDX-License-Identifier: Apache-2.0
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
