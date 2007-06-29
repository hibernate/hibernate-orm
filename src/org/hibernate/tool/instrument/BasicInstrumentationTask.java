package org.hibernate.tool.instrument;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.hibernate.bytecode.util.ClassDescriptor;
import org.hibernate.bytecode.util.ByteCodeHelper;
import org.hibernate.bytecode.util.FieldFilter;
import org.hibernate.bytecode.ClassTransformer;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.CRC32;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;

/**
 * Super class for all Hibernate instrumentation tasks.  Provides the basic
 * templating of how instrumentation should occur.
 *
 * @author Steve Ebersole
 */
public abstract class BasicInstrumentationTask extends Task {

	private static final int ZIP_MAGIC = 0x504B0304;
	private static final int CLASS_MAGIC = 0xCAFEBABE;

	protected final Logger logger = new Logger();
	private List filesets = new ArrayList();
	private Set classNames = new HashSet();
	private boolean extended;
	private boolean verbose;

	public void addFileset(FileSet set) {
		this.filesets.add( set );
	}

	protected final Iterator filesets() {
		return filesets.iterator();
	}

	public boolean isExtended() {
		return extended;
	}

	public void setExtended(boolean extended) {
		this.extended = extended;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void execute() throws BuildException {
		if ( isExtended() ) {
			collectClassNames();
		}
		logger.info( "starting instrumentation" );
		Project project = getProject();
		Iterator filesets = filesets();
		while ( filesets.hasNext() ) {
			FileSet fs = ( FileSet ) filesets.next();
			DirectoryScanner ds = fs.getDirectoryScanner( project );
			String[] includedFiles = ds.getIncludedFiles();
			File d = fs.getDir( project );
			for ( int i = 0; i < includedFiles.length; ++i ) {
				File file = new File( d, includedFiles[i] );
				try {
					processFile( file );
				}
				catch ( Exception e ) {
					throw new BuildException( e );
				}
			}
		}
	}

	private void collectClassNames() {
		logger.info( "collecting class names for extended instrumentation determination" );
		Project project = getProject();
		Iterator filesets = filesets();
		while ( filesets.hasNext() ) {
			FileSet fs = ( FileSet ) filesets.next();
			DirectoryScanner ds = fs.getDirectoryScanner( project );
			String[] includedFiles = ds.getIncludedFiles();
			File d = fs.getDir( project );
			for ( int i = 0; i < includedFiles.length; ++i ) {
				File file = new File( d, includedFiles[i] );
				try {
					collectClassNames( file );
				}
				catch ( Exception e ) {
					throw new BuildException( e );
				}
			}
		}
		logger.info( classNames.size() + " class(es) being checked" );
	}

	private void collectClassNames(File file) throws Exception {
	    if ( isClassFile( file ) ) {
			byte[] bytes = ByteCodeHelper.readByteCode( file );
			ClassDescriptor descriptor = getClassDescriptor( bytes );
		    classNames.add( descriptor.getName() );
	    }
	    else if ( isJarFile( file ) ) {
		    ZipEntryHandler collector = new ZipEntryHandler() {
			    public void handleEntry(ZipEntry entry, byte[] byteCode) throws Exception {
					if ( !entry.isDirectory() ) {
						// see if the entry represents a class file
						DataInputStream din = new DataInputStream( new ByteArrayInputStream( byteCode ) );
						if ( din.readInt() == CLASS_MAGIC ) {
				            classNames.add( getClassDescriptor( byteCode ).getName() );
						}
					}
			    }
		    };
		    ZipFileProcessor processor = new ZipFileProcessor( collector );
		    processor.process( file );
	    }
	}

	protected void processFile(File file) throws Exception {
	    if ( isClassFile( file ) ) {
	        processClassFile(file);
	    }
	    else if ( isJarFile( file ) ) {
	        processJarFile(file);
	    }
	    else {
		    logger.verbose( "ignoring " + file.toURL() );

	    }
	}

	protected final boolean isClassFile(File file) throws IOException {
        return checkMagic( file, CLASS_MAGIC );
    }

    protected final boolean isJarFile(File file) throws IOException {
        return checkMagic(file, ZIP_MAGIC);
    }

	protected final boolean checkMagic(File file, long magic) throws IOException {
        DataInputStream in = new DataInputStream( new FileInputStream( file ) );
        try {
            int m = in.readInt();
            return magic == m;
        }
        finally {
            in.close();
        }
    }

	protected void processClassFile(File file) throws Exception {
		logger.verbose( "Starting class file : " + file.toURL() );
		byte[] bytes = ByteCodeHelper.readByteCode( file );
		ClassDescriptor descriptor = getClassDescriptor( bytes );
		ClassTransformer transformer = getClassTransformer( descriptor );
		if ( transformer == null ) {
			logger.verbose( "skipping file : " + file.toURL() );
			return;
		}

		logger.info( "processing class [" + descriptor.getName() + "]; file = " + file.toURL() );
		byte[] transformedBytes = transformer.transform(
				getClass().getClassLoader(),
				descriptor.getName(),
				null,
				null,
				descriptor.getBytes()
		);

		OutputStream out = new FileOutputStream( file );
		try {
			out.write( transformedBytes );
			out.flush();
		}
		finally {
			try {
				out.close();
			}
			catch ( IOException ignore) {
				// intentionally empty
			}
		}
	}

	protected void processJarFile(final File file) throws Exception {
		logger.verbose( "starting jar file : " + file.toURL() );

        File tempFile = File.createTempFile(
		        file.getName(),
		        null,
		        new File( file.getAbsoluteFile().getParent() )
        );

        try {
			FileOutputStream fout = new FileOutputStream( tempFile, false );
			try {
				final ZipOutputStream out = new ZipOutputStream( fout );
				ZipEntryHandler transformer = new ZipEntryHandler() {
					public void handleEntry(ZipEntry entry, byte[] byteCode) throws Exception {
								logger.verbose( "starting entry : " + entry.toString() );
								if ( !entry.isDirectory() ) {
									// see if the entry represents a class file
									DataInputStream din = new DataInputStream( new ByteArrayInputStream( byteCode ) );
									if ( din.readInt() == CLASS_MAGIC ) {
										ClassDescriptor descriptor = getClassDescriptor( byteCode );
										ClassTransformer transformer = getClassTransformer( descriptor );
										if ( transformer == null ) {
											logger.verbose( "skipping entry : " + entry.toString() );
										}
										else {
											logger.info( "processing class [" + descriptor.getName() + "]; entry = " + file.toURL() );
											byteCode = transformer.transform(
													getClass().getClassLoader(),
													descriptor.getName(),
													null,
													null,
													descriptor.getBytes()
											);
										}
									}
									else {
										logger.verbose( "ignoring zip entry : " + entry.toString() );
									}
								}

								ZipEntry outEntry = new ZipEntry( entry.getName() );
								outEntry.setMethod( entry.getMethod() );
								outEntry.setComment( entry.getComment() );
								outEntry.setSize( byteCode.length );

								if ( outEntry.getMethod() == ZipEntry.STORED ){
									CRC32 crc = new CRC32();
									crc.update( byteCode );
									outEntry.setCrc( crc.getValue() );
									outEntry.setCompressedSize( byteCode.length );
								}
								out.putNextEntry( outEntry );
								out.write( byteCode );
								out.closeEntry();
					}
				};
				ZipFileProcessor processor = new ZipFileProcessor( transformer );
				processor.process( file );
				out.close();
			}
			finally{
				fout.close();
			}

            if ( file.delete() ) {
	            File newFile = new File( tempFile.getAbsolutePath() );
                if( !newFile.renameTo( file ) ) {
	                throw new IOException( "can not rename " + tempFile + " to " + file );
                }
            }
            else {
	            throw new IOException("can not delete " + file);
            }
        }
        finally {
	        tempFile.delete();
        }
	}

	protected boolean isBeingIntrumented(String className) {
		logger.verbose( "checking to see if class [" + className + "] is set to be instrumented" );
		return classNames.contains( className );
	}

	protected abstract ClassDescriptor getClassDescriptor(byte[] byecode) throws Exception;

	protected abstract ClassTransformer getClassTransformer(ClassDescriptor descriptor);

	protected class CustomFieldFilter implements FieldFilter {
		private final ClassDescriptor descriptor;

		public CustomFieldFilter(ClassDescriptor descriptor) {
			this.descriptor = descriptor;
		}

		public boolean shouldInstrumentField(String className, String fieldName) {
			if ( descriptor.getName().equals( className ) ) {
				logger.verbose( "accepting transformation of field [" + className + "." + fieldName + "]" );
				return true;
			}
			else {
				logger.verbose( "not accepting transformation of field [" + className + "." + fieldName + "]" );
				return false;
			}
		}

		public boolean shouldTransformFieldAccess(
				String transformingClassName,
				String fieldOwnerClassName,
				String fieldName) {
			if ( descriptor.getName().equals( fieldOwnerClassName ) ) {
				logger.verbose( "accepting transformation of field access [" + fieldOwnerClassName + "." + fieldName + "]" );
				return true;
			}
			else if ( isExtended() && isBeingIntrumented( fieldOwnerClassName ) ) {
				logger.verbose( "accepting extended transformation of field access [" + fieldOwnerClassName + "." + fieldName + "]" );
				return true;
			}
			else {
				logger.verbose( "not accepting transformation of field access [" + fieldOwnerClassName + "." + fieldName + "]" );
				return false;
			}
		}
	}

	protected class Logger {
		public void verbose(String message) {
			if ( verbose ) {
				System.out.println( message );
			}
			else {
				log( message, Project.MSG_VERBOSE );
			}
		}

		public void debug(String message) {
			log( message, Project.MSG_DEBUG );
		}

		public void info(String message) {
			log( message, Project.MSG_INFO );
		}

		public void warn(String message) {
			log( message, Project.MSG_WARN );
		}
	}


	private static interface ZipEntryHandler {
		public void handleEntry(ZipEntry entry, byte[] byteCode) throws Exception;
	}

	private static class ZipFileProcessor {
		private final ZipEntryHandler entryHandler;

		public ZipFileProcessor(ZipEntryHandler entryHandler) {
			this.entryHandler = entryHandler;
		}

		public void process(File file) throws Exception {
			ZipInputStream zip = new ZipInputStream( new FileInputStream( file ) );

			try {
				ZipEntry entry;
				while ( (entry = zip.getNextEntry()) != null ) {
					byte bytes[] = ByteCodeHelper.readByteCode( zip );
					entryHandler.handleEntry( entry, bytes );
					zip.closeEntry();
				}
            }
            finally {
	            zip.close();
            }
		}
	}
}
