/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.identity.HANAIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.ConfigurationService.Converter;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.naming.Identifier;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.spi.AnsiTrimFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.ConcatFunctionTemplate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.AbstractJdbcValueBinder;
import org.hibernate.sql.AbstractJdbcValueExtractor;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorHANADatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.java.internal.LobStreamDataHelper;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.AbstractTemplateSqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.BitSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BlobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BooleanSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.CharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.sql.spi.NCharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.DecimalSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.DoubleSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NVarcharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SmallIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * An abstract base class for SAP HANA dialects.
 * <p>
 * For more information on interacting with the SAP HANA database, refer to the
 * <a href="https://help.sap.com/viewer/4fe29514fd584807ac9f2a04f6754767/">SAP HANA SQL and System Views Reference</a>
 * and the <a href=
 * "https://help.sap.com/viewer/0eec0d68141541d1b07893a39944924e/latest/en-US/434e2962074540e18c802fd478de86d6.html">SAP
 * HANA Client Interface Programming Reference</a>.
 * <p>
 * Note: This dialect is configured to create foreign keys with {@code on update cascade}.
 *
 * @author Andrew Clemons <andrew.clemons@sap.com>
 * @author Jonathan Bregler <jonathan.bregler@sap.com>
 */
public abstract class AbstractHANADialect extends Dialect {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractHANADialect.class );

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			return sql + ( hasOffset ? " limit ? offset ?" : " limit ?" );
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean bindLimitParametersInReverseOrder() {
			return true;
		}

	};

	private static class CloseSuppressingReader extends FilterReader {
		protected CloseSuppressingReader(final Reader in) {
			super( in );
		}

		@Override
		public void close() {
			// do not close
		}
	}

	private static class CloseSuppressingInputStream extends FilterInputStream {

		protected CloseSuppressingInputStream(final InputStream in) {
			super( in );
		}

		@Override
		public void close() {
			// do not close
		}
	}

	private static class MaterializedBlob implements Blob {

		private byte[] bytes = null;

		public MaterializedBlob(byte[] bytes) {
			this.setBytes( bytes );
		}

		@Override
		public long length() {
			return this.getBytes().length;
		}

		@Override
		public byte[] getBytes(long pos, int length) {
			return Arrays.copyOfRange( this.bytes, (int) ( pos - 1 ), (int) ( pos - 1 + length ) );
		}

		@Override
		public InputStream getBinaryStream() {
			return new ByteArrayInputStream( this.getBytes() );
		}

		@Override
		public long position(byte[] pattern, long start) throws SQLException {
			throw new SQLFeatureNotSupportedException();
		}

		@Override
		public long position(Blob pattern, long start) throws SQLException {
			throw new SQLFeatureNotSupportedException();
		}

		@Override
		public int setBytes(long pos, byte[] bytes) {
			int bytesSet = 0;
			if ( this.bytes.length < pos - 1 + bytes.length ) {
				this.bytes = Arrays.copyOf( this.bytes, (int) ( pos - 1 + bytes.length ) );
			}
			for ( int i = 0; i < bytes.length && i < this.bytes.length; i++, bytesSet++ ) {
				this.bytes[(int) ( i + pos - 1 )] = bytes[i];
			}
			return bytesSet;
		}

		@Override
		public int setBytes(long pos, byte[] bytes, int offset, int len) {
			int bytesSet = 0;
			if ( this.bytes.length < pos - 1 + len ) {
				this.bytes = Arrays.copyOf( this.bytes, (int) ( pos - 1 + len ) );
			}
			for ( int i = offset; i < len && i < this.bytes.length; i++, bytesSet++ ) {
				this.bytes[(int) ( i + pos - 1 )] = bytes[i];
			}
			return bytesSet;
		}

		@Override
		public OutputStream setBinaryStream(long pos) {
			return new ByteArrayOutputStream() {

				{
					this.buf = getBytes();
				}
			};
		}

		@Override
		public void truncate(long len) {
			this.setBytes( Arrays.copyOf( this.getBytes(), (int) len ) );
		}

		@Override
		public void free() {
			this.setBytes( null );
		}

		@Override
		public InputStream getBinaryStream(long pos, long length) {
			return new ByteArrayInputStream( this.getBytes(), (int) ( pos - 1 ), (int) length );
		}

		byte[] getBytes() {
			return this.bytes;
		}

		void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}

	}

	private static class MaterializedNClob implements NClob {

		private String data;

		public MaterializedNClob(String data) {
			this.data = data;
		}

		@Override
		public void truncate(long len) {
			this.data = new String();
		}

		@Override
		public int setString(long pos, String str, int offset, int len) {
			this.data = this.data.substring( 0, (int) ( pos - 1 ) ) + str.substring( offset, offset + len )
					+ this.data.substring( (int) ( pos - 1 + len ) );
			return len;
		}

		@Override
		public int setString(long pos, String str) {
			this.data = this.data.substring( 0, (int) ( pos - 1 ) ) + str + this.data.substring( (int) ( pos - 1 + str.length() ) );
			return str.length();
		}

		@Override
		public Writer setCharacterStream(long pos) throws SQLException {
			throw new SQLFeatureNotSupportedException();
		}

		@Override
		public OutputStream setAsciiStream(long pos) throws SQLException {
			throw new SQLFeatureNotSupportedException();
		}

		@Override
		public long position(Clob searchstr, long start) {
			return this.data.indexOf( LobStreamDataHelper.extractString( searchstr ), (int) ( start - 1 ) );
		}

		@Override
		public long position(String searchstr, long start) {
			return this.data.indexOf( searchstr, (int) ( start - 1 ) );
		}

		@Override
		public long length() {
			return this.data.length();
		}

		@Override
		public String getSubString(long pos, int length) {
			return this.data.substring( (int) ( pos - 1 ), (int) ( pos - 1 + length ) );
		}

		@Override
		public Reader getCharacterStream(long pos, long length) {
			return new StringReader( this.data.substring( (int) ( pos - 1 ), (int) ( pos - 1 + length ) ) );
		}

		@Override
		public Reader getCharacterStream() {
			return new StringReader( this.data );
		}

		@Override
		public InputStream getAsciiStream() {
			return new ByteArrayInputStream( this.data.getBytes( StandardCharsets.ISO_8859_1 ) );
		}

		@Override
		public void free() {
			this.data = null;
		}
	}

	private static class HANAStreamBlobTypeDescriptor extends AbstractTemplateSqlTypeDescriptor {

		private static final long serialVersionUID = -2476600722093442047L;

		final int maxLobPrefetchSize;

		public HANAStreamBlobTypeDescriptor(int maxLobPrefetchSize) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
		}

		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getOrMakeJavaDescriptor( Clob.class );
		}

		@Override
		public int getJdbcTypeCode() {
			return Types.BLOB;
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		protected <X> JdbcValueBinder<X> createBinder(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					final BinaryStream binaryStream = javaTypeDescriptor.unwrap( value, BinaryStream.class, executionContext.getSession() );
					if ( value instanceof BlobImplementer ) {
						try ( InputStream is = new CloseSuppressingInputStream( binaryStream.getInputStream() ) ) {
							st.setBinaryStream( index, is, binaryStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setBinaryStream( index, binaryStream.getInputStream(), binaryStream.getLength() );
					}
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					final BinaryStream binaryStream = javaTypeDescriptor.unwrap( value, BinaryStream.class, executionContext.getSession() );
					if ( value instanceof BlobImplementer ) {
						try ( InputStream is = new CloseSuppressingInputStream( binaryStream.getInputStream() ) ) {
							st.setBinaryStream( name, is, binaryStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setBinaryStream( name, binaryStream.getInputStream(), binaryStream.getLength() );
					}
				}
			};
		}

		@Override
		protected <X> JdbcValueExtractor<X> createExtractor(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException {
					Blob rsBlob = rs.getBlob( position );
					if ( rsBlob == null || rsBlob.length() < HANAStreamBlobTypeDescriptor.this.maxLobPrefetchSize ) {
						return javaTypeDescriptor.wrap( rsBlob, executionContext.getSession() );
					}
					Blob blob = new MaterializedBlob( LobStreamDataHelper.extractBytes( rsBlob.getBinaryStream() ) );
					return javaTypeDescriptor.wrap( blob, executionContext.getSession() );
				}

				@Override
				protected X doExtract(CallableStatement statement, int position, ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getBlob( position ), executionContext.getSession() );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getBlob( name ), executionContext.getSession() );
				}
			};
		}

		@Override
		public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
			// literal values for BLOB data is not supported.
			return null;
		}
	}

	// the ClobJavaDescriptor and NClobSqlDescriptor for HANA are slightly
	// changed from the standard ones. The HANA JDBC driver currently closes any
	// stream passed in via
	// PreparedStatement.setCharacterStream(int,Reader,long)
	// after the stream has been processed. this causes problems later if we are
	// using non-contexual lob creation and HANA then closes our StringReader.
	// see test case LobLocatorTest

	private static class HANAClobTypeDescriptor<T> extends ClobSqlDescriptor {

		/** serial version uid. */
		private static final long serialVersionUID = -379042275442752102L;

		final int maxLobPrefetchSize;
		final boolean useUnicodeStringTypes;

		public HANAClobTypeDescriptor(int maxLobPrefetchSize, boolean useUnicodeStringTypes) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
			this.useUnicodeStringTypes = useUnicodeStringTypes;
		}

		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getOrMakeJavaDescriptor( Clob.class );
		}


		@Override
		public <X> JdbcValueBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class, executionContext.getSession() );

					if ( value instanceof ClobImplementer ) {
						try ( Reader r = new CloseSuppressingReader( characterStream.asReader() ) ) {
							st.setCharacterStream( index, r, characterStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
					}

				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class, executionContext.getSession() );

					if ( value instanceof ClobImplementer ) {
						try ( Reader r = new CloseSuppressingReader( characterStream.asReader() ) ) {
							st.setCharacterStream( name, r, characterStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
					}
				}
			};
		}

		@Override
		protected <X> JdbcValueExtractor<X> createExtractor(
				BasicJavaDescriptor<X> javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(
						ResultSet rs,
						int position,
						ExecutionContext executionContext) throws SQLException {
					Clob rsClob;
					if ( HANAClobTypeDescriptor.this.useUnicodeStringTypes ) {
						rsClob = rs.getNClob( position );
					}
					else {
						rsClob = rs.getClob( position );
					}

					if ( rsClob == null || rsClob.length() < HANAClobTypeDescriptor.this.maxLobPrefetchSize ) {
						return javaTypeDescriptor.wrap( rsClob, executionContext.getSession() );
					}

					final Clob clob = new MaterializedNClob( LobStreamDataHelper.extractString( rsClob ) );
					return javaTypeDescriptor.wrap( clob, executionContext.getSession() );
				}

				@Override
				protected X doExtract(
						CallableStatement statement,
						int position,
						ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getClob( position ), executionContext.getSession() );
				}

				@Override
				protected X doExtract(
						CallableStatement statement,
						String name,
						ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getClob( name ), executionContext.getSession() );
				}
			};
		}

		public int getMaxLobPrefetchSize() {
			return this.maxLobPrefetchSize;
		}

		public boolean isUseUnicodeStringTypes() {
			return this.useUnicodeStringTypes;
		}

	}

	private static class HANANClobSqlDescriptor extends NClobSqlDescriptor {

		/** serial version uid. */
		private static final long serialVersionUID = 5651116091681647859L;

		final int maxLobPrefetchSize;

		public HANANClobSqlDescriptor(int maxLobPrefetchSize) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
		}

		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getOrMakeJavaDescriptor( Clob.class );
		}

		@Override
		public <X> JdbcValueBinder<X> getNClobBinder(
				final JavaTypeDescriptor<X> javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {

				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class, executionContext.getSession() );

					if ( value instanceof NClobImplementer ) {
						try ( Reader r = new CloseSuppressingReader( characterStream.asReader() ) ) {
							st.setCharacterStream( index, r, characterStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
					}
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap(
							value,
							CharacterStream.class,
							executionContext.getSession()
					);

					if ( value instanceof NClobImplementer ) {
						try ( Reader r = new CloseSuppressingReader( characterStream.asReader() ) ) {
							st.setCharacterStream( name, r, characterStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
					}
				}
			};
		}

		@Override
		protected <X> JdbcValueExtractor<X> createExtractor(
				BasicJavaDescriptor<X> javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(
						ResultSet rs,
						int position,
						ExecutionContext executionContext) throws SQLException {
					NClob rsNClob = rs.getNClob( position );
					if ( rsNClob == null || rsNClob.length() < HANANClobSqlDescriptor.this.maxLobPrefetchSize ) {
						return javaTypeDescriptor.wrap( rsNClob, executionContext.getSession() );
					}
					NClob nClob = new MaterializedNClob( LobStreamDataHelper.extractString( rsNClob ) );
					return javaTypeDescriptor.wrap( nClob, executionContext.getSession() );
				}

				@Override
				protected X doExtract(
						CallableStatement statement,
						int position,
						ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getNClob( position ), executionContext.getSession() );
				}

				@Override
				protected X doExtract(
						CallableStatement statement,
						String name,
						ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getNClob( name ), executionContext.getSession() );
				}
			};
		}

		public int getMaxLobPrefetchSize() {
			return this.maxLobPrefetchSize;
		}

		@Override
		public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
			// literal values for (N)CLOB data is not supported.
			return null;
		}
	}

	public static class HANABlobTypeDescriptor extends AbstractTemplateSqlTypeDescriptor {

		private static final long serialVersionUID = 5874441715643764323L;

		final int maxLobPrefetchSize;

		final HANAStreamBlobTypeDescriptor hanaStreamBlobTypeDescriptor;

		public HANABlobTypeDescriptor(int maxLobPrefetchSize) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
			this.hanaStreamBlobTypeDescriptor = new HANAStreamBlobTypeDescriptor( maxLobPrefetchSize );
		}

		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getOrMakeJavaDescriptor( Clob.class );
		}

		@Override
		public int getJdbcTypeCode() {
			return Types.BLOB;
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		protected <X> JdbcValueExtractor<X> createExtractor(
				BasicJavaDescriptor<X> javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(
						ResultSet rs,
						int position,
						ExecutionContext executionContext) throws SQLException {
					Blob rsBlob = rs.getBlob( position );
					if ( rsBlob == null || rsBlob.length() < HANABlobTypeDescriptor.this.maxLobPrefetchSize ) {
						return javaTypeDescriptor.wrap( rsBlob, executionContext.getSession() );
					}
					Blob blob = new MaterializedBlob( LobStreamDataHelper.extractBytes( rsBlob.getBinaryStream() ) );
					return javaTypeDescriptor.wrap( blob, executionContext.getSession() );
				}

				@Override
				protected X doExtract(
						CallableStatement statement,
						int position,
						ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getBlob( position ), executionContext.getSession() );
				}

				@Override
				protected X doExtract(
						CallableStatement statement,
						String name,
						ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getBlob( name ), executionContext.getSession() );
				}
			};
		}

		@Override
		protected <X> JdbcValueBinder<X> createBinder(
				BasicJavaDescriptor<X> javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {

				@Override
				@SuppressWarnings("unchecked")
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					final SharedSessionContractImplementor session = executionContext.getSession();

					SqlTypeDescriptor descriptor = BlobSqlDescriptor.BLOB_BINDING;

					if ( byte[].class.isInstance( value ) ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = BlobSqlDescriptor.PRIMITIVE_ARRAY_BINDING;
					}
					else if ( session.useStreamForLobBinding() ) {
						descriptor = HANABlobTypeDescriptor.this.hanaStreamBlobTypeDescriptor;
					}

					final TypeConfiguration typeConfiguration = session.getFactory().getTypeConfiguration();
					descriptor.getSqlExpressableType( javaTypeDescriptor, typeConfiguration)
							.getJdbcValueBinder().bind( st, index, value, executionContext );
				}

				@Override
				@SuppressWarnings("unchecked")
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					final SharedSessionContractImplementor session = executionContext.getSession();

					SqlTypeDescriptor descriptor = BlobSqlDescriptor.BLOB_BINDING;

					if ( byte[].class.isInstance( value ) ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = BlobSqlDescriptor.PRIMITIVE_ARRAY_BINDING;
					}
					else if ( session.useStreamForLobBinding() ) {
						descriptor = HANABlobTypeDescriptor.this.hanaStreamBlobTypeDescriptor;
					}

					final TypeConfiguration typeConfiguration = session.getFactory().getTypeConfiguration();
					descriptor.getSqlExpressableType( javaTypeDescriptor, typeConfiguration )
							.getJdbcValueBinder().bind( st, name, value, executionContext );
				}
			};
		}

		public int getMaxLobPrefetchSize() {
			return this.maxLobPrefetchSize;
		}

		@Override
		public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
			// literal values for BLOB data is not supported.
			return null;
		}
	}

	// Set the LOB prefetch size. LOBs larger than this value will be read into memory as the HANA JDBC driver closes
	// the LOB when the result set is closed.
	private static final String MAX_LOB_PREFETCH_SIZE_PARAMETER_NAME = new String( "hibernate.dialect.hana.max_lob_prefetch_size" );
	// Use TINYINT instead of the native BOOLEAN type
	private static final String USE_LEGACY_BOOLEAN_TYPE_PARAMETER_NAME = new String( "hibernate.dialect.hana.use_legacy_boolean_type" );
	// Use unicode (NVARCHAR, NCLOB, etc.) instead of non-unicode (VARCHAR, CLOB) string types
	private static final String USE_UNICODE_STRING_TYPES_PARAMETER_NAME = new String( "hibernate.dialect.hana.use_unicode_string_types" );
	// Read and write double-typed fields as BigDecimal instead of Double to get around precision issues of the HANA
	// JDBC driver (https://service.sap.com/sap/support/notes/2590160)
	private static final String TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_PARAMETER_NAME = new String(
			"hibernate.dialect.hana.treat_double_typed_fields_as_decimal" );

	private static final int MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE = 1024;
	private static final Boolean USE_LEGACY_BOOLEAN_TYPE_DEFAULT_VALUE = Boolean.FALSE;
	private static final Boolean USE_UNICODE_STRING_TYPES_DEFAULT_VALUE = Boolean.FALSE;
	private static final Boolean TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_DEFAULT_VALUE = Boolean.FALSE;

	private HANANClobSqlDescriptor nClobTypeDescriptor = new HANANClobSqlDescriptor( MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE );

	private HANABlobTypeDescriptor blobTypeDescriptor = new HANABlobTypeDescriptor( MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE );

	private HANAClobTypeDescriptor clobTypeDescriptor = new HANAClobTypeDescriptor( MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE,
			USE_UNICODE_STRING_TYPES_DEFAULT_VALUE.booleanValue() );

	private boolean useLegacyBooleanType = USE_LEGACY_BOOLEAN_TYPE_DEFAULT_VALUE.booleanValue();
	private boolean useUnicodeStringTypes = USE_UNICODE_STRING_TYPES_DEFAULT_VALUE.booleanValue();
	private boolean treatDoubleTypedFieldsAsDecimal = TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_DEFAULT_VALUE.booleanValue();

	/*
	 * Tables named "TYPE" need to be quoted
	 */
	private final StandardTableExporter hanaTableExporter = new StandardTableExporter( this ) {

		@Override
		public String[] getSqlCreateStrings(ExportableTable table, JdbcServices jdbcServices) {
			String[] sqlCreateStrings = super.getSqlCreateStrings( table, jdbcServices );
			return quoteTypeIfNecessary( table, sqlCreateStrings, getCreateTableString() );
		}

		@Override
		public String[] getSqlDropStrings(ExportableTable table, JdbcServices jdbcServices) {
			String[] sqlDropStrings = super.getSqlDropStrings( table, jdbcServices );
			return quoteTypeIfNecessary( table, sqlDropStrings, "drop table" );
		}

		private String[] quoteTypeIfNecessary(ExportableTable table, String[] strings, String prefix) {
			if ( table.getTableName() == null || table.getTableName().isQuoted()
					|| !"type".equals( table.getTableName().getText().toLowerCase() ) ) {
				return strings;
			}

			Pattern createTableTypePattern = Pattern.compile( "(" + prefix + "\\s+)(" + table.getTableName().getText() + ")(.+)" );
			Pattern commentOnTableTypePattern = Pattern.compile( "(comment\\s+on\\s+table\\s+)(" + table.getTableName().getText() + ")(.+)" );
			for ( int i = 0; i < strings.length; i++ ) {
				Matcher createTableTypeMatcher = createTableTypePattern.matcher( strings[i] );
				Matcher commentOnTableTypeMatcher = commentOnTableTypePattern.matcher( strings[i] );
				if ( createTableTypeMatcher.matches() ) {
					strings[i] = createTableTypeMatcher.group( 1 ) + "\"TYPE\"" + createTableTypeMatcher.group( 3 );
				}
				if ( commentOnTableTypeMatcher.matches() ) {
					strings[i] = commentOnTableTypeMatcher.group( 1 ) + "\"TYPE\"" + commentOnTableTypeMatcher.group( 3 );
				}
			}

			return strings;
		}
	};

	public AbstractHANADialect() {
		super();

		registerColumnType( Types.DECIMAL, "decimal($p, $s)" );
		registerColumnType( Types.NUMERIC, "decimal($p, $s)" );
		registerColumnType( Types.DOUBLE, "double" );

		// varbinary max length 5000
		registerColumnType( Types.BINARY, 5000, "varbinary($l)" );
		registerColumnType( Types.VARBINARY, 5000, "varbinary($l)" );
		registerColumnType( Types.LONGVARBINARY, 5000, "varbinary($l)" );

		// for longer values, map to blob
		registerColumnType( Types.BINARY, "blob" );
		registerColumnType( Types.VARBINARY, "blob" );
		registerColumnType( Types.LONGVARBINARY, "blob" );

		registerColumnType( Types.CHAR, "varchar(1)" );
		registerColumnType( Types.NCHAR, "nvarchar(1)" );
		registerColumnType( Types.VARCHAR, 5000, "varchar($l)" );
		registerColumnType( Types.LONGVARCHAR, 5000, "varchar($l)" );
		registerColumnType( Types.NVARCHAR, 5000, "nvarchar($l)" );
		registerColumnType( Types.LONGNVARCHAR, 5000, "nvarchar($l)" );

		// for longer values map to clob/nclob
		registerColumnType( Types.LONGVARCHAR, "clob" );
		registerColumnType( Types.VARCHAR, "clob" );
		registerColumnType( Types.LONGNVARCHAR, "nclob" );
		registerColumnType( Types.NVARCHAR, "nclob" );
		registerColumnType( Types.CLOB, "clob" );
		registerColumnType( Types.NCLOB, "nclob" );

		registerColumnType( Types.BOOLEAN, "boolean" );

		// map bit/tinyint to smallint since tinyint is unsigned on HANA
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );

		registerHibernateType( Types.NCLOB, StandardSpiBasicTypes.MATERIALIZED_NCLOB.getJavaTypeDescriptor().getTypeName() );
		registerHibernateType( Types.CLOB, StandardSpiBasicTypes.MATERIALIZED_CLOB.getJavaTypeDescriptor().getTypeName() );
		registerHibernateType( Types.BLOB, StandardSpiBasicTypes.MATERIALIZED_BLOB.getJavaTypeDescriptor().getTypeName() );
		registerHibernateType( Types.NVARCHAR, StandardSpiBasicTypes.STRING.getJavaTypeDescriptor().getTypeName() );

		registerHanaKeywords();

		// createBlob() and createClob() are not supported by the HANA JDBC
		// driver
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );

		// getGeneratedKeys() is not supported by the HANA JDBC driver
		getDefaultProperties().setProperty( AvailableSettings.USE_GET_GENERATED_KEYS, "false" );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );
		registry.registerNamed( "to_date", StandardSpiBasicTypes.DATE );
		registry.registerNamed( "to_seconddate", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "to_time", StandardSpiBasicTypes.TIME );
		registry.registerNamed( "to_timestamp", StandardSpiBasicTypes.TIMESTAMP );

		registry.registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "current_utcdate", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "current_utctime", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_utctimestamp", StandardSpiBasicTypes.TIMESTAMP );

		registry.registerNamed( "add_days" );
		registry.registerNamed( "add_months" );
		registry.registerNamed( "add_seconds" );
		registry.registerNamed( "add_years" );
		registry.registerNamed( "dayname", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "dayofmonth", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "dayofyear", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "days_between", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "hour", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "isoweek", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "last_day", StandardSpiBasicTypes.DATE );
		registry.registerNamed( "localtoutc", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "minute", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "month", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "monthname", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "next_day", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "now", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "quarter", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "second", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "seconds_between", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "week", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "weekday", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "year", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "utctolocal", StandardSpiBasicTypes.TIMESTAMP );


		registry.registerNamed( "to_bigint", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "to_binary", StandardSpiBasicTypes.BINARY );
		registry.registerNamed( "to_decimal", StandardSpiBasicTypes.BIG_DECIMAL );
		registry.registerNamed( "to_double", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "to_int", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "to_integer", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "to_real", StandardSpiBasicTypes.FLOAT );
		registry.registerNamed( "to_smalldecimal", StandardSpiBasicTypes.BIG_DECIMAL );
		registry.registerNamed( "to_smallint", StandardSpiBasicTypes.SHORT );
		registry.registerNamed( "to_tinyint", StandardSpiBasicTypes.BYTE );

		registry.registerNamed( "abs" );
		registry.registerNamed( "acos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "asin", StandardSpiBasicTypes.DOUBLE );
		registry.namedTemplateBuilder( "atan2", "atan" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		registry.registerNamed( "bin2hex", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "bitand", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "ceil" );
		registry.registerNamed( "cos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "cosh", StandardSpiBasicTypes.DOUBLE );
		registry.namedTemplateBuilder( "cot", "cos" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		registry.registerNamed( "exp", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "floor" );
		registry.registerNamed( "greatest" );
		registry.registerNamed( "hex2bin", StandardSpiBasicTypes.BINARY );
		registry.registerNamed( "least" );
		registry.registerNamed( "ln", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "log", StandardSpiBasicTypes.DOUBLE );
		registry.namedTemplateBuilder( "log", "ln" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		registry.registerNamed( "power" );
		registry.registerNamed( "round" );
		registry.registerNamed( "mod", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "sign", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "sin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "sinh", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "sqrt", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "tan", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "tanh", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "uminus" );

		registry.registerNamed( "to_alphanum", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "to_nvarchar", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "to_varchar", StandardSpiBasicTypes.STRING );

		registry.registerNamed( "ascii", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "char", StandardSpiBasicTypes.CHARACTER );
		registry.register( "concat", ConcatFunctionTemplate.INSTANCE );
		registry.registerNamed( "lcase", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "left", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "length", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "locate", "locate(?2, ?1, ?3)",StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "lpad", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "ltrim", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "nchar", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "replace", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "right", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "rpad", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "rtrim", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "substr_after", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "substr_before", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "substring", StandardSpiBasicTypes.STRING );
		registry.register( "trim", AnsiTrimFunctionTemplate.INSTANCE );
		registry.registerNamed( "ucase", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "unicode", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "bit_length", "length(to_binary(?1))*8", StandardSpiBasicTypes.INTEGER );

		registry.registerNamed( "to_blob", StandardSpiBasicTypes.BLOB );
		registry.registerNamed( "to_clob", StandardSpiBasicTypes.CLOB );
		registry.registerNamed( "to_nclob", StandardSpiBasicTypes.NCLOB );

		registry.registerNamed( "coalesce" );
		registry.registerNoArgs( "current_connection", StandardSpiBasicTypes.INTEGER );
		registry.registerNoArgs( "current_schema", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "current_user", StandardSpiBasicTypes.STRING );
		registry.registerVarArgs( "grouping_id", StandardSpiBasicTypes.INTEGER, "(", ",", ")" );
		registry.registerNamed( "ifnull" );
		registry.registerNamed( "map" );
		registry.registerNamed( "nullif" );
		registry.registerNamed( "session_context" );
		registry.registerNoArgs( "session_user", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "sysuuid", StandardSpiBasicTypes.STRING );
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(final SQLException sqlException, final String message, final String sql) {

				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

				if ( errorCode == 131 ) {
					// 131 - Transaction rolled back by lock wait timeout
					return new LockTimeoutException( message, sqlException, sql );
				}

				if ( errorCode == 146 ) {
					// 146 - Resource busy and acquire with NOWAIT specified
					return new LockTimeoutException( message, sqlException, sql );
				}

				if ( errorCode == 132 ) {
					// 132 - Transaction rolled back due to unavailable resource
					return new LockAcquisitionException( message, sqlException, sql );
				}

				if ( errorCode == 133 ) {
					// 133 - Transaction rolled back by detected deadlock
					return new LockAcquisitionException( message, sqlException, sql );
				}

				// 259 - Invalid table name
				// 260 - Invalid column name
				// 261 - Invalid index name
				// 262 - Invalid query name
				// 263 - Invalid alias name
				if ( errorCode == 257 || ( errorCode >= 259 && errorCode <= 263 ) ) {
					throw new SQLGrammarException( message, sqlException, sql );
				}

				// 257 - Cannot insert NULL or update to NULL
				// 301 - Unique constraint violated
				// 461 - foreign key constraint violation
				// 462 - failed on update or delete by foreign key constraint violation
				if ( errorCode == 287 || errorCode == 301 || errorCode == 461 || errorCode == 462 ) {
					final String constraintName = getViolatedConstraintNameExtracter()
							.extractConstraintName( sqlException );

					return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}

				return null;
			}
		};
	}

	@Override
	public boolean forUpdateOfColumns() {
		return true;
	}

	@Override
	public String getAddColumnString() {
		return "add (";
	}

	@Override
	public String getAddColumnSuffixString() {
		return ")";
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getCreateSequenceString(final String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		if ( incrementSize == 0 ) {
			throw new MappingException( "Unable to create the sequence [" + sequenceName + "]: the increment size must not be 0" );
		}

		String createSequenceString = getCreateSequenceString( sequenceName ) + " start with " + initialValue + " increment by " + incrementSize;
		if ( incrementSize > 0 ) {
			if ( initialValue < 1 ) {
				// default minvalue for an ascending sequence is 1
				createSequenceString += " minvalue " + initialValue;
			}
		}
		else if ( incrementSize < 0 ) {
			if ( initialValue > -1 ) {
				// default maxvalue for a descending sequence is -1
				createSequenceString += " maxvalue " + initialValue;
			}
		}
		return createSequenceString;
	}

	@Override
	public IdTableStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( getIdTableExporter() );
	}

	@Override
	protected Exporter<IdTable> getIdTableExporter() {
		return new GlobalTempTableExporter();
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp from sys.dummy";
	}

	@Override
	public String getDropSequenceString(final String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getForUpdateString(final String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateString(final String aliases, final LockOptions lockOptions) {
		LockMode lockMode = lockOptions.findGreatestLockMode();
		lockOptions.setLockMode( lockMode );

		// not sure why this is sometimes empty
		if ( aliases == null || aliases.isEmpty() ) {
			return getForUpdateString( lockOptions );
		}

		return getForUpdateString( aliases, lockMode, lockOptions.getTimeOut() );
	}

	@SuppressWarnings({ "deprecation" })
	private String getForUpdateString(String aliases, LockMode lockMode, int timeout) {
		switch ( lockMode ) {
			case UPGRADE:
				return getForUpdateString( aliases );
			case PESSIMISTIC_READ:
				return getReadLockString( aliases, timeout );
			case PESSIMISTIC_WRITE:
				return getWriteLockString( aliases, timeout );
			case UPGRADE_NOWAIT:
			case FORCE:
			case PESSIMISTIC_FORCE_INCREMENT:
				return getForUpdateNowaitString( aliases );
			case UPGRADE_SKIPLOCKED:
				return getForUpdateSkipLockedString( aliases );
			default:
				return "";
		}
	}

	@Override
	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait";
	}

	@Override
	public String getLimitString(final String sql, final boolean hasOffset) {
		return new StringBuilder( sql.length() + 20 ).append( sql ).append( hasOffset ? " limit ? offset ?" : " limit ?" )
				.toString();
	}

	@Override
	public String getNotExpression(final String expression) {
		return "not (" + expression + ")";
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from sys.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorHANADatabaseImpl.INSTANCE;
	}

	@Override
	public String getSelectSequenceNextValString(final String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getSequenceNextValString(final String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from sys.dummy";
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(final int sqlCode) {
		switch ( sqlCode ) {
			case Types.BOOLEAN: {
				return this.useLegacyBooleanType ? BitSqlDescriptor.INSTANCE : BooleanSqlDescriptor.INSTANCE;
			}
			case Types.CHAR: {
				return this.useUnicodeStringTypes ? NCharSqlDescriptor.INSTANCE : CharSqlDescriptor.INSTANCE;
			}
			case Types.VARCHAR: {
				return this.useUnicodeStringTypes ? NVarcharSqlDescriptor.INSTANCE : VarcharSqlDescriptor.INSTANCE;
			}
			case Types.TINYINT: {
				// tinyint is unsigned on HANA
				return SmallIntSqlDescriptor.INSTANCE;
			}
			case Types.BLOB: {
				return this.blobTypeDescriptor;
			}
			case Types.CLOB: {
				return this.clobTypeDescriptor;
			}
			case Types.NCLOB: {
				return this.nClobTypeDescriptor;
			}
			case Types.DOUBLE:
				return this.treatDoubleTypedFieldsAsDecimal ? DecimalSqlDescriptor.INSTANCE : DoubleSqlDescriptor.INSTANCE;
			default: {
				return super.getSqlTypeDescriptorOverride( sqlCode );
			}
		}
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	protected void registerHanaKeywords() {
		registerKeyword( "all" );
		registerKeyword( "alter" );
		registerKeyword( "as" );
		registerKeyword( "before" );
		registerKeyword( "begin" );
		registerKeyword( "both" );
		registerKeyword( "case" );
		registerKeyword( "char" );
		registerKeyword( "condition" );
		registerKeyword( "connect" );
		registerKeyword( "cross" );
		registerKeyword( "cube" );
		registerKeyword( "current_connection" );
		registerKeyword( "current_date" );
		registerKeyword( "current_schema" );
		registerKeyword( "current_time" );
		registerKeyword( "current_timestamp" );
		registerKeyword( "current_transaction_isolation_level" );
		registerKeyword( "current_user" );
		registerKeyword( "current_utcdate" );
		registerKeyword( "current_utctime" );
		registerKeyword( "current_utctimestamp" );
		registerKeyword( "currval" );
		registerKeyword( "cursor" );
		registerKeyword( "declare" );
		registerKeyword( "deferred" );
		registerKeyword( "distinct" );
		registerKeyword( "else" );
		registerKeyword( "elseif" );
		registerKeyword( "end" );
		registerKeyword( "except" );
		registerKeyword( "exception" );
		registerKeyword( "exec" );
		registerKeyword( "false" );
		registerKeyword( "for" );
		registerKeyword( "from" );
		registerKeyword( "full" );
		registerKeyword( "group" );
		registerKeyword( "having" );
		registerKeyword( "if" );
		registerKeyword( "in" );
		registerKeyword( "inner" );
		registerKeyword( "inout" );
		registerKeyword( "intersect" );
		registerKeyword( "into" );
		registerKeyword( "is" );
		registerKeyword( "join" );
		registerKeyword( "leading" );
		registerKeyword( "left" );
		registerKeyword( "limit" );
		registerKeyword( "loop" );
		registerKeyword( "minus" );
		registerKeyword( "natural" );
		registerKeyword( "nchar" );
		registerKeyword( "nextval" );
		registerKeyword( "null" );
		registerKeyword( "on" );
		registerKeyword( "order" );
		registerKeyword( "out" );
		registerKeyword( "prior" );
		registerKeyword( "return" );
		registerKeyword( "returns" );
		registerKeyword( "reverse" );
		registerKeyword( "right" );
		registerKeyword( "rollup" );
		registerKeyword( "rowid" );
		registerKeyword( "select" );
		registerKeyword( "session_user" );
		registerKeyword( "set" );
		registerKeyword( "sql" );
		registerKeyword( "start" );
		registerKeyword( "sysuuid" );
		registerKeyword( "tablesample" );
		registerKeyword( "top" );
		registerKeyword( "trailing" );
		registerKeyword( "true" );
		registerKeyword( "union" );
		registerKeyword( "unknown" );
		registerKeyword( "using" );
		registerKeyword( "utctimestamp" );
		registerKeyword( "values" );
		registerKeyword( "when" );
		registerKeyword( "where" );
		registerKeyword( "while" );
		registerKeyword( "with" );
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	/**
	 * HANA currently does not support check constraints.
	 */
	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		// http://scn.sap.com/thread/3221812
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsTableCheck() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return true;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}

	@Override
	public int getMaxAliasLength() {
		return 128;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getSelectGUIDString() {
		return "select sysuuid from sys.dummy";
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}

	@SuppressWarnings("deprecation")
	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		/*
		 * Copied from Dialect
		 */
		builder.applyIdentifierCasing( dbMetaData );

		builder.applyReservedWords( dbMetaData );
		builder.applyReservedWords( AnsiSqlKeywords.INSTANCE.sql2003() );
		builder.applyReservedWords( getKeywords() );

		builder.setNameQualifierSupport( getNameQualifierSupport() );

		/*
		 * HANA-specific extensions
		 */
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.UPPER );

		final IdentifierHelper identifierHelper = builder.build();

		return new IdentifierHelper() {

			private final IdentifierHelper helper = identifierHelper;

			@Override
			public String toMetaDataSchemaName(Identifier schemaIdentifier) {
				return this.helper.toMetaDataSchemaName( schemaIdentifier );
			}

			@Override
			public String toMetaDataObjectName(Identifier identifier) {
				return this.helper.toMetaDataObjectName( identifier );
			}

			@Override
			public String toMetaDataCatalogName(Identifier catalogIdentifier) {
				return this.helper.toMetaDataCatalogName( catalogIdentifier );
			}

			@Override
			public Identifier toIdentifier(String text) {
				return normalizeQuoting( Identifier.toIdentifier( text ) );
			}

			@Override
			public Identifier toIdentifier(String text, boolean quoted) {
				return normalizeQuoting( Identifier.toIdentifier( text, quoted ) );
			}

			@Override
			public Identifier normalizeQuoting(Identifier identifier) {
				Identifier normalizedIdentifier = this.helper.normalizeQuoting( identifier );

				if ( normalizedIdentifier == null ) {
					return null;
				}

				// need to quote names containing special characters like ':'
				if ( !normalizedIdentifier.isQuoted() && !normalizedIdentifier.getText().matches( "\\w+" ) ) {
					normalizedIdentifier = Identifier.quote( normalizedIdentifier );
				}

				return normalizedIdentifier;
			}

			@Override
			public boolean isReservedWord(String word) {
				return this.helper.isReservedWord( word );
			}

			@Override
			public Identifier applyGlobalQuoting(String text) {
				return this.helper.applyGlobalQuoting( text );
			}
		};
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select current_schema from sys.dummy";
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases ) + " nowait";
	}

	@Override
	public String getReadLockString(int timeout) {
		return getWriteLockString( timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		return getWriteLockString( aliases, timeout );
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout > 0 ) {
			return getForUpdateString() + " wait " + timeout;
		}
		else if ( timeout == 0 ) {
			return getForUpdateNowaitString();
		}
		else {
			return getForUpdateString();
		}
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		if ( timeout > 0 ) {
			return getForUpdateString( aliases ) + " wait " + timeout;
		}
		else if ( timeout == 0 ) {
			return getForUpdateNowaitString( aliases );
		}
		else {
			return getForUpdateString( aliases );
		}
	}

	@Override
	public String getQueryHintString(String query, List<String> hints) {
		return query + " with hint (" + String.join( ",", hints ) + ")";
	}

	@Override
	public String getTableComment(String comment) {
		return "comment '" + comment + "'";
	}

	@Override
	public String getColumnComment(String comment) {
		return "comment '" + comment + "'";
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final ConnectionProvider connectionProvider = serviceRegistry.getService( ConnectionProvider.class );

		int maxLobPrefetchSizeDefault = MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE;
		if ( connectionProvider != null ) {
			Connection conn = null;
			try {
				conn = connectionProvider.getConnection();
				try ( Statement statement = conn.createStatement() ) {
					try ( ResultSet rs = statement.executeQuery(
							"SELECT TOP 1 VALUE, MAP(LAYER_NAME, 'DEFAULT', 1, 'SYSTEM', 2, 'DATABASE', 3, 4) AS LAYER FROM SYS.M_INIFILE_CONTENTS WHERE FILE_NAME='indexserver.ini' AND SECTION='session' AND KEY='max_lob_prefetch_size' ORDER BY LAYER DESC" ) ) {
						// This only works if the current user has the privilege INIFILE ADMIN
						if ( rs.next() ) {
							maxLobPrefetchSizeDefault = rs.getInt( 1 );
						}
					}
				}
			}
			catch (Exception e) {
				LOG.debug(
						"An error occurred while trying to determine the value of the HANA parameter indexserver.ini / session / max_lob_prefetch_size. Using the default value "
								+ maxLobPrefetchSizeDefault,
						e );
			}
			finally {
				if ( conn != null ) {
					try {
						connectionProvider.closeConnection( conn );
					}
					catch (SQLException e) {
						// ignore
					}
				}
			}
		}

		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		int maxLobPrefetchSize = configurationService.getSetting(
				MAX_LOB_PREFETCH_SIZE_PARAMETER_NAME,
				new Converter<Integer>() {

					@Override
					public Integer convert(Object value) {
						return Integer.valueOf( value.toString() );
					}

				},
				Integer.valueOf( maxLobPrefetchSizeDefault ) ).intValue();

		if ( this.nClobTypeDescriptor.getMaxLobPrefetchSize() != maxLobPrefetchSize ) {
			this.nClobTypeDescriptor = new HANANClobSqlDescriptor( maxLobPrefetchSize );
		}

		if ( this.blobTypeDescriptor.getMaxLobPrefetchSize() != maxLobPrefetchSize ) {
			this.blobTypeDescriptor = new HANABlobTypeDescriptor( maxLobPrefetchSize );
		}

		this.useUnicodeStringTypes = configurationService.getSetting( USE_UNICODE_STRING_TYPES_PARAMETER_NAME, StandardConverters.BOOLEAN,
				USE_UNICODE_STRING_TYPES_DEFAULT_VALUE ).booleanValue();

		if ( this.useUnicodeStringTypes ) {
			registerColumnType( Types.CHAR, "nvarchar(1)" );
			registerColumnType( Types.VARCHAR, 5000, "nvarchar($l)" );
			registerColumnType( Types.LONGVARCHAR, 5000, "nvarchar($l)" );

			// for longer values map to clob/nclob
			registerColumnType( Types.LONGVARCHAR, "nclob" );
			registerColumnType( Types.VARCHAR, "nclob" );
			registerColumnType( Types.CLOB, "nclob" );
		}

		if ( this.clobTypeDescriptor.getMaxLobPrefetchSize() != maxLobPrefetchSize
				|| this.clobTypeDescriptor.isUseUnicodeStringTypes() != this.useUnicodeStringTypes ) {
			this.clobTypeDescriptor = new HANAClobTypeDescriptor( maxLobPrefetchSize, this.useUnicodeStringTypes );
		}

		this.useLegacyBooleanType = configurationService.getSetting( USE_LEGACY_BOOLEAN_TYPE_PARAMETER_NAME, StandardConverters.BOOLEAN,
				USE_LEGACY_BOOLEAN_TYPE_DEFAULT_VALUE ).booleanValue();

		if ( this.useLegacyBooleanType ) {
			registerColumnType( Types.BOOLEAN, "tinyint" );
		}

		this.treatDoubleTypedFieldsAsDecimal = configurationService.getSetting( TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_PARAMETER_NAME, StandardConverters.BOOLEAN,
				TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_DEFAULT_VALUE ).booleanValue();

		if ( this.treatDoubleTypedFieldsAsDecimal ) {
			registerHibernateType( Types.DOUBLE, StandardSpiBasicTypes.BIG_DECIMAL.getJavaTypeDescriptor().getTypeName() );
		}
	}

	public SqlTypeDescriptor getBlobTypeDescriptor() {
		return this.blobTypeDescriptor;
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		if ( this.useLegacyBooleanType ) {
			return bool ? "1" : "0";
		}
		return bool ? "true" : "false";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new HANAIdentityColumnSupport();
	}

	@Override
	public Exporter<ExportableTable> getTableExporter() {
		return this.hanaTableExporter;
	}

	/*
	 * HANA doesn't really support REF_CURSOR returns from a procedure, but REF_CURSOR support can be emulated by using
	 * procedures or functions with an OUT parameter of type TABLE. The results will be returned as result sets on the
	 * callable statement.
	 */
	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return StandardCallableStatementSupport.REF_CURSOR_INSTANCE;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
		// Result set (TABLE) OUT parameters don't need to be registered
		return position;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, String name) throws SQLException {
		// Result set (TABLE) OUT parameters don't need to be registered
		return 0;
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return false;
	}
}
