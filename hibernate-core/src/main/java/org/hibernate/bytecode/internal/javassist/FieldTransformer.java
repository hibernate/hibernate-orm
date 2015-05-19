/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.StackMapTable;
import javassist.bytecode.stackmap.MapMaker;

/**
 * The thing that handles actual class enhancement in regards to
 * intercepting field accesses.
 *
 * @author Muga Nishizawa
 * @author Steve Ebersole
 * @author Dustin Schultz
 */
public class FieldTransformer {

	private static final String EACH_READ_METHOD_PREFIX = "$javassist_read_";

	private static final String EACH_WRITE_METHOD_PREFIX = "$javassist_write_";

	private static final String FIELD_HANDLED_TYPE_NAME = FieldHandled.class.getName();

	private static final String HANDLER_FIELD_NAME = "$JAVASSIST_READ_WRITE_HANDLER";

	private static final String FIELD_HANDLER_TYPE_NAME = FieldHandler.class.getName();

	private static final String HANDLER_FIELD_DESCRIPTOR = 'L' + FIELD_HANDLER_TYPE_NAME.replace( '.', '/' ) + ';';

	private static final String GETFIELDHANDLER_METHOD_NAME = "getFieldHandler";

	private static final String SETFIELDHANDLER_METHOD_NAME = "setFieldHandler";

	private static final String GETFIELDHANDLER_METHOD_DESCRIPTOR = "()" + HANDLER_FIELD_DESCRIPTOR;

	private static final String SETFIELDHANDLER_METHOD_DESCRIPTOR = "(" + HANDLER_FIELD_DESCRIPTOR + ")V";

	private final FieldFilter filter;
	private final ClassPool classPool;

	FieldTransformer(FieldFilter f, ClassPool c) {
		filter = f;
		classPool = c;
	}

	/**
	 * Transform the class contained in the given file, writing the result back to the same file.
	 *
	 * @param file The file containing the class to be transformed
	 *
	 * @throws Exception Indicates a problem performing the transformation
	 */
	public void transform(File file) throws Exception {
		final DataInputStream in = new DataInputStream( new FileInputStream( file ) );
		final ClassFile classfile = new ClassFile( in );
		transform( classfile );

		final DataOutputStream out = new DataOutputStream( new FileOutputStream( file ) );
		try {
			classfile.write( out );
		}
		finally {
			out.close();
		}
	}

	/**
	 * Transform the class defined by the given ClassFile descriptor.  The ClassFile descriptor itself is mutated
	 *
	 * @param classFile The class file descriptor
	 *
	 * @throws Exception Indicates a problem performing the transformation
	 */
	public void transform(ClassFile classFile) throws Exception {
		if ( classFile.isInterface() ) {
			return;
		}
		try {
			addFieldHandlerField( classFile );
			addGetFieldHandlerMethod( classFile );
			addSetFieldHandlerMethod( classFile );
			addFieldHandledInterface( classFile );
			addReadWriteMethods( classFile );
			transformInvokevirtualsIntoPutAndGetfields( classFile );
		}
		catch (CannotCompileException e) {
			throw new RuntimeException( e.getMessage(), e );
		}
	}

	private void addFieldHandlerField(ClassFile classfile) throws CannotCompileException {
		final ConstPool constPool = classfile.getConstPool();
		final FieldInfo fieldInfo = new FieldInfo( constPool, HANDLER_FIELD_NAME, HANDLER_FIELD_DESCRIPTOR );
		fieldInfo.setAccessFlags( AccessFlag.PRIVATE | AccessFlag.TRANSIENT );
		classfile.addField( fieldInfo );
	}

	private void addGetFieldHandlerMethod(ClassFile classfile) throws CannotCompileException, BadBytecode {
		final ConstPool constPool = classfile.getConstPool();
		final int thisClassInfo = constPool.getThisClassInfo();
		final MethodInfo getterMethodInfo = new MethodInfo(
				constPool,
				GETFIELDHANDLER_METHOD_NAME,
				GETFIELDHANDLER_METHOD_DESCRIPTOR
		);

		/* local variable | this | */
		final Bytecode code = new Bytecode( constPool, 2, 1 );
		// aload_0 // load this
		code.addAload( 0 );
		// getfield // get field "$JAVASSIST_CALLBACK" defined already
		code.addOpcode( Opcode.GETFIELD );
		final int fieldIndex = constPool.addFieldrefInfo( thisClassInfo, HANDLER_FIELD_NAME, HANDLER_FIELD_DESCRIPTOR );
		code.addIndex( fieldIndex );
		// areturn // return the value of the field
		code.addOpcode( Opcode.ARETURN );
		getterMethodInfo.setCodeAttribute( code.toCodeAttribute() );
		getterMethodInfo.setAccessFlags( AccessFlag.PUBLIC );
		final CodeAttribute codeAttribute = getterMethodInfo.getCodeAttribute();
		if ( codeAttribute != null ) {
			final StackMapTable smt = MapMaker.make( classPool, getterMethodInfo );
			codeAttribute.setAttribute( smt );
		}
		classfile.addMethod( getterMethodInfo );
	}

	private void addSetFieldHandlerMethod(ClassFile classfile) throws CannotCompileException, BadBytecode {
		final ConstPool constPool = classfile.getConstPool();
		final int thisClassInfo = constPool.getThisClassInfo();
		final MethodInfo methodInfo = new MethodInfo(
				constPool,
				SETFIELDHANDLER_METHOD_NAME,
				SETFIELDHANDLER_METHOD_DESCRIPTOR
		);

		/* local variables | this | callback | */
		final Bytecode code = new Bytecode(constPool, 3, 3);
		// aload_0 : load this
		code.addAload( 0 );
		// aload_1 : load callback
		code.addAload( 1 );
		// putfield // put field "$JAVASSIST_CALLBACK" defined already
		code.addOpcode( Opcode.PUTFIELD );
		final int fieldIndex = constPool.addFieldrefInfo( thisClassInfo, HANDLER_FIELD_NAME, HANDLER_FIELD_DESCRIPTOR );
		code.addIndex( fieldIndex );
		// return
		code.addOpcode( Opcode.RETURN );
		methodInfo.setCodeAttribute( code.toCodeAttribute() );
		methodInfo.setAccessFlags( AccessFlag.PUBLIC );
		final CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		if ( codeAttribute != null ) {
			final StackMapTable smt = MapMaker.make( classPool, methodInfo );
			codeAttribute.setAttribute( smt );
		}
		classfile.addMethod( methodInfo );
	}

	private void addFieldHandledInterface(ClassFile classfile) {
		final String[] interfaceNames = classfile.getInterfaces();
		final String[] newInterfaceNames = new String[interfaceNames.length + 1];
		System.arraycopy( interfaceNames, 0, newInterfaceNames, 0, interfaceNames.length );
		newInterfaceNames[newInterfaceNames.length - 1] = FIELD_HANDLED_TYPE_NAME;
		classfile.setInterfaces( newInterfaceNames );
	}

	private void addReadWriteMethods(ClassFile classfile) throws CannotCompileException, BadBytecode {
		final List fields = classfile.getFields();
		for ( Object field : fields ) {
			final FieldInfo finfo = (FieldInfo) field;
			if ( (finfo.getAccessFlags() & AccessFlag.STATIC) == 0 && (!finfo.getName().equals( HANDLER_FIELD_NAME )) ) {
				// case of non-static field
				if ( filter.handleRead( finfo.getDescriptor(), finfo.getName() ) ) {
					addReadMethod( classfile, finfo );
				}
				if ( filter.handleWrite( finfo.getDescriptor(), finfo.getName() ) ) {
					addWriteMethod( classfile, finfo );
				}
			}
		}
	}

	private void addReadMethod(ClassFile classfile, FieldInfo finfo) throws CannotCompileException, BadBytecode {
		final ConstPool constPool = classfile.getConstPool();
		final int thisClassInfo = constPool.getThisClassInfo();
		final String readMethodDescriptor = "()" + finfo.getDescriptor();
		final MethodInfo readMethodInfo = new MethodInfo(
				constPool,
				EACH_READ_METHOD_PREFIX + finfo.getName(),
				readMethodDescriptor
		);

		/* local variables | target obj | each oldvalue | */
		final Bytecode code = new Bytecode(constPool, 5, 3);
		// aload_0
		code.addAload( 0 );
		// getfield // get each field
		code.addOpcode( Opcode.GETFIELD );
		final int baseFieldIndex = constPool.addFieldrefInfo( thisClassInfo, finfo.getName(), finfo.getDescriptor() );
		code.addIndex( baseFieldIndex );
		// aload_0
		code.addAload( 0 );
		// invokeinterface : invoke Enabled.getInterceptFieldCallback()
		final int enabledClassIndex = constPool.addClassInfo( FIELD_HANDLED_TYPE_NAME );
		code.addInvokeinterface(
				enabledClassIndex,
				GETFIELDHANDLER_METHOD_NAME,
				GETFIELDHANDLER_METHOD_DESCRIPTOR,
				1
		);
		// ifnonnull
		code.addOpcode( Opcode.IFNONNULL );
		code.addIndex( 4 );
		// *return // each type
		addTypeDependDataReturn( code, finfo.getDescriptor() );
		// *store_1 // each type
		addTypeDependDataStore( code, finfo.getDescriptor(), 1 );
		// aload_0
		code.addAload( 0 );
		// invokeinterface // invoke Enabled.getInterceptFieldCallback()
		code.addInvokeinterface(
				enabledClassIndex,
				GETFIELDHANDLER_METHOD_NAME, GETFIELDHANDLER_METHOD_DESCRIPTOR,
				1
		);
		// aload_0
		code.addAload( 0 );
		// ldc // name of the field
		code.addLdc( finfo.getName() );
		// *load_1 // each type
		addTypeDependDataLoad( code, finfo.getDescriptor(), 1 );
		// invokeinterface // invoke Callback.read*() // each type
		addInvokeFieldHandlerMethod(
				classfile, code, finfo.getDescriptor(),
				true
		);
		// *return // each type
		addTypeDependDataReturn( code, finfo.getDescriptor() );

		readMethodInfo.setCodeAttribute( code.toCodeAttribute() );
		readMethodInfo.setAccessFlags( AccessFlag.PUBLIC );
		final CodeAttribute codeAttribute = readMethodInfo.getCodeAttribute();
		if ( codeAttribute != null ) {
			final StackMapTable smt = MapMaker.make( classPool, readMethodInfo );
			codeAttribute.setAttribute( smt );
		}
		classfile.addMethod( readMethodInfo );
	}

	private void addWriteMethod(ClassFile classfile, FieldInfo finfo) throws CannotCompileException, BadBytecode {
		final ConstPool constPool = classfile.getConstPool();
		final int thisClassInfo = constPool.getThisClassInfo();
		final String writeMethodDescriptor = "(" + finfo.getDescriptor() + ")V";
		final MethodInfo writeMethodInfo = new MethodInfo(
				constPool,
				EACH_WRITE_METHOD_PREFIX+ finfo.getName(),
				writeMethodDescriptor
		);

		/* local variables | target obj | each oldvalue | */
		final Bytecode code = new Bytecode(constPool, 6, 3);
		// aload_0
		code.addAload( 0 );
		// invokeinterface : enabled.getInterceptFieldCallback()
		final int enabledClassIndex = constPool.addClassInfo( FIELD_HANDLED_TYPE_NAME );
		code.addInvokeinterface(
				enabledClassIndex,
				GETFIELDHANDLER_METHOD_NAME, GETFIELDHANDLER_METHOD_DESCRIPTOR,
				1
		);
		// ifnonnull (label1)
		code.addOpcode( Opcode.IFNONNULL );
		code.addIndex( 9 );
		// aload_0
		code.addAload( 0 );
		// *load_1
		addTypeDependDataLoad( code, finfo.getDescriptor(), 1 );
		// putfield
		code.addOpcode( Opcode.PUTFIELD );
		final int baseFieldIndex = constPool.addFieldrefInfo( thisClassInfo, finfo.getName(), finfo.getDescriptor() );
		code.addIndex( baseFieldIndex );
		code.growStack( -Descriptor.dataSize( finfo.getDescriptor() ) );
		// return ;
		code.addOpcode( Opcode.RETURN );
		// aload_0
		code.addAload( 0 );
		// dup
		code.addOpcode( Opcode.DUP );
		// invokeinterface // enabled.getInterceptFieldCallback()
		code.addInvokeinterface(
				enabledClassIndex,
				GETFIELDHANDLER_METHOD_NAME,
				GETFIELDHANDLER_METHOD_DESCRIPTOR,
				1
		);
		// aload_0
		code.addAload( 0 );
		// ldc // field name
		code.addLdc( finfo.getName() );
		// aload_0
		code.addAload( 0 );
		// getfield // old value of the field
		code.addOpcode( Opcode.GETFIELD );
		code.addIndex( baseFieldIndex );
		code.growStack( Descriptor.dataSize( finfo.getDescriptor() ) - 1 );
		// *load_1
		addTypeDependDataLoad( code, finfo.getDescriptor(), 1 );
		// invokeinterface // callback.write*(..)
		addInvokeFieldHandlerMethod( classfile, code, finfo.getDescriptor(), false );
		// putfield // new value of the field
		code.addOpcode( Opcode.PUTFIELD );
		code.addIndex( baseFieldIndex );
		code.growStack( -Descriptor.dataSize( finfo.getDescriptor() ) );
		// return
		code.addOpcode( Opcode.RETURN );

		writeMethodInfo.setCodeAttribute( code.toCodeAttribute() );
		writeMethodInfo.setAccessFlags( AccessFlag.PUBLIC );
		final CodeAttribute codeAttribute = writeMethodInfo.getCodeAttribute();
		if ( codeAttribute != null ) {
			final StackMapTable smt = MapMaker.make( classPool, writeMethodInfo );
			codeAttribute.setAttribute( smt );
		}
		classfile.addMethod( writeMethodInfo );
	}

	private void transformInvokevirtualsIntoPutAndGetfields(ClassFile classfile) throws CannotCompileException, BadBytecode {
		for ( Object o : classfile.getMethods() ) {
			final MethodInfo methodInfo = (MethodInfo) o;
			final String methodName = methodInfo.getName();
			if ( methodName.startsWith( EACH_READ_METHOD_PREFIX )
					|| methodName.startsWith( EACH_WRITE_METHOD_PREFIX )
					|| methodName.equals( GETFIELDHANDLER_METHOD_NAME )
					|| methodName.equals( SETFIELDHANDLER_METHOD_NAME ) ) {
				continue;
			}

			final CodeAttribute codeAttr = methodInfo.getCodeAttribute();
			if ( codeAttr == null ) {
				continue;
			}

			final CodeIterator iter = codeAttr.iterator();
			while ( iter.hasNext() ) {
				int pos = iter.next();
				pos = transformInvokevirtualsIntoGetfields( classfile, iter, pos );
				transformInvokevirtualsIntoPutfields( classfile, iter, pos );
			}
			final StackMapTable smt = MapMaker.make( classPool, methodInfo );
			codeAttr.setAttribute( smt );
		}
	}

	private int transformInvokevirtualsIntoGetfields(ClassFile classfile, CodeIterator iter, int pos) {
		final ConstPool constPool = classfile.getConstPool();
		final int c = iter.byteAt( pos );
		if ( c != Opcode.GETFIELD ) {
			return pos;
		}

		final int index = iter.u16bitAt( pos + 1 );
		final String fieldName = constPool.getFieldrefName( index );
		final String className = constPool.getFieldrefClassName( index );
		if ( !filter.handleReadAccess( className, fieldName ) ) {
			return pos;
		}

		final String fieldReaderMethodDescriptor = "()" + constPool.getFieldrefType( index );
		final int fieldReaderMethodIndex = constPool.addMethodrefInfo(
				constPool.getThisClassInfo(),
				EACH_READ_METHOD_PREFIX + fieldName,
				fieldReaderMethodDescriptor
		);
		iter.writeByte( Opcode.INVOKEVIRTUAL, pos );
		iter.write16bit( fieldReaderMethodIndex, pos + 1 );
		return pos;
	}

	private int transformInvokevirtualsIntoPutfields(ClassFile classfile, CodeIterator iter, int pos) {
		final ConstPool constPool = classfile.getConstPool();
		final int c = iter.byteAt( pos );
		if ( c != Opcode.PUTFIELD ) {
			return pos;
		}

		final int index = iter.u16bitAt( pos + 1 );
		final String fieldName = constPool.getFieldrefName( index );
		final String className = constPool.getFieldrefClassName( index );
		if ( !filter.handleWriteAccess( className, fieldName ) ) {
			return pos;
		}

		final String fieldWriterMethodDescriptor = "(" + constPool.getFieldrefType( index ) + ")V";
		final int fieldWriterMethodIndex = constPool.addMethodrefInfo(
				constPool.getThisClassInfo(),
				EACH_WRITE_METHOD_PREFIX + fieldName,
				fieldWriterMethodDescriptor
		);
		iter.writeByte( Opcode.INVOKEVIRTUAL, pos );
		iter.write16bit( fieldWriterMethodIndex, pos + 1 );
		return pos;
	}

	private static void addInvokeFieldHandlerMethod(
			ClassFile classfile,
			Bytecode code,
			String typeName,
			boolean isReadMethod) {
		final ConstPool constPool = classfile.getConstPool();
		// invokeinterface
		final int callbackTypeIndex = constPool.addClassInfo( FIELD_HANDLER_TYPE_NAME );
		if ( ( typeName.charAt( 0 ) == 'L' )
				&& ( typeName.charAt( typeName.length() - 1 ) == ';' )
				|| ( typeName.charAt( 0 ) == '[' ) ) {
			// reference type
			final int indexOfL = typeName.indexOf( 'L' );
			String type;
			if ( indexOfL == 0 ) {
				// not array
				type = typeName.substring( 1, typeName.length() - 1 );
				type = type.replace( '/', '.' );
			}
			else if ( indexOfL == -1 ) {
				// array of primitive type
				// do nothing
				type = typeName;
			}
			else {
				// array of reference type
				type = typeName.replace( '/', '.' );
			}

			if ( isReadMethod ) {
				code.addInvokeinterface(
						callbackTypeIndex,
						"readObject",
						"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
						4
				);
				// checkcast
				code.addCheckcast( type );
			}
			else {
				code.addInvokeinterface(
						callbackTypeIndex,
						"writeObject",
						"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
						5
				);
				// checkcast
				code.addCheckcast( type );
			}
		}
		else if ( typeName.equals( "Z" ) ) {
			// boolean
			if ( isReadMethod ) {
				code.addInvokeinterface(
						callbackTypeIndex,
						"readBoolean",
						"(Ljava/lang/Object;Ljava/lang/String;Z)Z",
						4
				);
			}
			else {
				code.addInvokeinterface(
						callbackTypeIndex,
						"writeBoolean",
						"(Ljava/lang/Object;Ljava/lang/String;ZZ)Z",
						5
				);
			}
		}
		else if ( typeName.equals( "B" ) ) {
			// byte
			if ( isReadMethod ) {
				code.addInvokeinterface(
						callbackTypeIndex,
						"readByte",
						"(Ljava/lang/Object;Ljava/lang/String;B)B",
						4
				);
			}
			else {
				code.addInvokeinterface(
						callbackTypeIndex,
						"writeByte",
						"(Ljava/lang/Object;Ljava/lang/String;BB)B",
						5
				);
			}
		}
		else if ( typeName.equals( "C" ) ) {
			// char
			if ( isReadMethod ) {
				code.addInvokeinterface(
						callbackTypeIndex,
						"readChar",
						"(Ljava/lang/Object;Ljava/lang/String;C)C",
						4
				);
			}
			else {
				code.addInvokeinterface(
						callbackTypeIndex,
						"writeChar",
						"(Ljava/lang/Object;Ljava/lang/String;CC)C",
						5
				);
			}
		}
		else if ( typeName.equals( "I" ) ) {
			// int
			if ( isReadMethod ) {
				code.addInvokeinterface(
						callbackTypeIndex,
						"readInt",
						"(Ljava/lang/Object;Ljava/lang/String;I)I",
						4
				);
			}
			else {
				code.addInvokeinterface(
						callbackTypeIndex,
						"writeInt",
						"(Ljava/lang/Object;Ljava/lang/String;II)I",
						5
				);
			}
		}
		else if ( typeName.equals( "S" ) ) {
			// short
			if ( isReadMethod ) {
				code.addInvokeinterface(
						callbackTypeIndex,
						"readShort",
						"(Ljava/lang/Object;Ljava/lang/String;S)S",
						4
				);
			}
			else {
				code.addInvokeinterface(
						callbackTypeIndex,
						"writeShort",
						"(Ljava/lang/Object;Ljava/lang/String;SS)S",
						5
				);
			}
		}
		else if ( typeName.equals( "D" ) ) {
			// double
			if ( isReadMethod ) {
				code.addInvokeinterface(
						callbackTypeIndex,
						"readDouble",
						"(Ljava/lang/Object;Ljava/lang/String;D)D",
						5
				);
			}
			else {
				code.addInvokeinterface(
						callbackTypeIndex,
						"writeDouble",
						"(Ljava/lang/Object;Ljava/lang/String;DD)D",
						7
				);
			}
		}
		else if ( typeName.equals( "F" ) ) {
			// float
			if ( isReadMethod ) {
				code.addInvokeinterface(
						callbackTypeIndex,
						"readFloat",
						"(Ljava/lang/Object;Ljava/lang/String;F)F",
						4
				);
			}
			else {
				code.addInvokeinterface(
						callbackTypeIndex,
						"writeFloat",
						"(Ljava/lang/Object;Ljava/lang/String;FF)F",
						5
				);
			}
		}
		else if ( typeName.equals( "J" ) ) {
			// long
			if ( isReadMethod ) {
				code.addInvokeinterface(
						callbackTypeIndex,
						"readLong",
						"(Ljava/lang/Object;Ljava/lang/String;J)J",
						5
				);
			}
			else {
				code.addInvokeinterface(
						callbackTypeIndex,
						"writeLong",
						"(Ljava/lang/Object;Ljava/lang/String;JJ)J",
						7
				);
			}
		}
		else {
			// bad type
			throw new RuntimeException( "bad type: " + typeName );
		}
	}

	private static void addTypeDependDataLoad(Bytecode code, String typeName, int i) {
		if ( typeName.charAt( 0 ) == 'L'
				&& typeName.charAt( typeName.length() - 1 ) == ';'
				|| typeName.charAt( 0 ) == '[' ) {
			// reference type
			code.addAload( i );
		}
		else if ( typeName.equals( "Z" )
				|| typeName.equals( "B" )
				|| typeName.equals( "C" )
				|| typeName.equals( "I" )
				|| typeName.equals( "S" ) ) {
			// boolean, byte, char, int, short
			code.addIload( i );
		}
		else if ( typeName.equals( "D" ) ) {
			// double
			code.addDload( i );
		}
		else if ( typeName.equals( "F" ) ) {
			// float
			code.addFload( i );
		}
		else if ( typeName.equals( "J" ) ) {
			// long
			code.addLload( i );
		}
		else {
			// bad type
			throw new RuntimeException( "bad type: " + typeName );
		}
	}

	private static void addTypeDependDataStore(Bytecode code, String typeName, int i) {
		if ( typeName.charAt( 0 ) == 'L'
				&& typeName.charAt( typeName.length() - 1 ) == ';'
				|| typeName.charAt( 0 ) == '[' ) {
			// reference type
			code.addAstore( i );
		}
		else if ( typeName.equals( "Z" )
				|| typeName.equals( "B" )
				|| typeName.equals( "C" )
				|| typeName.equals( "I" )
				|| typeName.equals( "S" ) ) {
			// boolean, byte, char, int, short
			code.addIstore( i );
		}
		else if ( typeName.equals( "D" ) ) {
			// double
			code.addDstore( i );
		}
		else if ( typeName.equals( "F" ) ) {
			// float
			code.addFstore( i );
		}
		else if ( typeName.equals( "J" ) ) {
			// long
			code.addLstore( i );
		}
		else {
			// bad type
			throw new RuntimeException( "bad type: " + typeName );
		}
	}

	private static void addTypeDependDataReturn(Bytecode code, String typeName) {
		if ( typeName.charAt( 0 ) == 'L'
				&& typeName.charAt( typeName.length() - 1 ) == ';'
				|| typeName.charAt( 0 ) == '[') {
			// reference type
			code.addOpcode( Opcode.ARETURN );
		}
		else if ( typeName.equals( "Z" )
				|| typeName.equals( "B" )
				|| typeName.equals( "C" )
				|| typeName.equals( "I" )
				|| typeName.equals( "S" ) ) {
			// boolean, byte, char, int, short
			code.addOpcode( Opcode.IRETURN );
		}
		else if ( typeName.equals( "D" ) ) {
			// double
			code.addOpcode( Opcode.DRETURN );
		}
		else if ( typeName.equals( "F" ) ) {
			// float
			code.addOpcode( Opcode.FRETURN );
		}
		else if ( typeName.equals( "J" ) ) {
			// long
			code.addOpcode( Opcode.LRETURN );
		}
		else {
			// bad type
			throw new RuntimeException( "bad type: " + typeName );
		}
	}

}
