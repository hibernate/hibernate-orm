/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2022-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.orm.jbt.internal.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;

public class DummyMetadataDescriptor implements MetadataDescriptor {

	private static final MetadataInvocationHandler HANDLER = new MetadataInvocationHandler();	
	private static final Class<?>[] INTERFACES = new Class[] { Metadata.class };
	private static final ClassLoader LOADER = Metadata.class.getClassLoader();

	@Override
	public Metadata createMetadata() {
		return (Metadata)Proxy.newProxyInstance(LOADER, INTERFACES, HANDLER);
	}

	@Override
	public Properties getProperties() {
		return null;
	}

	private static class MetadataInvocationHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ("getEntityBindings".equals(method.getName()) || 
					"collectTableMappings".equals(method.getName())) {
				return Collections.emptySet();
			} else {
				return null;
			}
		}		
	}

}
