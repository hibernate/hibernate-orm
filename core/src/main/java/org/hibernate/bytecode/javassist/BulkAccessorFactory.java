package org.hibernate.bytecode.javassist;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
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
	private static int counter = 0;

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
		Method[] getters = new Method[getterNames.length];
		Method[] setters = new Method[setterNames.length];
		findAccessors( targetBean, getterNames, setterNames, types, getters, setters );

		Class beanClass;
		try {
			ClassFile classfile = make( getters, setters );
			ClassLoader loader = this.getClassLoader();
			if ( writeDirectory != null ) {
				FactoryHelper.writeFile( classfile, writeDirectory );
			}

			beanClass = FactoryHelper.toClass( classfile, loader, getDomain() );
			return ( BulkAccessor ) this.newInstance( beanClass );
		}
		catch ( Exception e ) {
			throw new BulkAccessorException( e.getMessage(), e );
		}
	}

	private ProtectionDomain getDomain() {
		Class cl;
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
			className = "org.javassist.tmp." + className;
		}

		ClassFile classfile = new ClassFile( false, className, BULKACESSOR_CLASS_NAME );
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
		BulkAccessor instance = ( BulkAccessor ) type.newInstance();
		instance.target = targetBean;
		int len = getterNames.length;
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
	 * @param classfile
	 * @throws CannotCompileException
	 */
	private void addDefaultConstructor(ClassFile classfile) throws CannotCompileException {
		ConstPool cp = classfile.getConstPool();
		String cons_desc = "()V";
		MethodInfo mi = new MethodInfo( cp, MethodInfo.nameInit, cons_desc );

		Bytecode code = new Bytecode( cp, 0, 1 );
		// aload_0
		code.addAload( 0 );
		// invokespecial
		code.addInvokespecial( BulkAccessor.class.getName(), MethodInfo.nameInit, cons_desc );
		// return
		code.addOpcode( Opcode.RETURN );

		mi.setCodeAttribute( code.toCodeAttribute() );
		mi.setAccessFlags( AccessFlag.PUBLIC );
		classfile.addMethod( mi );
	}

	private void addGetter(ClassFile classfile, final Method[] getters) throws CannotCompileException {
		ConstPool cp = classfile.getConstPool();
		int target_type_index = cp.addClassInfo( this.targetBean.getName() );
		String desc = GET_SETTER_DESC;
		MethodInfo mi = new MethodInfo( cp, GENERATED_GETTER_NAME, desc );

		Bytecode code = new Bytecode( cp, 6, 4 );
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
					Method getter = getters[i];
					// aload_2 // args
					code.addAload( 2 );
					// iconst_i // continue to aastore
					code.addIconst( i ); // growing stack is 1
					Class returnType = getter.getReturnType();
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
					String getter_desc = RuntimeSupport.makeDescriptor( getter );
					String getterName = getter.getName();
					if ( this.targetBean.isInterface() ) {
						// invokeinterface
						code.addInvokeinterface( target_type_index, getterName, getter_desc, 1 );
					}
					else {
						// invokevirtual
						code.addInvokevirtual( target_type_index, getterName, getter_desc );
					}

					if ( typeIndex >= 0 ) {       // is a primitive type
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

		mi.setCodeAttribute( code.toCodeAttribute() );
		mi.setAccessFlags( AccessFlag.PUBLIC );
		classfile.addMethod( mi );
	}

	private void addSetter(ClassFile classfile, final Method[] setters) throws CannotCompileException {
		ConstPool cp = classfile.getConstPool();
		int target_type_index = cp.addClassInfo( this.targetBean.getName() );
		String desc = GET_SETTER_DESC;
		MethodInfo mi = new MethodInfo( cp, GENERATED_SETTER_NAME, desc );

		Bytecode code = new Bytecode( cp, 4, 6 );
		/* | this | bean | args | i | raw bean | exception | */
		if ( setters.length > 0 ) {
			int start, end; // required to exception table
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
					int diff = i - lastIndex;
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
				Class[] setterParamTypes = setters[i].getParameterTypes();
				Class setterParamType = setterParamTypes[0];
				if ( setterParamType.isPrimitive() ) {
					// checkcast (case of primitive type)
					// invokevirtual (case of primitive type)
					this.addUnwrapper( classfile, code, setterParamType );
				}
				else {
					// checkcast (case of reference type)
					code.addCheckcast( setterParamType.getName() );
				}
				/* current stack len = 2 */
				String rawSetterMethod_desc = RuntimeSupport.makeDescriptor( setters[i] );
				if ( !this.targetBean.isInterface() ) {
					// invokevirtual
					code.addInvokevirtual( target_type_index, setters[i].getName(), rawSetterMethod_desc );
				}
				else {
					// invokeinterface
					Class[] params = setters[i].getParameterTypes();
					int size;
					if ( params[0].equals( Double.TYPE ) || params[0].equals( Long.TYPE ) ) {
						size = 3;
					}
					else {
						size = 2;
					}

					code.addInvokeinterface( target_type_index, setters[i].getName(), rawSetterMethod_desc, size );
				}
			}

			// end region to handling exception (BulkAccessorException)
			end = code.currentPc();
			// return
			code.addOpcode( Opcode.RETURN );
			/* current stack len = 0 */
			// register in exception table
			int throwableType_index = cp.addClassInfo( THROWABLE_CLASS_NAME );
			code.addExceptionHandler( start, end, code.currentPc(), throwableType_index );
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
			String cons_desc = "(Ljava/lang/Throwable;I)V";
			code.addInvokespecial( BULKEXCEPTION_CLASS_NAME, MethodInfo.nameInit, cons_desc );
			// athrow
			code.addOpcode( Opcode.ATHROW );
		}
		else {
			// return
			code.addOpcode( Opcode.RETURN );
		}

		mi.setCodeAttribute( code.toCodeAttribute() );
		mi.setAccessFlags( AccessFlag.PUBLIC );
		classfile.addMethod( mi );
	}

	private void addUnwrapper(
			ClassFile classfile,
	        Bytecode code,
	        Class type) {
		int index = FactoryHelper.typeIndex( type );
		String wrapperType = FactoryHelper.wrapperTypes[index];
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
		int length = types.length;
		if ( setterNames.length != length || getterNames.length != length ) {
			throw new BulkAccessorException( "bad number of accessors" );
		}

		Class[] getParam = new Class[0];
		Class[] setParam = new Class[1];
		for ( int i = 0; i < length; i++ ) {
			if ( getterNames[i] != null ) {
				Method getter = findAccessor( clazz, getterNames[i], getParam, i );
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

	private static Method findAccessor(
			Class clazz,
	        String name,
	        Class[] params,
	        int index) throws BulkAccessorException {
		try {
			Method method = clazz.getDeclaredMethod( name, params );
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
