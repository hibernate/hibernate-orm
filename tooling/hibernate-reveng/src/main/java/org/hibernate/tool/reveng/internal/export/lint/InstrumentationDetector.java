/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

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
		}
		catch(MappingException me) {
			// ignore
			return;
		}

		if(clazz.isLazy()) {
			try {
				mappedClass.getConstructor();
			}
			catch (SecurityException e) {
				// ignore
			}
			catch (NoSuchMethodException e) {
				collector.reportIssue(new Issue("LAZY_NO_DEFAULT_CONSTRUCTOR",Issue.NORMAL_PRIORITY, "lazy='true' set for '" + clazz.getEntityName() +"', but class has no default constructor." ));
			}

		}
		else if(enhanceEnabled){
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
			}

		}
	}

	@Override
	protected void visitProperty(
			Property property,
			IssueCollector collector) {
	}
}
