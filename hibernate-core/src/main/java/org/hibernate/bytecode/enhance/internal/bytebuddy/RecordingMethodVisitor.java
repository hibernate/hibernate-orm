package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.bytebuddy.jar.asm.Handle;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;

/**
 * A {@link MethodVisitor} that buffers all code-emitting instructions and replays them
 * in {@link #visitEnd()}, after {@link #getMaxLocals()} is known.
 * <p>
 * Metadata visits (annotations, parameters, attributes) pass through to the delegate
 * immediately. Subclasses override specific {@code visitXxx} methods to inject
 * transformation logic and call {@link #record(Consumer)} to buffer custom instructions.
 * <p>
 * Intended for use with {@code ClassWriter.COMPUTE_FRAMES}: stack map frames are
 * dropped during recording and recomputed by the {@code ClassWriter}.
 */
class RecordingMethodVisitor extends MethodVisitor {

	private final MethodVisitor delegate;
	private final List<Consumer<MethodVisitor>> recorded = new ArrayList<>();
	private int maxLocals;

	RecordingMethodVisitor(int api, MethodVisitor delegate) {
		super( api, delegate );
		this.delegate = delegate;
	}

	protected void record(Consumer<MethodVisitor> action) {
		recorded.add( action );
	}

	protected int getMaxLocals() {
		return maxLocals;
	}

	// --- Code-emitting methods: buffer instead of passing through ---

	@Override
	public void visitCode() {
		record( MethodVisitor::visitCode );
	}

	@Override
	public void visitInsn(int opcode) {
		record( mv -> mv.visitInsn( opcode ) );
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		record( mv -> mv.visitIntInsn( opcode, operand ) );
	}

	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		record( mv -> mv.visitVarInsn( opcode, varIndex ) );
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		record( mv -> mv.visitTypeInsn( opcode, type ) );
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		record( mv -> mv.visitFieldInsn( opcode, owner, name, desc ) );
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		record( mv -> mv.visitMethodInsn( opcode, owner, name, descriptor, isInterface ) );
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... bsmArgs) {
		record( mv -> mv.visitInvokeDynamicInsn( name, descriptor, bsm, bsmArgs ) );
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		record( mv -> mv.visitJumpInsn( opcode, label ) );
	}

	@Override
	public void visitLabel(Label label) {
		record( mv -> mv.visitLabel( label ) );
	}

	@Override
	public void visitLdcInsn(Object value) {
		record( mv -> mv.visitLdcInsn( value ) );
	}

	@Override
	public void visitIincInsn(int varIndex, int increment) {
		record( mv -> mv.visitIincInsn( varIndex, increment ) );
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		record( mv -> mv.visitTableSwitchInsn( min, max, dflt, labels ) );
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		record( mv -> mv.visitLookupSwitchInsn( dflt, keys, labels ) );
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		record( mv -> mv.visitMultiANewArrayInsn( descriptor, numDimensions ) );
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		record( mv -> mv.visitTryCatchBlock( start, end, handler, type ) );
	}

	@Override
	public void visitLocalVariable(
			String name, String descriptor, String signature,
			Label start, Label end, int index) {
		record( mv -> mv.visitLocalVariable( name, descriptor, signature, start, end, index ) );
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		record( mv -> mv.visitLineNumber( line, start ) );
	}

	@Override
	public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
		// Dropped: COMPUTE_FRAMES recomputes all stack map frames
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		this.maxLocals = maxLocals;
	}

	@Override
	public void visitEnd() {
		for ( var action : recorded ) {
			action.accept( delegate );
		}
		delegate.visitMaxs( 0, 0 );
		delegate.visitEnd();
	}
}
