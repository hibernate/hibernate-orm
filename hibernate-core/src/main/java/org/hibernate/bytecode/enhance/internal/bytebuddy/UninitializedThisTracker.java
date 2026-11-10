package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.ArrayList;
import java.util.List;

import net.bytebuddy.jar.asm.Handle;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

/**
 * A {@link MethodVisitor} that tracks whether operand stack slots hold
 * {@code uninitializedThis} in a constructor body before {@code super()},
 * then delegates each instruction to the wrapped visitor.
 * <p>
 * Used during bytecode enhancement to determine whether {@code PUTFIELD}/{@code GETFIELD}
 * targets {@code this} (requiring raw field access, since {@code INVOKEVIRTUAL} is illegal
 * on {@code uninitializedThis}) or another fully-initialized object (where replacement
 * with {@code INVOKEVIRTUAL $$_hibernate_read/write_*} is safe).
 * <p>
 * Before delegating {@code visitFieldInsn}, the tracker computes
 * {@link #lastFieldTargetedThis()} so the wrapped visitor can read it.
 * Before delegating {@code visitMethodInsn}, it computes
 * {@link #lastMethodWasSuperCall()}.
 * <p>
 * If an unrecognized opcode or stack underflow is encountered, {@link #isTrackingLost()}
 * is set. The wrapped visitor decides the policy: throw an error (strict, the default)
 * or fall back to emitting raw field accesses (lenient).
 *
 * @see PersistentAttributeTransformer#wrapMethodWithPreSuperFieldAccess
 */
final class UninitializedThisTracker extends MethodVisitor {

	private int stackDepth;
	private final List<Integer> uninitThisPositions = new ArrayList<>( 2 );
	private int newObjectDepth;
	private boolean beforeSuperCall = true;
	private boolean trackingLost;

	private boolean lastFieldTargetedThis;
	private boolean lastMethodWasSuperCall;

	UninitializedThisTracker(int api, MethodVisitor mv) {
		super( api, mv );
	}

	boolean isBeforeSuperCall() {
		return beforeSuperCall;
	}

	boolean isTrackingLost() {
		return trackingLost;
	}

	boolean lastFieldTargetedThis() {
		return lastFieldTargetedThis;
	}

	boolean lastMethodWasSuperCall() {
		return lastMethodWasSuperCall;
	}

	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		if ( beforeSuperCall ) {
			trackVarInsn( opcode, varIndex );
		}
		super.visitVarInsn( opcode, varIndex );
	}

	@Override
	public void visitInsn(int opcode) {
		if ( beforeSuperCall ) {
			trackInsn( opcode );
		}
		super.visitInsn( opcode );
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		if ( beforeSuperCall ) {
			trackIntInsn( opcode );
		}
		super.visitIntInsn( opcode, operand );
	}

	@Override
	public void visitLdcInsn(Object value) {
		if ( beforeSuperCall && !trackingLost ) {
			push( value instanceof Long || value instanceof Double ? 2 : 1 );
		}
		super.visitLdcInsn( value );
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if ( beforeSuperCall ) {
			trackTypeInsn( opcode );
		}
		super.visitTypeInsn( opcode, type );
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if ( beforeSuperCall ) {
			trackJumpInsn( opcode );
		}
		super.visitJumpInsn( opcode, label );
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		if ( beforeSuperCall && !trackingLost ) {
			pop( 1 );
		}
		super.visitTableSwitchInsn( min, max, dflt, labels );
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if ( beforeSuperCall && !trackingLost ) {
			pop( 1 );
		}
		super.visitLookupSwitchInsn( dflt, keys, labels );
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if ( beforeSuperCall ) {
			var fieldSize = Type.getType( desc ).getSize();
			lastFieldTargetedThis = !trackingLost
					&& ( opcode == Opcodes.PUTFIELD || opcode == Opcodes.GETFIELD )
					&& isThisOnStack( opcode == Opcodes.PUTFIELD ? fieldSize : 0 );
			super.visitFieldInsn( opcode, owner, name, desc );
			trackFieldInsn( opcode, fieldSize );
		}
		else {
			lastFieldTargetedThis = false;
			super.visitFieldInsn( opcode, owner, name, desc );
		}
	}

	@Override
	public void visitMethodInsn(
			int opcode, String owner, String name,
			String descriptor, boolean isInterface) {
		lastMethodWasSuperCall = false;
		if ( beforeSuperCall ) {
			if ( !trackingLost ) {
				popMethodArgs( descriptor, opcode != Opcodes.INVOKESTATIC );
			}
			if ( opcode == Opcodes.INVOKESPECIAL && "<init>".equals( name ) ) {
				if ( newObjectDepth > 0 ) {
					newObjectDepth--;
				}
				else {
					beforeSuperCall = false;
					lastMethodWasSuperCall = true;
				}
			}
			else if ( !trackingLost ) {
				push( Type.getReturnType( descriptor ).getSize() );
			}
		}
		super.visitMethodInsn( opcode, owner, name, descriptor, isInterface );
	}

	@Override
	public void visitInvokeDynamicInsn(
			String name, String descriptor,
			Handle bsm, Object... bsmArgs) {
		if ( beforeSuperCall && !trackingLost ) {
			popMethodArgs( descriptor, false );
			push( Type.getReturnType( descriptor ).getSize() );
		}
		super.visitInvokeDynamicInsn( name, descriptor, bsm, bsmArgs );
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		if ( beforeSuperCall && !trackingLost ) {
			pop( numDimensions );
			push( 1 );
		}
		super.visitMultiANewArrayInsn( descriptor, numDimensions );
	}

	// -- internal tracking methods --

	private boolean isThisOnStack(int fromTop) {
		var position = stackDepth - 1 - fromTop;
		return position >= 0 && uninitThisPositions.contains( position );
	}

	private void trackVarInsn(int opcode, int varIndex) {
		if ( trackingLost ) {
			return;
		}
		switch ( opcode ) {
			case Opcodes.ALOAD -> {
				if ( varIndex == 0 ) {
					uninitThisPositions.add( stackDepth );
				}
				push( 1 );
			}
			case Opcodes.ILOAD, Opcodes.FLOAD -> push( 1 );
			case Opcodes.LLOAD, Opcodes.DLOAD -> push( 2 );
			case Opcodes.ASTORE, Opcodes.ISTORE, Opcodes.FSTORE -> pop( 1 );
			case Opcodes.LSTORE, Opcodes.DSTORE -> pop( 2 );
			default -> handleUnknown();
		}
	}

	private void trackInsn(int opcode) {
		if ( trackingLost ) {
			return;
		}
		var delta = insnStackDelta( opcode );
		if ( trackingLost ) {
			return;
		}
		if ( delta > 0 ) {
			push( delta );
		}
		else if ( delta < 0 ) {
			pop( -delta );
		}
	}

	private void trackIntInsn(int opcode) {
		if ( trackingLost ) {
			return;
		}
		switch ( opcode ) {
			case Opcodes.BIPUSH, Opcodes.SIPUSH -> push( 1 );
			case Opcodes.NEWARRAY -> { /* pop int, push array ref: no net change */ }
			default -> handleUnknown();
		}
	}

	private void trackTypeInsn(int opcode) {
		if ( opcode == Opcodes.NEW ) {
			newObjectDepth++;
			if ( !trackingLost ) {
				push( 1 );
			}
			return;
		}
		if ( trackingLost ) {
			return;
		}
		switch ( opcode ) {
			case Opcodes.ANEWARRAY -> { /* pop count, push array: no net change */ }
			case Opcodes.CHECKCAST -> { /* same ref, no change */ }
			case Opcodes.INSTANCEOF -> {
				pop( 1 );
				push( 1 );
			}
			default -> handleUnknown();
		}
	}

	private void trackJumpInsn(int opcode) {
		if ( trackingLost ) {
			return;
		}
		switch ( opcode ) {
			case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT,
					Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE,
					Opcodes.IFNULL, Opcodes.IFNONNULL -> pop( 1 );
			case Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE,
					Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE,
					Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE,
					Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE -> pop( 2 );
			case Opcodes.GOTO -> { /* no stack change */ }
			default -> handleUnknown();
		}
	}

	private void trackFieldInsn(int opcode, int fieldSize) {
		if ( trackingLost ) {
			return;
		}
		switch ( opcode ) {
			case Opcodes.PUTFIELD -> pop( fieldSize + 1 );
			case Opcodes.GETFIELD -> {
				pop( 1 );
				push( fieldSize );
			}
			case Opcodes.GETSTATIC -> push( fieldSize );
			case Opcodes.PUTSTATIC -> pop( fieldSize );
			default -> handleUnknown();
		}
	}

	private void popMethodArgs(String descriptor, boolean hasReceiver) {
		var argSlots = 0;
		for ( var argType : Type.getArgumentTypes( descriptor ) ) {
			argSlots += argType.getSize();
		}
		pop( argSlots );
		if ( hasReceiver ) {
			pop( 1 );
		}
	}

	private void push(int slots) {
		stackDepth += slots;
	}

	private void pop(int slots) {
		stackDepth -= slots;
		if ( stackDepth < 0 ) {
			stackDepth = 0;
			trackingLost = true;
			return;
		}
		uninitThisPositions.removeIf( p -> p >= stackDepth );
	}

	private int insnStackDelta(int opcode) {
		return switch ( opcode ) {
			case Opcodes.NOP -> 0;
			case Opcodes.ACONST_NULL, Opcodes.ICONST_M1,
					Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2,
					Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5,
					Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2 -> 1;
			case Opcodes.LCONST_0, Opcodes.LCONST_1,
					Opcodes.DCONST_0, Opcodes.DCONST_1 -> 2;
			case Opcodes.DUP, Opcodes.DUP_X1, Opcodes.DUP_X2 -> 1;
			case Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2 -> 2;
			case Opcodes.SWAP -> 0;
			case Opcodes.POP -> -1;
			case Opcodes.POP2 -> -2;
			case Opcodes.INEG, Opcodes.FNEG, Opcodes.LNEG, Opcodes.DNEG,
					Opcodes.I2F, Opcodes.F2I, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S,
					Opcodes.L2D, Opcodes.D2L, Opcodes.ARRAYLENGTH -> 0;
			case Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL, Opcodes.IDIV,
					Opcodes.IREM, Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR,
					Opcodes.IAND, Opcodes.IOR, Opcodes.IXOR,
					Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV,
					Opcodes.FREM, Opcodes.FCMPL, Opcodes.FCMPG,
					Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR -> -1;
			case Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV,
					Opcodes.LREM, Opcodes.LAND, Opcodes.LOR, Opcodes.LXOR,
					Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV,
					Opcodes.DREM -> -2;
			case Opcodes.LCMP, Opcodes.DCMPL, Opcodes.DCMPG -> -3;
			case Opcodes.I2L, Opcodes.I2D, Opcodes.F2L, Opcodes.F2D -> 1;
			case Opcodes.L2I, Opcodes.L2F, Opcodes.D2I, Opcodes.D2F -> -1;
			case Opcodes.AALOAD, Opcodes.IALOAD, Opcodes.FALOAD,
					Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD -> -1;
			case Opcodes.LALOAD, Opcodes.DALOAD -> 0;
			case Opcodes.AASTORE, Opcodes.IASTORE, Opcodes.FASTORE,
					Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE -> -3;
			case Opcodes.LASTORE, Opcodes.DASTORE -> -4;
			case Opcodes.ATHROW, Opcodes.MONITORENTER, Opcodes.MONITOREXIT -> -1;
			case Opcodes.IRETURN, Opcodes.FRETURN, Opcodes.ARETURN -> -1;
			case Opcodes.LRETURN, Opcodes.DRETURN -> -2;
			case Opcodes.RETURN -> 0;
			default -> {
				handleUnknown();
				yield 0;
			}
		};
	}

	private void handleUnknown() {
		trackingLost = true;
	}
}
