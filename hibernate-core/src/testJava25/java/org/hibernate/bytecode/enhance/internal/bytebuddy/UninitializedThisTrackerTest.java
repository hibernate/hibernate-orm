package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.utility.OpenedClassReader;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link UninitializedThisTracker} correctly identifies whether
 * {@code PUTFIELD}/{@code GETFIELD} targets {@code this} or another object in
 * constructors compiled by javac 25+ with flexible constructor bodies (JEP 513).
 * <p>
 * Each test defines a small inner class with a specific constructor pattern,
 * reads the bytecode that javac produced, and asserts the tracker's results.
 */
public class UninitializedThisTrackerTest {

	record FieldAccess(String name, int opcode, boolean targetIsThis) {}

	@Test
	void simpleFieldWriteBeforeSuperIsThis() {
		var results = analyzeConstructor(
				SimpleFieldWrite.class,
				Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ) )
		);
		assertThat( results ).containsExactly(
				new FieldAccess( "title", Opcodes.PUTFIELD, true )
		);
	}

	@Test
	void otherObjectFieldWriteBeforeSuperIsNotThis() {
		var results = analyzeConstructor(
				OtherObjectFieldWrite.class,
				Type.getMethodDescriptor(
						Type.VOID_TYPE,
						Type.getType( OtherObjectFieldWrite.class ),
						Type.getType( String.class )
				)
		);
		assertThat( results ).containsExactly(
				new FieldAccess( "title", Opcodes.PUTFIELD, false )
		);
	}

	@Test
	void multipleFieldWritesBeforeSuperAreThis() {
		var results = analyzeConstructor(
				MultiFieldWrite.class,
				Type.getMethodDescriptor(
						Type.VOID_TYPE,
						Type.getType( String.class ),
						Type.INT_TYPE
				)
		);
		assertThat( results ).containsExactly(
				new FieldAccess( "title", Opcodes.PUTFIELD, true ),
				new FieldAccess( "count", Opcodes.PUTFIELD, true )
		);
	}

	@Test
	void conditionalFieldWriteBeforeSuperIsThis() {
		var results = analyzeConstructor(
				ConditionalFieldWrite.class,
				Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ) )
		);
		assertThat( results ).containsExactly(
				new FieldAccess( "title", Opcodes.PUTFIELD, true )
		);
	}

	@Test
	void computedValueFieldWriteBeforeSuperIsThis() {
		var results = analyzeConstructor(
				ComputedValueFieldWrite.class,
				Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ) )
		);
		assertThat( results ).containsExactly(
				new FieldAccess( "title", Opcodes.PUTFIELD, true )
		);
	}

	@Test
	void newObjectInValueBeforeSuperIsThis() {
		var results = analyzeConstructor(
				NewObjectInValue.class,
				Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ) )
		);
		assertThat( results ).containsExactly(
				new FieldAccess( "title", Opcodes.PUTFIELD, true )
		);
	}

	@Test
	void conventionalConstructorHasNoPreSuperFieldAccess() {
		var results = analyzeConstructor(
				ConventionalConstructor.class,
				Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ) )
		);
		assertThat( results ).isEmpty();
	}

	@Test
	void mixedThisAndOtherFieldWritesBeforeSuper() {
		var results = analyzeConstructor(
				MixedThisAndOtherWrite.class,
				Type.getMethodDescriptor(
						Type.VOID_TYPE,
						Type.getType( MixedThisAndOtherWrite.class ),
						Type.getType( String.class )
				)
		);
		assertThat( results ).containsExactly(
				new FieldAccess( "title", Opcodes.PUTFIELD, true ),
				new FieldAccess( "title", Opcodes.PUTFIELD, false )
		);
	}

	// --- Test entities with flexible constructors ---

	public static class SimpleFieldWrite {
		String title;

		public SimpleFieldWrite(String title) {
			this.title = title;
			super();
		}
	}

	public static class OtherObjectFieldWrite {
		String title;

		public OtherObjectFieldWrite() {
		}

		public OtherObjectFieldWrite(OtherObjectFieldWrite other, String title) {
			other.title = title;
			super();
		}
	}

	public static class MultiFieldWrite {
		String title;
		int count;

		public MultiFieldWrite(String title, int count) {
			this.title = title;
			this.count = count;
			super();
		}
	}

	public static class ConditionalFieldWrite {
		String title;

		public ConditionalFieldWrite(String title) {
			if ( title != null ) {
				this.title = title;
			}
			super();
		}
	}

	public static class ComputedValueFieldWrite {
		String title;

		public ComputedValueFieldWrite(String title) {
			this.title = title.trim();
			super();
		}
	}

	public static class NewObjectInValue {
		String title;

		public NewObjectInValue(String title) {
			this.title = new String( title );
			super();
		}
	}

	public static class ConventionalConstructor {
		String title;

		public ConventionalConstructor(String title) {
			super();
			this.title = title;
		}
	}

	public static class MixedThisAndOtherWrite {
		String title;

		public MixedThisAndOtherWrite() {
		}

		public MixedThisAndOtherWrite(MixedThisAndOtherWrite other, String title) {
			this.title = title;
			other.title = title;
			super();
		}
	}

	// --- Test infrastructure ---

	private static List<FieldAccess> analyzeConstructor(
			Class<?> clazz, String constructorDescriptor) {
		var results = new ArrayList<FieldAccess>();
		try ( var is = clazz.getClassLoader().getResourceAsStream(
				clazz.getName().replace( '.', '/' ) + ".class" ) ) {
			if ( is == null ) {
				throw new AssertionError( "Could not load bytecode for " + clazz.getName() );
			}
			var reader = new ClassReader( is );
			reader.accept( new ClassVisitor( OpenedClassReader.ASM_API ) {
				@Override
				public MethodVisitor visitMethod(
						int access, String name, String descriptor,
						String signature, String[] exceptions) {
					if ( "<init>".equals( name )
							&& constructorDescriptor.equals( descriptor ) ) {
						var capture = new FieldAccessCapture( results );
						var tracker = new UninitializedThisTracker(
								OpenedClassReader.ASM_API, capture );
						capture.tracker = tracker;
						return tracker;
					}
					return null;
				}
			}, 0 );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
		return results;
	}

	private static class FieldAccessCapture extends MethodVisitor {
		final List<FieldAccess> results;
		UninitializedThisTracker tracker;

		FieldAccessCapture(List<FieldAccess> results) {
			super( OpenedClassReader.ASM_API );
			this.results = results;
		}

		@Override
		public void visitFieldInsn(
				int opcode, String owner, String name, String desc) {
			if ( tracker.isBeforeSuperCall()
					&& ( opcode == Opcodes.PUTFIELD
							|| opcode == Opcodes.GETFIELD ) ) {
				results.add( new FieldAccess(
						name, opcode, tracker.lastFieldTargetedThis() ) );
			}
		}
	}
}
