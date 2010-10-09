/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.bytecode.javassist;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

import javassist.CannotCompileException;
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

/**
 * The thing that handles actual class enhancement in regards to
 * intercepting field accesses.
 *
 * @author Muga Nishizawa
 * @author Steve Ebersole
 */
public class FieldTransformer {

	private static final String EACH_READ_METHOD_PREFIX = "$javassist_read_";

	private static final String EACH_WRITE_METHOD_PREFIX = "$javassist_write_";

	private static final String FIELD_HANDLED_TYPE_NAME = FieldHandled.class
			.getName();

	private static final String HANDLER_FIELD_NAME = "$JAVASSIST_READ_WRITE_HANDLER";

	private static final String FIELD_HANDLER_TYPE_NAME = FieldHandler.class
			.getName();

	private static final String HANDLER_FIELD_DESCRIPTOR = 'L' + FIELD_HANDLER_TYPE_NAME
			.replace('.', '/') + ';';

	private static final String GETFIELDHANDLER_METHOD_NAME = "getFieldHandler";

	private static final String SETFIELDHANDLER_METHOD_NAME = "setFieldHandler";

	private static final String GETFIELDHANDLER_METHOD_DESCRIPTOR = "()"
	                                                                + HANDLER_FIELD_DESCRIPTOR;

	private static final String SETFIELDHANDLER_METHOD_DESCRIPTOR = "("
	                                                                + HANDLER_FIELD_DESCRIPTOR + ")V";

	private FieldFilter filter;

	public FieldTransformer() {
		this(null);
	}

	public FieldTransformer(FieldFilter f) {
		filter = f;
	}

	public void setFieldFilter(FieldFilter f) {
		filter = f;
	}

	public void transform(File file) throws Exception {
		DataInputStream in = new DataInputStream(new FileInputStream(file));
		ClassFile classfile = new ClassFile(in);
		transform(classfile);
		DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
		try {
			classfile.write(out);
		} finally {
			out.close();
		}
	}

	public void transform(ClassFile classfile) throws Exception {
		if (classfile.isInterface()) {
			return;
		}
		try {
			addFieldHandlerField(classfile);
			addGetFieldHandlerMethod(classfile);
			addSetFieldHandlerMethod(classfile);
			addFieldHandledInterface(classfile);
			addReadWriteMethods(classfile);
			transformInvokevirtualsIntoPutAndGetfields(classfile);
		} catch (CannotCompileException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private void addFieldHandlerField(ClassFile classfile)
			throws CannotCompileException {
		ConstPool cp = classfile.getConstPool();
		FieldInfo finfo = new FieldInfo(cp, HANDLER_FIELD_NAME,
		                                HANDLER_FIELD_DESCRIPTOR);
		finfo.setAccessFlags(AccessFlag.PRIVATE | AccessFlag.TRANSIENT);
		classfile.addField(finfo);
	}

	private void addGetFieldHandlerMethod(ClassFile classfile)
			throws CannotCompileException {
		ConstPool cp = classfile.getConstPool();
		int this_class_index = cp.getThisClassInfo();
		MethodInfo minfo = new MethodInfo(cp, GETFIELDHANDLER_METHOD_NAME,
		                                  GETFIELDHANDLER_METHOD_DESCRIPTOR);
		/* local variable | this | */
		Bytecode code = new Bytecode(cp, 2, 1);
		// aload_0 // load this
		code.addAload(0);
		// getfield // get field "$JAVASSIST_CALLBACK" defined already
		code.addOpcode(Opcode.GETFIELD);
		int field_index = cp.addFieldrefInfo(this_class_index,
		                                     HANDLER_FIELD_NAME, HANDLER_FIELD_DESCRIPTOR);
		code.addIndex(field_index);
		// areturn // return the value of the field
		code.addOpcode(Opcode.ARETURN);
		minfo.setCodeAttribute(code.toCodeAttribute());
		minfo.setAccessFlags(AccessFlag.PUBLIC);
		classfile.addMethod(minfo);
	}

	private void addSetFieldHandlerMethod(ClassFile classfile)
			throws CannotCompileException {
		ConstPool cp = classfile.getConstPool();
		int this_class_index = cp.getThisClassInfo();
		MethodInfo minfo = new MethodInfo(cp, SETFIELDHANDLER_METHOD_NAME,
		                                  SETFIELDHANDLER_METHOD_DESCRIPTOR);
		/* local variables | this | callback | */
		Bytecode code = new Bytecode(cp, 3, 3);
		// aload_0 // load this
		code.addAload(0);
		// aload_1 // load callback
		code.addAload(1);
		// putfield // put field "$JAVASSIST_CALLBACK" defined already
		code.addOpcode(Opcode.PUTFIELD);
		int field_index = cp.addFieldrefInfo(this_class_index,
		                                     HANDLER_FIELD_NAME, HANDLER_FIELD_DESCRIPTOR);
		code.addIndex(field_index);
		// return
		code.addOpcode(Opcode.RETURN);
		minfo.setCodeAttribute(code.toCodeAttribute());
		minfo.setAccessFlags(AccessFlag.PUBLIC);
		classfile.addMethod(minfo);
	}

	private void addFieldHandledInterface(ClassFile classfile) {
		String[] interfaceNames = classfile.getInterfaces();
		String[] newInterfaceNames = new String[interfaceNames.length + 1];
		System.arraycopy(interfaceNames, 0, newInterfaceNames, 0,
		                 interfaceNames.length);
		newInterfaceNames[newInterfaceNames.length - 1] = FIELD_HANDLED_TYPE_NAME;
		classfile.setInterfaces(newInterfaceNames);
	}

	private void addReadWriteMethods(ClassFile classfile)
			throws CannotCompileException {
		List fields = classfile.getFields();
		for (Iterator field_iter = fields.iterator(); field_iter.hasNext();) {
			FieldInfo finfo = (FieldInfo) field_iter.next();
			if ((finfo.getAccessFlags() & AccessFlag.STATIC) == 0
			    && (!finfo.getName().equals(HANDLER_FIELD_NAME))) {
				// case of non-static field
				if (filter.handleRead(finfo.getDescriptor(), finfo
						.getName())) {
					addReadMethod(classfile, finfo);
				}
				if (filter.handleWrite(finfo.getDescriptor(), finfo
						.getName())) {
					addWriteMethod(classfile, finfo);
				}
			}
		}
	}

	private void addReadMethod(ClassFile classfile, FieldInfo finfo)
			throws CannotCompileException {
		ConstPool cp = classfile.getConstPool();
		int this_class_index = cp.getThisClassInfo();
		String desc = "()" + finfo.getDescriptor();
		MethodInfo minfo = new MethodInfo(cp, EACH_READ_METHOD_PREFIX
		                                      + finfo.getName(), desc);
		/* local variables | target obj | each oldvalue | */
		Bytecode code = new Bytecode(cp, 5, 3);
		// aload_0
		code.addAload(0);
		// getfield // get each field
		code.addOpcode(Opcode.GETFIELD);
		int base_field_index = cp.addFieldrefInfo(this_class_index, finfo
				.getName(), finfo.getDescriptor());
		code.addIndex(base_field_index);
		// aload_0
		code.addAload(0);
		// invokeinterface // invoke Enabled.getInterceptFieldCallback()
		int enabled_class_index = cp.addClassInfo(FIELD_HANDLED_TYPE_NAME);
		code.addInvokeinterface(enabled_class_index,
		                        GETFIELDHANDLER_METHOD_NAME, GETFIELDHANDLER_METHOD_DESCRIPTOR,
		                        1);
		// ifnonnull
		code.addOpcode(Opcode.IFNONNULL);
		code.addIndex(4);
		// *return // each type
		addTypeDependDataReturn(code, finfo.getDescriptor());
		// *store_1 // each type
		addTypeDependDataStore(code, finfo.getDescriptor(), 1);
		// aload_0
		code.addAload(0);
		// invokeinterface // invoke Enabled.getInterceptFieldCallback()
		code.addInvokeinterface(enabled_class_index,
		                        GETFIELDHANDLER_METHOD_NAME, GETFIELDHANDLER_METHOD_DESCRIPTOR,
		                        1);
		// aload_0
		code.addAload(0);
		// ldc // name of the field
		code.addLdc(finfo.getName());
		// *load_1 // each type
		addTypeDependDataLoad(code, finfo.getDescriptor(), 1);
		// invokeinterface // invoke Callback.read*() // each type
		addInvokeFieldHandlerMethod(classfile, code, finfo.getDescriptor(),
		                            true);
		// *return // each type
		addTypeDependDataReturn(code, finfo.getDescriptor());

		minfo.setCodeAttribute(code.toCodeAttribute());
		minfo.setAccessFlags(AccessFlag.PUBLIC);
		classfile.addMethod(minfo);
	}

	private void addWriteMethod(ClassFile classfile, FieldInfo finfo)
			throws CannotCompileException {
		ConstPool cp = classfile.getConstPool();
		int this_class_index = cp.getThisClassInfo();
		String desc = "(" + finfo.getDescriptor() + ")V";
		MethodInfo minfo = new MethodInfo(cp, EACH_WRITE_METHOD_PREFIX
		                                      + finfo.getName(), desc);
		/* local variables | target obj | each oldvalue | */
		Bytecode code = new Bytecode(cp, 6, 3);
		// aload_0
		code.addAload(0);
		// invokeinterface // enabled.getInterceptFieldCallback()
		int enabled_class_index = cp.addClassInfo(FIELD_HANDLED_TYPE_NAME);
		code.addInvokeinterface(enabled_class_index,
		                        GETFIELDHANDLER_METHOD_NAME, GETFIELDHANDLER_METHOD_DESCRIPTOR,
		                        1);
		// ifnonnull (label1)
		code.addOpcode(Opcode.IFNONNULL);
		code.addIndex(9);
		// aload_0
		code.addAload(0);
		// *load_1
		addTypeDependDataLoad(code, finfo.getDescriptor(), 1);
		// putfield
		code.addOpcode(Opcode.PUTFIELD);
		int base_field_index = cp.addFieldrefInfo(this_class_index, finfo
				.getName(), finfo.getDescriptor());
		code.addIndex(base_field_index);
		code.growStack(-Descriptor.dataSize(finfo.getDescriptor()));
		// return ;
		code.addOpcode(Opcode.RETURN);
		// aload_0
		code.addAload(0);
		// dup
		code.addOpcode(Opcode.DUP);
		// invokeinterface // enabled.getInterceptFieldCallback()
		code.addInvokeinterface(enabled_class_index,
		                        GETFIELDHANDLER_METHOD_NAME, GETFIELDHANDLER_METHOD_DESCRIPTOR,
		                        1);
		// aload_0
		code.addAload(0);
		// ldc // field name
		code.addLdc(finfo.getName());
		// aload_0
		code.addAload(0);
		// getfield // old value of the field
		code.addOpcode(Opcode.GETFIELD);
		code.addIndex(base_field_index);
		code.growStack(Descriptor.dataSize(finfo.getDescriptor()) - 1);
		// *load_1
		addTypeDependDataLoad(code, finfo.getDescriptor(), 1);
		// invokeinterface // callback.write*(..)
		addInvokeFieldHandlerMethod(classfile, code, finfo.getDescriptor(),
		                            false);
		// putfield // new value of the field
		code.addOpcode(Opcode.PUTFIELD);
		code.addIndex(base_field_index);
		code.growStack(-Descriptor.dataSize(finfo.getDescriptor()));
		// return
		code.addOpcode(Opcode.RETURN);

		minfo.setCodeAttribute(code.toCodeAttribute());
		minfo.setAccessFlags(AccessFlag.PUBLIC);
		classfile.addMethod(minfo);
	}

	private void transformInvokevirtualsIntoPutAndGetfields(ClassFile classfile)
			throws CannotCompileException {
		List methods = classfile.getMethods();
		for (Iterator method_iter = methods.iterator(); method_iter.hasNext();) {
			MethodInfo minfo = (MethodInfo) method_iter.next();
			String methodName = minfo.getName();
			if (methodName.startsWith(EACH_READ_METHOD_PREFIX)
			    || methodName.startsWith(EACH_WRITE_METHOD_PREFIX)
			    || methodName.equals(GETFIELDHANDLER_METHOD_NAME)
			    || methodName.equals(SETFIELDHANDLER_METHOD_NAME)) {
				continue;
			}
			CodeAttribute codeAttr = minfo.getCodeAttribute();
			if (codeAttr == null) {
				continue;
			}
			CodeIterator iter = codeAttr.iterator();
			while (iter.hasNext()) {
				try {
					int pos = iter.next();
					pos = transformInvokevirtualsIntoGetfields(classfile, iter, pos);
					pos = transformInvokevirtualsIntoPutfields(classfile, iter, pos);

				} catch (BadBytecode e) {
					throw new CannotCompileException(e);
				}
			}
		}
	}

	private int transformInvokevirtualsIntoGetfields(ClassFile classfile, CodeIterator iter, int pos) {
		ConstPool cp = classfile.getConstPool();
		int c = iter.byteAt(pos);
		if (c != Opcode.GETFIELD) {
			return pos;
		}
		int index = iter.u16bitAt(pos + 1);
		String fieldName = cp.getFieldrefName(index);
		String className = cp.getFieldrefClassName(index);
		if ( !filter.handleReadAccess( className, fieldName ) ) {
			return pos;
		}
		String desc = "()" + cp.getFieldrefType( index );
		int read_method_index = cp.addMethodrefInfo(
				cp.getThisClassInfo(),
				EACH_READ_METHOD_PREFIX + fieldName,
				desc
		);
		iter.writeByte(Opcode.INVOKEVIRTUAL, pos);
		iter.write16bit(read_method_index, pos + 1);
		return pos;
	}

	private int transformInvokevirtualsIntoPutfields(
			ClassFile classfile,
			CodeIterator iter, int pos) {
		ConstPool cp = classfile.getConstPool();
		int c = iter.byteAt(pos);
		if (c != Opcode.PUTFIELD) {
			return pos;
		}
		int index = iter.u16bitAt(pos + 1);
		String fieldName = cp.getFieldrefName(index);
		String className = cp.getFieldrefClassName(index);
		if ( !filter.handleWriteAccess( className, fieldName ) ) {
			return pos;
		}
		String desc = "(" + cp.getFieldrefType( index ) + ")V";
		int write_method_index = cp.addMethodrefInfo(
				cp.getThisClassInfo(),
				EACH_WRITE_METHOD_PREFIX + fieldName,
				desc
		);
		iter.writeByte(Opcode.INVOKEVIRTUAL, pos);
		iter.write16bit(write_method_index, pos + 1);
		return pos;
	}

	private static void addInvokeFieldHandlerMethod(ClassFile classfile,
	                                                Bytecode code, String typeName, boolean isReadMethod) {
		ConstPool cp = classfile.getConstPool();
		// invokeinterface
		int callback_type_index = cp.addClassInfo(FIELD_HANDLER_TYPE_NAME);
		if ((typeName.charAt(0) == 'L')
		    && (typeName.charAt(typeName.length() - 1) == ';')
		    || (typeName.charAt(0) == '[')) {
			// reference type
			int indexOfL = typeName.indexOf('L');
			String type;
			if (indexOfL == 0) {
				// not array
				type = typeName.substring(1, typeName.length() - 1);
				type = type.replace('/', '.');
			} else if (indexOfL == -1) {
				// array of primitive type
				// do nothing
				type = typeName;
			} else {
				// array of reference type
				type = typeName.replace('/', '.');
			}
			if (isReadMethod) {
				code
						.addInvokeinterface(
								callback_type_index,
								"readObject",
								"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
								4);
				// checkcast
				code.addCheckcast(type);
			} else {
				code
						.addInvokeinterface(
								callback_type_index,
								"writeObject",
								"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
								5);
				// checkcast
				code.addCheckcast(type);
			}
		} else if (typeName.equals("Z")) {
			// boolean
			if (isReadMethod) {
				code.addInvokeinterface(callback_type_index, "readBoolean",
				                        "(Ljava/lang/Object;Ljava/lang/String;Z)Z", 4);
			} else {
				code.addInvokeinterface(callback_type_index, "writeBoolean",
				                        "(Ljava/lang/Object;Ljava/lang/String;ZZ)Z", 5);
			}
		} else if (typeName.equals("B")) {
			// byte
			if (isReadMethod) {
				code.addInvokeinterface(callback_type_index, "readByte",
				                        "(Ljava/lang/Object;Ljava/lang/String;B)B", 4);
			} else {
				code.addInvokeinterface(callback_type_index, "writeByte",
				                        "(Ljava/lang/Object;Ljava/lang/String;BB)B", 5);
			}
		} else if (typeName.equals("C")) {
			// char
			if (isReadMethod) {
				code.addInvokeinterface(callback_type_index, "readChar",
				                        "(Ljava/lang/Object;Ljava/lang/String;C)C", 4);
			} else {
				code.addInvokeinterface(callback_type_index, "writeChar",
				                        "(Ljava/lang/Object;Ljava/lang/String;CC)C", 5);
			}
		} else if (typeName.equals("I")) {
			// int
			if (isReadMethod) {
				code.addInvokeinterface(callback_type_index, "readInt",
				                        "(Ljava/lang/Object;Ljava/lang/String;I)I", 4);
			} else {
				code.addInvokeinterface(callback_type_index, "writeInt",
				                        "(Ljava/lang/Object;Ljava/lang/String;II)I", 5);
			}
		} else if (typeName.equals("S")) {
			// short
			if (isReadMethod) {
				code.addInvokeinterface(callback_type_index, "readShort",
				                        "(Ljava/lang/Object;Ljava/lang/String;S)S", 4);
			} else {
				code.addInvokeinterface(callback_type_index, "writeShort",
				                        "(Ljava/lang/Object;Ljava/lang/String;SS)S", 5);
			}
		} else if (typeName.equals("D")) {
			// double
			if (isReadMethod) {
				code.addInvokeinterface(callback_type_index, "readDouble",
				                        "(Ljava/lang/Object;Ljava/lang/String;D)D", 5);
			} else {
				code.addInvokeinterface(callback_type_index, "writeDouble",
				                        "(Ljava/lang/Object;Ljava/lang/String;DD)D", 7);
			}
		} else if (typeName.equals("F")) {
			// float
			if (isReadMethod) {
				code.addInvokeinterface(callback_type_index, "readFloat",
				                        "(Ljava/lang/Object;Ljava/lang/String;F)F", 4);
			} else {
				code.addInvokeinterface(callback_type_index, "writeFloat",
				                        "(Ljava/lang/Object;Ljava/lang/String;FF)F", 5);
			}
		} else if (typeName.equals("J")) {
			// long
			if (isReadMethod) {
				code.addInvokeinterface(callback_type_index, "readLong",
				                        "(Ljava/lang/Object;Ljava/lang/String;J)J", 5);
			} else {
				code.addInvokeinterface(callback_type_index, "writeLong",
				                        "(Ljava/lang/Object;Ljava/lang/String;JJ)J", 7);
			}
		} else {
			// bad type
			throw new RuntimeException("bad type: " + typeName);
		}
	}

	private static void addTypeDependDataLoad(Bytecode code, String typeName,
	                                          int i) {
		if ((typeName.charAt(0) == 'L')
		    && (typeName.charAt(typeName.length() - 1) == ';')
		    || (typeName.charAt(0) == '[')) {
			// reference type
			code.addAload(i);
		} else if (typeName.equals("Z") || typeName.equals("B")
		           || typeName.equals("C") || typeName.equals("I")
		           || typeName.equals("S")) {
			// boolean, byte, char, int, short
			code.addIload(i);
		} else if (typeName.equals("D")) {
			// double
			code.addDload(i);
		} else if (typeName.equals("F")) {
			// float
			code.addFload(i);
		} else if (typeName.equals("J")) {
			// long
			code.addLload(i);
		} else {
			// bad type
			throw new RuntimeException("bad type: " + typeName);
		}
	}

	private static void addTypeDependDataStore(Bytecode code, String typeName,
	                                           int i) {
		if ((typeName.charAt(0) == 'L')
		    && (typeName.charAt(typeName.length() - 1) == ';')
		    || (typeName.charAt(0) == '[')) {
			// reference type
			code.addAstore(i);
		} else if (typeName.equals("Z") || typeName.equals("B")
		           || typeName.equals("C") || typeName.equals("I")
		           || typeName.equals("S")) {
			// boolean, byte, char, int, short
			code.addIstore(i);
		} else if (typeName.equals("D")) {
			// double
			code.addDstore(i);
		} else if (typeName.equals("F")) {
			// float
			code.addFstore(i);
		} else if (typeName.equals("J")) {
			// long
			code.addLstore(i);
		} else {
			// bad type
			throw new RuntimeException("bad type: " + typeName);
		}
	}

	private static void addTypeDependDataReturn(Bytecode code, String typeName) {
		if ((typeName.charAt(0) == 'L')
		    && (typeName.charAt(typeName.length() - 1) == ';')
		    || (typeName.charAt(0) == '[')) {
			// reference type
			code.addOpcode(Opcode.ARETURN);
		} else if (typeName.equals("Z") || typeName.equals("B")
		           || typeName.equals("C") || typeName.equals("I")
		           || typeName.equals("S")) {
			// boolean, byte, char, int, short
			code.addOpcode(Opcode.IRETURN);
		} else if (typeName.equals("D")) {
			// double
			code.addOpcode(Opcode.DRETURN);
		} else if (typeName.equals("F")) {
			// float
			code.addOpcode(Opcode.FRETURN);
		} else if (typeName.equals("J")) {
			// long
			code.addOpcode(Opcode.LRETURN);
		} else {
			// bad type
			throw new RuntimeException("bad type: " + typeName);
		}
	}

}
