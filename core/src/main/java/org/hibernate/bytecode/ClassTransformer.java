//$Id: $
package org.hibernate.bytecode;

import java.security.ProtectionDomain;

/**
 * A persistence provider provides an instance of this interface
 * to the PersistenceUnitInfo.addTransformer method.
 * The supplied transformer instance will get called to transform
 * entity class files when they are loaded and redefined.  The transformation
 * occurs before the class is defined by the JVM
 *
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Emmanuel Bernard
 */
public interface ClassTransformer
{
   /**
	* Invoked when a class is being loaded or redefined to add hooks for persistence bytecode manipulation
	*
	* @param loader the defining class loaderof the class being transformed.  It may be null if using bootstrap loader
	* @param classname The name of the class being transformed
	* @param classBeingRedefined If an already loaded class is being redefined, then pass this as a parameter
	* @param protectionDomain ProtectionDomain of the class being (re)-defined
	* @param classfileBuffer The input byte buffer in class file format
	* @return A well-formed class file that can be loaded
	*/
   public byte[] transform(ClassLoader loader,
					String classname,
					Class classBeingRedefined,
					ProtectionDomain protectionDomain,
					byte[] classfileBuffer);
}
