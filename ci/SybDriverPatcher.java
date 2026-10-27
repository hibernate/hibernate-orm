import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import java.util.zip.*;
import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ConstantDescs;

public class SybDriverPatcher {
	private static final String TARGET = "com/sybase/jdbc4/jdbc/SybDriver.class";
	private static final String FLAG = "_isDriverRegistered";

	private static byte[] patch(byte[] bytes) {
		ClassFile cf = ClassFile.of();
		ClassModel model = cf.parse( bytes );

		boolean hasFlag = model.fields().stream()
				.anyMatch( f -> f.fieldName().stringValue().equals( FLAG ) );

		boolean[] patched = {false};

		ClassTransform transform = new ClassTransform() {
			@Override
			public void accept(ClassBuilder cb, ClassElement e) {
				if ( e instanceof MethodModel m
					 && m.methodName().stringValue().equals( "<init>" ) ) {

					cb.transformMethod( m, (mb, me) -> {
						if ( me instanceof CodeModel code ) {
							mb.transformCode( code, (b, ce) -> {
								if ( ce instanceof InvokeInstruction i
									 && i.name().stringValue().equals( "registerWithDriverManager" )
									 && i.typeSymbol().descriptorString().equals( "()V" )
									 && i.opcode() == Opcode.INVOKEVIRTUAL ) {

									// Save the receiver already on stack.
									int receiver = b.allocateLocal( TypeKind.REFERENCE );
									Label skip = b.newLabel();

									b.astore( receiver );
									b.getstatic(
											model.thisClass().asSymbol(),
											FLAG,
											ConstantDescs.CD_boolean
									);
									b.ifne( skip );

									// Set the guard before invoking.
									b.iconst_1();
									b.putstatic(
											model.thisClass().asSymbol(),
											FLAG,
											ConstantDescs.CD_boolean
									);

									b.aload( receiver );
									b.with( ce );
									b.labelBinding( skip );
									patched[0] = true;
								}
								else {
									b.with( ce );
								}
							} );
						}
						else {
							mb.with( me );
						}
					} );
				}
				else {
					cb.with( e );
				}
			}

			@Override
			public void atEnd(ClassBuilder cb) {
				if ( !hasFlag ) {
					cb.withField(
							FLAG,
							ConstantDescs.CD_boolean,
							ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC
					);
				}
			}
		};

		byte[] result = cf.transformClass( model, transform );

		if ( !patched[0] ) {
			throw new IllegalStateException(
					"No matching registerWithDriverManager()V call found" );
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		if ( args.length != 2 ) {
			System.err.println(
					"Usage: java SybDriverPatcher.java input.jar output.jar" );
			System.exit( 2 );
		}

		Path input = Path.of( args[0] );
		Path output = Path.of( args[1] );
		boolean[] found = {false};

		try (JarFile jar = new JarFile( input.toFile() );
			 OutputStream fileOut = Files.newOutputStream( output );
			 ZipOutputStream out = new ZipOutputStream( fileOut )) {

			var entries = jar.entries();
			while ( entries.hasMoreElements() ) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();

				ZipEntry copy = new ZipEntry( name );
				copy.setTime( entry.getTime() );
				out.putNextEntry( copy );

				try (InputStream in = jar.getInputStream( entry )) {
					if ( name.equals( TARGET ) ) {
						out.write( patch( in.readAllBytes() ) );
						found[0] = true;
					}
					else {
						in.transferTo( out );
					}
				}
				out.closeEntry();
			}
		}

		if ( !found[0] ) {
			Files.deleteIfExists( output );
			throw new FileNotFoundException( "Target class not found: " + TARGET );
		}

		System.out.println( "Patched JAR: " + output );
	}
}
