/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
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

import java.lang.reflect.Method;

import org.hibernate.tool.reveng.ide.completion.HQLCompletionProposal;
import org.hibernate.tool.reveng.ide.completion.IHQLCompletionRequestor;

public class HqlCompletionRequestor implements IHQLCompletionRequestor {
	
	private Object handler = null;
	
	public HqlCompletionRequestor(Object handler) {
		this.handler = handler;
	}

	@Override
	public boolean accept(HQLCompletionProposal proposal) {
		try {
			Method m = handler
					.getClass()
					.getMethod("accept", new Class[] { Object.class });
			m.setAccessible(true);
			return (boolean)m.invoke(handler, proposal);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	@Override
	public void completionFailure(String errorMessage) {
		try {
			Method m = handler
					.getClass()
					.getMethod("completionFailure", new Class[] { String.class });
			m.setAccessible(true);
			m.invoke(handler, errorMessage);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

}
