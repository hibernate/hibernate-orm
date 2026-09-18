/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.source.registrationtargets;

/// Registration parity fixture.
/// @author Steve Ebersole
@org.hibernate.annotations.FilterDef(name = "first", defaultCondition = "1=1")
@org.hibernate.annotations.FilterDef(name = "second", defaultCondition = "2=2")
@org.hibernate.annotations.NamedQuery(name = "queryOne", query = "from Target")
@org.hibernate.annotations.NamedQuery(name = "queryTwo", query = "from Target where id = 1")
public class Target {
}
