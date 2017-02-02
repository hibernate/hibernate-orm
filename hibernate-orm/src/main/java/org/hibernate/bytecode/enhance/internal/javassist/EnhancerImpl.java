/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import javassist.NotFoundException;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

public class EnhancerImpl implements Enhancer {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( Enhancer.class );

	protected final JavassistEnhancementContext enhancementContext;
	private final ClassPool classPool;

	/**
	 * Constructs the Enhancer, using the given context.
	 *
	 * @param enhancementContext Describes the context in which enhancement will occur so as to give access
	 * to contextual/environmental information.
	 */
	public EnhancerImpl(EnhancementContext enhancementContext) {
		this.enhancementContext = new JavassistEnhancementContext( enhancementContext );
		this.classPool = buildClassPool( this.enhancementContext );
	}

	EnhancerImpl(JavassistEnhancementContext enhancementContext) {
		this.enhancementContext = enhancementContext;
		this.classPool = buildClassPool( enhancementContext );
	}

	/**
	 * Performs the enhancement.
	 *
	 * @param className The name of the class whose bytecode is being enhanced.
	 * @param originalBytes The class's original (pre-enhancement) byte code
	 *
	 * @return The enhanced bytecode. Could be the same as the original bytecode if the original was
	 * already enhanced or we could not enhance it for some reason.
	 *
	 * @throws EnhancementException Indicates a problem performing the enhancement
	 */
	@Override
	public synchronized byte[] enhance(String className, byte[] originalBytes) throws EnhancementException {
		CtClass managedCtClass = null;
		try {
			managedCtClass = classPool.makeClassIfNew( new ByteArrayInputStream( originalBytes ) );
			return enhance( managedCtClass ) ? getByteCode( managedCtClass ) : null;
		}
		catch (IOException e) {
			log.unableToBuildEnhancementMetamodel( className );
			return null;
		}
		finally {
			if ( managedCtClass != null ) {
				managedCtClass.detach();
			}
		}
	}

	private ClassPool buildClassPool(JavassistEnhancementContext enhancementContext) {
		ClassPool classPool = new ClassPool( false ) {
			@Override
			public ClassLoader getClassLoader() {
				return enhancementContext.getLoadingClassLoader();
			}
		};

		ClassLoader loadingClassLoader = enhancementContext.getLoadingClassLoader();
		if ( loadingClassLoader != null ) {
			classPool.appendClassPath( new LoaderClassPath( loadingClassLoader ) );
		}
		return classPool;
	}

	protected CtClass loadCtClassFromClass(Class<?> aClass) {
		String resourceName = aClass.getName().replace( '.', '/' ) + ".class";
		InputStream resourceAsStream = aClass.getClassLoader().getResourceAsStream( resourceName );
		if ( resourceAsStream == null ) {
			throw new UncheckedIOException( new FileNotFoundException ( "Not found: " + resourceName ) );
		}
		try {
			return classPool.makeClass( resourceAsStream );
		}
		catch (IOException e) {
			throw new EnhancementException( "Could not prepare Javassist ClassPool", e );
		}
		finally {
			try {
				resourceAsStream.close();
			}
			catch (IOException ioe) {
				log.debugf( "An error occurs closing InputStream for class [%s]", aClass.getName() );
			}
		}
	}

	private boolean enhance(CtClass managedCtClass) {
		// can't effectively enhance interfaces
		if ( managedCtClass.isInterface() ) {
			log.debugf( "Skipping enhancement of [%s]: it's an interface!", managedCtClass.getName() );
			return false;
		}
		// skip already enhanced classes
		if ( alreadyEnhanced( managedCtClass ) ) {
			log.debugf( "Skipping enhancement of [%s]: already enhanced", managedCtClass.getName() );
			return false;
		}

		if ( enhancementContext.isEntityClass( managedCtClass ) ) {
			log.infof( "Enhancing [%s] as Entity", managedCtClass.getName() );
			new EntityEnhancer( enhancementContext ).enhance( managedCtClass );
			return true;
		}
		else if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
			log.infof( "Enhancing [%s] as Composite", managedCtClass.getName() );
			new CompositeEnhancer( enhancementContext ).enhance( managedCtClass );
			return true;
		}
		else if ( enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			log.infof( "Enhancing [%s] as MappedSuperclass", managedCtClass.getName() );
			new MappedSuperclassEnhancer( enhancementContext ).enhance( managedCtClass );
			return true;
		}
		else if ( enhancementContext.doExtendedEnhancement( managedCtClass ) ) {
			log.infof( "Extended enhancement of [%s]", managedCtClass.getName() );
			new PersistentAttributesEnhancer( enhancementContext ).extendedEnhancement( managedCtClass );
			return true;
		}
		else {
			log.debugf( "Skipping enhancement of [%s]: not entity or composite", managedCtClass.getName() );
			return false;
		}
	}

	// See HHH-10977 HHH-11284 HHH-11404 --- check for declaration of Managed interface on the class, not inherited
	private boolean alreadyEnhanced(CtClass managedCtClass) {
		try {
			for ( CtClass declaredInterface : managedCtClass.getInterfaces() ) {
				if ( PersistentAttributesHelper.isAssignable( declaredInterface, Managed.class.getName() ) ) {
					return true;
				}
			}
			return false;
		}
		catch ( NotFoundException e ) {
			throw new HibernateException( "Unable to transform class: " + e.getMessage() , e );
		}
	}

	private byte[] getByteCode(CtClass managedCtClass) {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream( byteStream );
		try {
			managedCtClass.toBytecode( out );
			return byteStream.toByteArray();
		}
		catch (Exception e) {
			log.unableToTransformClass( e.getMessage() );
			throw new HibernateException( "Unable to transform class: " + e.getMessage() , e );
		}
		finally {
			try {
				out.close();
			}
			catch (IOException ignored) {
			}
		}
	}

	protected void addInterceptorHandling(CtClass managedCtClass) {
		// interceptor handling is only needed if class has lazy-loadable attributes
		if ( !enhancementContext.hasLazyLoadableAttributes( managedCtClass ) ) {
			return;
		}
		log.debugf( "Weaving in PersistentAttributeInterceptable implementation on [%s]", managedCtClass.getName() );

		managedCtClass.addInterface( loadCtClassFromClass( PersistentAttributeInterceptable.class ) );

		FieldWriter.addFieldWithGetterAndSetter(
				managedCtClass,
				loadCtClassFromClass( PersistentAttributeInterceptor.class ),
				EnhancerConstants.INTERCEPTOR_FIELD_NAME,
				EnhancerConstants.INTERCEPTOR_GETTER_NAME,
				EnhancerConstants.INTERCEPTOR_SETTER_NAME
		);
	}
}
