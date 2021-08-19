/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.StackMapTable;
import javassist.util.proxy.FactoryHelper;
import javassist.util.proxy.RuntimeSupport;

/**
 * A factory of bulk accessors.
 *
 * @author Muga Nishizawa
 * @author modified by Shigeru Chiba
 */
class BulkAccessorFactory {
	private static final String PACKAGE_NAME_PREFIX = "org.javassist.tmp.";
	private static final String BULKACESSOR_CLASS_NAME = BulkAccessor.class.getName();
	private static final String OBJECT_CLASS_NAME = Object.class.getName();
	private static final String GENERATED_GETTER_NAME = "getPropertyValues";
	private static final String GENERATED_SETTER_NAME = "setPropertyValues";
	private static final String GET_SETTER_DESC = "(Ljava/lang/Object;[Ljava/lang/Object;)V";
	private static final String THROWABLE_CLASS_NAME = Throwable.class.getName();
	private static final String BULKEXCEPTION_CLASS_NAME = BulkAccessorException.class.getName();

	private static int counter;

	private Class targetBean;
	private String[] getterNames;
	private String[] setterNames;
	private Class[] types;
	public String writeDirectory;

	BulkAccessorFactory(
			Class target,
			String[] getterNames,
			String[] setterNames,
			Class[] types) {
		this.targetBean = target;
		this.getterNames = getterNames;
		this.setterNames = setterNames;
		this.types = types;
		this.writeDirectory = null;
	}

	BulkAccessor create() {
		final Method[] getters = new Method[getterNames.length];
		final Method[] setters = new Method[setterNames.length];
		findAccessors( targetBean, getterNames, setterNames, types, getters, setters );

		final Class beanClass;
		try {
			final ClassFile classfile = make( getters, setters );
			final ClassLoader loader = this.getClassLoader();
			if ( writeDirectory != null ) {
				FactoryHelper.writeFile( classfile, writeDirectory );
			}

			beanClass = FactoryHelper.toClass( classfile, null, loader, getDomain() );
			return (BulkAccessor) this.newInstance( beanClass );
		}
		catch ( Exception e ) {
			throw new BulkAccessorException( e.getMessage(), e );
		}
	}

	private ProtectionDomain getDomain() {
		final Class cl;
		if ( this.targetBean != null ) {
			cl = this.targetBean;
		}
		else {
			cl = this.getClass();
		}
		return cl.getProtectionDomain();
	}

	private ClassFile make(Method[] getters, Method[] setters) throws CannotCompileException {
		String className = targetBean.getName();
		// set the name of bulk accessor.
		className = className + "_$$_bulkaccess_" + counter++;
		if ( className.startsWith( "java." ) ) {
			className = PACKAGE_NAME_PREFIX + className;
		}

		final ClassFile classfile = new ClassFile( false, className, BULKACESSOR_CLASS_NAME );
		classfile.setAccessFlags( AccessFlag.PUBLIC );
		addDefaultConstructor( classfile );
		addGetter( classfile, getters );
		addSetter( classfile, setters );
		return classfile;
	}

	private ClassLoader getClassLoader() {
		if ( targetBean != null && targetBean.getName().equals( OBJECT_CLASS_NAME ) ) {
			return targetBean.getClassLoader();
		}
		else {
			return getClass().getClassLoader();
		}
	}

	private Object newInstance(Class type) throws Exception {
		final BulkAccessor instance = (BulkAccessor) type.newInstance();
		instance.target = targetBean;
		final int len = getterNames.length;
		instance.getters = new String[len];
		instance.setters = new String[len];
		instance.types = new Class[len];
		for ( int i = 0; i < len; i++ ) {
			instance.getters[i] = getterNames[i];
			instance.setters[i] = setterNames[i];
			instance.types[i] = types[i];
		}

		return instance;
	}

	/**
	 * Declares a constructor that takes no parameter.
	 *
	 * @param classfile The class descriptor
	 *
	 * @throws CannotCompileException Indicates trouble with the underlying Javassist calls
	 */
	private void addDefaultConstructor(ClassFile classfile) throws CannotCompileException {
		final ConstPool constPool = classfile.getConstPool();
		final String constructorSignature = "()V";
		final MethodInfo constructorMethodInfo = new MethodInfo( constPool, MethodInfo.nameInit, constructorSignature );

		final Bytecode code = new Bytecode( constPool, 0, 1 );
		// aload_0
		code.addAload( 0 );
		// invokespecial
		code.addInvokespecial( BulkAccessor.class.getName(), MethodInfo.nameInit, constructorSignature );
		// return
		code.addOpcode( Opcode.RETURN );

		constructorMethodInfo.setCodeAttribute( code.toCodeAttribute() );
		constructorMethodInfo.setAccessFlags( AccessFlag.PUBLIC );
		classfile.addMethod( constructorMethodInfo );
	}

	private void addGetter(ClassFile classfile, final Method[] getters) throws CannotCompileException {
		final ConstPool constPool = classfile.getConstPool();
		final int targetBeanConstPoolIndex = constPool.addClassInfo( this.targetBean.getName() );
		final String desc = GET_SETTER_DESC;
		final MethodInfo getterMethodInfo = new MethodInfo( constPool, GENERATED_GETTER_NAME, desc );

		final Bytecode code = new Bytecode( constPool, 6, 4 );
		/* | this | bean | args | raw bean | */
		if ( getters.length >= 0 ) {
			// aload_1 // load bean
			code.addAload( 1 );
			// checkcast // cast bean
			code.addCheckcast( this.targetBean.getName() );
			// astore_3 // store bean
			code.addAstore( 3 );
			for ( int i = 0; i < getters.length; ++i ) {
				if ( getters[i] != null ) {
					final Method getter = getters[i];
					// aload_2 // args
					code.addAload( 2 );
					// iconst_i // continue to aastore
					// growing stack is 1
					code.addIconst( i );
					final Class returnType = getter.getReturnType();
					int typeIndex = -1;
					if ( returnType.isPrimitive() ) {
						typeIndex = FactoryHelper.typeIndex( returnType );
						// new
						code.addNew( FactoryHelper.wrapperTypes[typeIndex] );
						// dup
						code.addOpcode( Opcode.DUP );
					}

					// aload_3 // load the raw bean
					code.addAload( 3 );
					final String getterSignature = RuntimeSupport.makeDescriptor( getter );
					final String getterName = getter.getName();
					if ( this.targetBean.isInterface() ) {
						// invokeinterface
						code.addInvokeinterface( targetBeanConstPoolIndex, getterName, getterSignature, 1 );
					}
					else {
						// invokevirtual
						code.addInvokevirtual( targetBeanConstPoolIndex, getterName, getterSignature );
					}

					if ( typeIndex >= 0 ) {
						// is a primitive type
						// invokespecial
						code.addInvokespecial(
								FactoryHelper.wrapperTypes[typeIndex],
								MethodInfo.nameInit,
								FactoryHelper.wrapperDesc[typeIndex]
						);
					}

					// aastore // args
					code.add( Opcode.AASTORE );
					code.growStack( -3 );
				}
			}
		}
		// return
		code.addOpcode( Opcode.RETURN );

		getterMethodInfo.setCodeAttribute( code.toCodeAttribute() );
		getterMethodInfo.setAccessFlags( AccessFlag.PUBLIC );
		classfile.addMethod( getterMethodInfo );
	}

	private void addSetter(ClassFile classfile, final Method[] setters) throws CannotCompileException {
		final ConstPool constPool = classfile.getConstPool();
		final int targetTypeConstPoolIndex = constPool.addClassInfo( this.targetBean.getName() );
		final String desc = GET_SETTER_DESC;
		final MethodInfo setterMethodInfo = new MethodInfo( constPool, GENERATED_SETTER_NAME, desc );

		final Bytecode code = new Bytecode( constPool, 4, 6 );
		StackMapTable stackmap = null;
		/* | this | bean | args | i | raw bean | exception | */
		if ( setters.length > 0 ) {
			// required to exception table
			int start;
			int end;
			// iconst_0 // i
			code.addIconst( 0 );
			// istore_3 // store i
			code.addIstore( 3 );
			// aload_1 // load the bean
			code.addAload( 1 );
			// checkcast // cast the bean into a raw bean
			code.addCheckcast( this.targetBean.getName() );
			// astore 4 // store the raw bean
			code.addAstore( 4 );
			/* current stack len = 0 */
			// start region to handling exception (BulkAccessorException)
			start = code.currentPc();
			int lastIndex = 0;
			for ( int i = 0; i < setters.length; ++i ) {
				if ( setters[i] != null ) {
					final int diff = i - lastIndex;
					if ( diff > 0 ) {
						// iinc 3, 1
						code.addOpcode( Opcode.IINC );
						code.add( 3 );
						code.add( diff );
						lastIndex = i;
					}
				}
				/* current stack len = 0 */
				// aload 4 // load the raw bean
				code.addAload( 4 );
				// aload_2 // load the args
				code.addAload( 2 );
				// iconst_i
				code.addIconst( i );
				// aaload
				code.addOpcode( Opcode.AALOAD );
				// checkcast
				final Class[] setterParamTypes = setters[i].getParameterTypes();
				final Class setterParamType = setterParamTypes[0];
				if ( setterParamType.isPrimitive() ) {
					// checkcast (case of primitive type)
					// invokevirtual (case of primitive type)
					this.addUnwrapper( code, setterParamType );
				}
				else {
					// checkcast (case of reference type)
					code.addCheckcast( setterParamType.getName() );
				}
				/* current stack len = 2 */
				final String rawSetterMethodDesc = RuntimeSupport.makeDescriptor( setters[i] );
				if ( !this.targetBean.isInterface() ) {
					// invokevirtual
					code.addInvokevirtual( targetTypeConstPoolIndex, setters[i].getName(), rawSetterMethodDesc );
				}
				else {
					// invokeinterface
					final Class[] params = setters[i].getParameterTypes();
					int size;
					if ( params[0].equals( Double.TYPE ) || params[0].equals( Long.TYPE ) ) {
						size = 3;
					}
					else {
						size = 2;
					}

					code.addInvokeinterface( targetTypeConstPoolIndex, setters[i].getName(), rawSetterMethodDesc, size );
				}
			}

			// end region to handling exception (BulkAccessorException)
			end = code.currentPc();
			// return
			code.addOpcode( Opcode.RETURN );
			/* current stack len = 0 */
			// register in exception table
			final int throwableTypeIndex = constPool.addClassInfo( THROWABLE_CLASS_NAME );
			final int handlerPc = code.currentPc();
			code.addExceptionHandler( start, end, handlerPc, throwableTypeIndex );
			// astore 5 // store exception
			code.addAstore( 5 );
			// new // BulkAccessorException
			code.addNew( BULKEXCEPTION_CLASS_NAME );
			// dup
			code.addOpcode( Opcode.DUP );
			// aload 5 // load exception
			code.addAload( 5 );
			// iload_3 // i
			code.addIload( 3 );
			// invokespecial // BulkAccessorException.<init>
			final String consDesc = "(Ljava/lang/Throwable;I)V";
			code.addInvokespecial( BULKEXCEPTION_CLASS_NAME, MethodInfo.nameInit, consDesc );
			// athrow
			code.addOpcode( Opcode.ATHROW );
			final StackMapTable.Writer writer = new StackMapTable.Writer(32);
			final int[] localTags = {
					StackMapTable.OBJECT,
					StackMapTable.OBJECT,
					StackMapTable.OBJECT,
					StackMapTable.INTEGER
			};
			final int[] localData = {
					constPool.getThisClassInfo(),
					constPool.addClassInfo( "java/lang/Object" ),
					constPool.addClassInfo( "[Ljava/lang/Object;" ),
					0
			};
			final int[] stackTags = {
					StackMapTable.OBJECT
			};
			final int[] stackData = {
					throwableTypeIndex
			};
			writer.fullFrame( handlerPc, localTags, localData, stackTags, stackData );
			stackmap = writer.toStackMapTable( constPool );
		}
		else {
			// return
			code.addOpcode( Opcode.RETURN );
		}
		final CodeAttribute ca = code.toCodeAttribute();
		if ( stackmap != null ) {
			ca.setAttribute( stackmap );
		}
		setterMethodInfo.setCodeAttribute( ca );
		setterMethodInfo.setAccessFlags( AccessFlag.PUBLIC );
		classfile.addMethod( setterMethodInfo );
	}

	private void addUnwrapper(Bytecode code, Class type) {
		final int index = FactoryHelper.typeIndex( type );
		final String wrapperType = FactoryHelper.wrapperTypes[index];
		// checkcast
		code.addCheckcast( wrapperType );
		// invokevirtual
		code.addInvokevirtual( wrapperType, FactoryHelper.unwarpMethods[index], FactoryHelper.unwrapDesc[index] );
	}

	private static void findAccessors(
			Class clazz,
			String[] getterNames,
			String[] setterNames,
			Class[] types,
			Method[] getters,
			Method[] setters) {
		final int length = types.length;
		if ( setterNames.length != length || getterNames.length != length ) {
			throw new BulkAccessorException( "bad number of accessors" );
		}

		final Class[] getParam = new Class[0];
		final Class[] setParam = new Class[1];
		for ( int i = 0; i < length; i++ ) {
			if ( getterNames[i] != null ) {
				final Method getter = findAccessor( clazz, getterNames[i], getParam, i );
				if ( getter.getReturnType() != types[i] ) {
					throw new BulkAccessorException( "wrong return type: " + getterNames[i], i );
				}

				getters[i] = getter;
			}

			if ( setterNames[i] != null ) {
				setParam[0] = types[i];
				setters[i] = findAccessor( clazz, setterNames[i], setParam, i );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Method findAccessor(Class clazz, String name, Class[] params, int index)
			throws BulkAccessorException {
		try {
			final Method method = clazz.getDeclaredMethod( name, params );
			if ( Modifier.isPrivate( method.getModifiers() ) ) {
				throw new BulkAccessorException( "private property", index );
			}

			return method;
		}
		catch ( NoSuchMethodException e ) {
			throw new BulkAccessorException( "cannot find an accessor", index );
		}
	}
}
