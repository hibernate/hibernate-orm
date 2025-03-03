/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.concrete;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.runner.RunWith;

/**
 * Version of {@link AbstractConcreteProxyTest} using bytecode-enhanced {@linkplain org.hibernate.proxy.HibernateProxy proxies}.
 *
 * @author Marco Belladelli
 */
@RunWith( BytecodeEnhancerRunner.class )
public class BytecodeConcreteProxyTest extends AbstractConcreteProxyTest {
}
