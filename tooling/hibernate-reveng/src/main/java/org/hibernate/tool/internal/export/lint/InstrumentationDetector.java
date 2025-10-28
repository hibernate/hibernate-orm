/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.export.lint;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.engine.spi.Managed;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

public class InstrumentationDetector extends EntityModelDetector {
	
	public String getName() {
		return "instrument";
	}

	private boolean enhanceEnabled;
	
	public void initialize(Metadata metadata) {
		super.initialize(metadata);	
		if (metadata instanceof MetadataImplementor) {
			final BytecodeProvider bytecodeProvider =
					((MetadataImplementor)metadata).getMetadataBuildingOptions().getServiceRegistry()
							.getService( BytecodeProvider.class );
			if(bytecodeProvider != null 
					&& !(bytecodeProvider instanceof org.hibernate.bytecode.internal.none.BytecodeProviderImpl)) {
				enhanceEnabled = true;
			}
		}
	}
	
	protected void visit(PersistentClass clazz, IssueCollector collector) {
		Class<?> mappedClass;	
		try {
			mappedClass = clazz.getMappedClass();
		} catch(MappingException me) {
			// ignore
			return;
		}

		if(clazz.isLazy()) {
			try {
				mappedClass.getConstructor( new Class[0] );
			}
			catch (SecurityException e) {
				// ignore
			}
			catch (NoSuchMethodException e) {
				collector.reportIssue(new Issue("LAZY_NO_DEFAULT_CONSTRUCTOR",Issue.NORMAL_PRIORITY, "lazy='true' set for '" + clazz.getEntityName() +"', but class has no default constructor." ));
				return;
			}

		} else if(enhanceEnabled){
			Class<?>[] interfaces = mappedClass.getInterfaces();
			boolean enhanced = false;
            for (Class<?> intface : interfaces) {
                if (intface.getName().equals(Managed.class.getName())) {
                    enhanced = true;
                    break;
                }
            }
			
			if (!enhanced) {
				collector.reportIssue( new Issue("LAZY_NOT_INSTRUMENTED", Issue.HIGH_PRIORITY, "'" + clazz.getEntityName() + "' has lazy='false', but its class '" + mappedClass.getName() + "' has not been instrumented with javaassist") );
				return;
			}
			
		}
	}

	@Override
	protected void visitProperty(
			Property property,
			IssueCollector collector) {
	}
}
