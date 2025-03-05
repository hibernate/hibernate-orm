/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal;

import org.hibernate.boot.spi.SecondPass;

/**
 * Because {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy} often requires
 * access info from PersistentClass, we sometimes need to wait until the proper PersistentClass
 * is bound to the in-flight metadata, which means a SecondPass (in this version still using
 * second passes).
 *
 * @author Steve Ebersole
 */
public interface ImplicitColumnNamingSecondPass extends SecondPass {
}
