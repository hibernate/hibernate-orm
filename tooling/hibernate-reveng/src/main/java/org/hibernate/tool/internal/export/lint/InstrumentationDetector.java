package org.hibernate.tool.internal.export.lint;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.cfg.Environment;
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
		BytecodeProvider bytecodeProvider = Environment.getBytecodeProvider();
		if(bytecodeProvider instanceof org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl ||
		   bytecodeProvider instanceof org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl) {
			enhanceEnabled = true;
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
			for (int i = 0; i < interfaces.length; i++) {
				Class<?> intface = interfaces[i];	
				if(intface.getName().equals(Managed.class.getName())) {
					enhanced = true;
				} 							
			}
			
			if (enhanceEnabled && !enhanced) {
				collector.reportIssue( new Issue("LAZY_NOT_INSTRUMENTED", Issue.HIGH_PRIORITY, "'" + clazz.getEntityName() + "' has lazy='false', but its class '" + mappedClass.getName() + "' has not been instrumented with javaassist") );
				return;
			} else {
				// unknown bytecodeprovider...can't really check for that.
			}
			
		}
	}

	@Override
	protected void visitProperty(
			PersistentClass clazz, 
			Property property,
			IssueCollector collector) {
	}
}
