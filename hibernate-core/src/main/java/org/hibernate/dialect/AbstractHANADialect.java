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
import java.time.Duration;
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
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.AnsiTrimFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
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
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Table;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorHANADatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.DataHelper;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.BitTypeDescriptor;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.type.descriptor.sql.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.sql.CharTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.DecimalTypeDescriptor;
import org.hibernate.type.descriptor.sql.DoubleTypeDescriptor;
import org.hibernate.type.descriptor.sql.NCharTypeDescriptor;
import org.hibernate.type.descriptor.sql.NClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.NVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.SmallIntTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

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
 * @author <a href="mailto:andrew.clemons@sap.com">Andrew Clemons</a>
 * @author <a href="mailto:jonathan.bregler@sap.com">Jonathan Bregler</a>
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
		public long length() throws SQLException {
			return this.getBytes().length;
		}

		@Override
		public byte[] getBytes(long pos, int length) throws SQLException {
			return Arrays.copyOfRange( this.bytes, (int) ( pos - 1 ), (int) ( pos - 1 + length ) );
		}

		@Override
		public InputStream getBinaryStream() throws SQLException {
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
		public int setBytes(long pos, byte[] bytes) throws SQLException {
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
		public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
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
		public OutputStream setBinaryStream(long pos) throws SQLException {
			return new ByteArrayOutputStream() {

				{
					this.buf = getBytes();
				}
			};
		}

		@Override
		public void truncate(long len) throws SQLException {
			this.setBytes( Arrays.copyOf( this.getBytes(), (int) len ) );
		}

		@Override
		public void free() throws SQLException {
			this.setBytes( null );
		}

		@Override
		public InputStream getBinaryStream(long pos, long length) throws SQLException {
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

		private String data = null;

		public MaterializedNClob(String data) {
			this.data = data;
		}

		@Override
		public void truncate(long len) throws SQLException {
			this.data = "";
		}

		@Override
		public int setString(long pos, String str, int offset, int len) throws SQLException {
			this.data = this.data.substring( 0, (int) ( pos - 1 ) ) + str.substring( offset, offset + len )
					+ this.data.substring( (int) ( pos - 1 + len ) );
			return len;
		}

		@Override
		public int setString(long pos, String str) throws SQLException {
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
		public long position(Clob searchstr, long start) throws SQLException {
			return this.data.indexOf( DataHelper.extractString( searchstr ), (int) ( start - 1 ) );
		}

		@Override
		public long position(String searchstr, long start) throws SQLException {
			return this.data.indexOf( searchstr, (int) ( start - 1 ) );
		}

		@Override
		public long length() throws SQLException {
			return this.data.length();
		}

		@Override
		public String getSubString(long pos, int length) throws SQLException {
			return this.data.substring( (int) ( pos - 1 ), (int) ( pos - 1 + length ) );
		}

		@Override
		public Reader getCharacterStream(long pos, long length) throws SQLException {
			return new StringReader( this.data.substring( (int) ( pos - 1 ), (int) ( pos - 1 + length ) ) );
		}

		@Override
		public Reader getCharacterStream() throws SQLException {
			return new StringReader( this.data );
		}

		@Override
		public InputStream getAsciiStream() throws SQLException {
			return new ByteArrayInputStream( this.data.getBytes( StandardCharsets.ISO_8859_1 ) );
		}

		@Override
		public void free() throws SQLException {
			this.data = null;
		}
	}

	private static class HANAStreamBlobTypeDescriptor implements SqlTypeDescriptor {

		private static final long serialVersionUID = -2476600722093442047L;

		final int maxLobPrefetchSize;

		public HANAStreamBlobTypeDescriptor(int maxLobPrefetchSize) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
		}

		@Override
		public int getSqlType() {
			return Types.BLOB;
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {

				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					final BinaryStream binaryStream = javaTypeDescriptor.unwrap( value, BinaryStream.class, options );
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
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					final BinaryStream binaryStream = javaTypeDescriptor.unwrap( value, BinaryStream.class, options );
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
		public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {

				@Override
				protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
					Blob rsBlob = rs.getBlob( name );
					if ( rsBlob == null || rsBlob.length() < HANAStreamBlobTypeDescriptor.this.maxLobPrefetchSize ) {
						return javaTypeDescriptor.wrap( rsBlob, options );
					}
					Blob blob = new MaterializedBlob( DataHelper.extractBytes( rsBlob.getBinaryStream() ) );
					return javaTypeDescriptor.wrap( blob, options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getBlob( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getBlob( name ), options );
				}
			};
		}

	}

	// the ClobTypeDescriptor and NClobTypeDescriptor for HANA are slightly
	// changed from the standard ones. The HANA JDBC driver currently closes any
	// stream passed in via
	// PreparedStatement.setCharacterStream(int,Reader,long)
	// after the stream has been processed. this causes problems later if we are
	// using non-contexual lob creation and HANA then closes our StringReader.
	// see test case LobLocatorTest

	private static class HANAClobTypeDescriptor extends ClobTypeDescriptor {

		/** serial version uid. */
		private static final long serialVersionUID = -379042275442752102L;

		final int maxLobPrefetchSize;
		final boolean useUnicodeStringTypes;

		public HANAClobTypeDescriptor(int maxLobPrefetchSize, boolean useUnicodeStringTypes) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
			this.useUnicodeStringTypes = useUnicodeStringTypes;
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {

				@Override
				protected void doBind(final PreparedStatement st, final X value, final int index, final WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class, options );

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
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class, options );

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
		public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {

				@Override
				protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
					Clob rsClob;
					if ( HANAClobTypeDescriptor.this.useUnicodeStringTypes ) {
						rsClob = rs.getNClob( name );
					}
					else {
						rsClob = rs.getClob( name );
					}

					if ( rsClob == null || rsClob.length() < HANAClobTypeDescriptor.this.maxLobPrefetchSize ) {
						return javaTypeDescriptor.wrap( rsClob, options );
					}
					Clob clob = new MaterializedNClob( DataHelper.extractString( rsClob ) );
					return javaTypeDescriptor.wrap( clob, options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getClob( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getClob( name ), options );
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

	private static class HANANClobTypeDescriptor extends NClobTypeDescriptor {

		/** serial version uid. */
		private static final long serialVersionUID = 5651116091681647859L;

		final int maxLobPrefetchSize;

		public HANANClobTypeDescriptor(int maxLobPrefetchSize) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
		}

		@Override
		public <X> BasicBinder<X> getNClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {

				@Override
				protected void doBind(final PreparedStatement st, final X value, final int index, final WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class, options );

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
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class, options );

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
		public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {

				@Override
				protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
					NClob rsNClob = rs.getNClob( name );
					if ( rsNClob == null || rsNClob.length() < HANANClobTypeDescriptor.this.maxLobPrefetchSize ) {
						return javaTypeDescriptor.wrap( rsNClob, options );
					}
					NClob nClob = new MaterializedNClob( DataHelper.extractString( rsNClob ) );
					return javaTypeDescriptor.wrap( nClob, options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getNClob( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getNClob( name ), options );
				}
			};
		}

		public int getMaxLobPrefetchSize() {
			return this.maxLobPrefetchSize;
		}
	}

	public static class HANABlobTypeDescriptor implements SqlTypeDescriptor {

		private static final long serialVersionUID = 5874441715643764323L;

		final int maxLobPrefetchSize;

		final HANAStreamBlobTypeDescriptor hanaStreamBlobTypeDescriptor;

		public HANABlobTypeDescriptor(int maxLobPrefetchSize) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
			this.hanaStreamBlobTypeDescriptor = new HANAStreamBlobTypeDescriptor( maxLobPrefetchSize );
		}

		@Override
		public int getSqlType() {
			return Types.BLOB;
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {

				@Override
				protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
					Blob rsBlob = rs.getBlob( name );
					if ( rsBlob == null || rsBlob.length() < HANABlobTypeDescriptor.this.maxLobPrefetchSize ) {
						return javaTypeDescriptor.wrap( rsBlob, options );
					}
					Blob blob = new MaterializedBlob( DataHelper.extractBytes( rsBlob.getBinaryStream() ) );
					return javaTypeDescriptor.wrap( blob, options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getBlob( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getBlob( name ), options );
				}
			};
		}

		@Override
		public <X> BasicBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {

				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					SqlTypeDescriptor descriptor = BlobTypeDescriptor.BLOB_BINDING;
					if ( byte[].class.isInstance( value ) ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING;
					}
					else if ( options.useStreamForLobBinding() ) {
						descriptor = HANABlobTypeDescriptor.this.hanaStreamBlobTypeDescriptor;
					}
					descriptor.getBinder( javaTypeDescriptor ).bind( st, value, index, options );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					SqlTypeDescriptor descriptor = BlobTypeDescriptor.BLOB_BINDING;
					if ( byte[].class.isInstance( value ) ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING;
					}
					else if ( options.useStreamForLobBinding() ) {
						descriptor = HANABlobTypeDescriptor.this.hanaStreamBlobTypeDescriptor;
					}
					descriptor.getBinder( javaTypeDescriptor ).bind( st, value, name, options );
				}
			};
		}

		public int getMaxLobPrefetchSize() {
			return this.maxLobPrefetchSize;
		}
	}

	// Set the LOB prefetch size. LOBs larger than this value will be read into memory as the HANA JDBC driver closes
	// the LOB when the result set is closed.
	private static final String MAX_LOB_PREFETCH_SIZE_PARAMETER_NAME = "hibernate.dialect.hana.max_lob_prefetch_size";
	// Use TINYINT instead of the native BOOLEAN type
	private static final String USE_LEGACY_BOOLEAN_TYPE_PARAMETER_NAME = "hibernate.dialect.hana.use_legacy_boolean_type";
	// Use unicode (NVARCHAR, NCLOB, etc.) instead of non-unicode (VARCHAR, CLOB) string types
	private static final String USE_UNICODE_STRING_TYPES_PARAMETER_NAME = "hibernate.dialect.hana.use_unicode_string_types";
	// Read and write double-typed fields as BigDecimal instead of Double to get around precision issues of the HANA
	// JDBC driver (https://service.sap.com/sap/support/notes/2590160)
	private static final String TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_PARAMETER_NAME = "hibernate.dialect.hana.treat_double_typed_fields_as_decimal";

	private static final int MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE = 1024;
	private static final Boolean USE_LEGACY_BOOLEAN_TYPE_DEFAULT_VALUE = Boolean.FALSE;
	private static final Boolean TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_DEFAULT_VALUE = Boolean.FALSE;

	private HANANClobTypeDescriptor nClobTypeDescriptor = new HANANClobTypeDescriptor( MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE );

	private HANABlobTypeDescriptor blobTypeDescriptor = new HANABlobTypeDescriptor( MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE );

	private HANAClobTypeDescriptor clobTypeDescriptor;

	private boolean useLegacyBooleanType = USE_LEGACY_BOOLEAN_TYPE_DEFAULT_VALUE.booleanValue();
	private boolean useUnicodeStringTypes;

	private boolean treatDoubleTypedFieldsAsDecimal = TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_DEFAULT_VALUE.booleanValue();

	/*
	 * Tables named "TYPE" need to be quoted
	 */
	private final StandardTableExporter hanaTableExporter = new StandardTableExporter( this ) {

		@Override
		public String[] getSqlCreateStrings(org.hibernate.mapping.Table table, org.hibernate.boot.Metadata metadata) {
			String[] sqlCreateStrings = super.getSqlCreateStrings( table, metadata );
			return quoteTypeIfNecessary( table, sqlCreateStrings, getCreateTableString() );
		}

		@Override
		public String[] getSqlDropStrings(Table table, org.hibernate.boot.Metadata metadata) {
			String[] sqlDropStrings = super.getSqlDropStrings( table, metadata );
			return quoteTypeIfNecessary( table, sqlDropStrings, "drop table" );
		}

		private String[] quoteTypeIfNecessary(org.hibernate.mapping.Table table, String[] strings, String prefix) {
			if ( table.getNameIdentifier() == null || table.getNameIdentifier().isQuoted()
					|| !"type".equals( table.getNameIdentifier().getText().toLowerCase() ) ) {
				return strings;
			}

			Pattern createTableTypePattern = Pattern.compile( "(" + prefix + "\\s+)(" + table.getNameIdentifier().getText() + ")(.+)" );
			Pattern commentOnTableTypePattern = Pattern.compile( "(comment\\s+on\\s+table\\s+)(" + table.getNameIdentifier().getText() + ")(.+)" );
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

		this.useUnicodeStringTypes = useUnicodeStringTypesDefault().booleanValue();
		this.clobTypeDescriptor = new HANAClobTypeDescriptor( MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE,
				useUnicodeStringTypesDefault().booleanValue() );

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

		registerHibernateType( Types.NCLOB, StandardBasicTypes.MATERIALIZED_NCLOB.getName() );
		registerHibernateType( Types.CLOB, StandardBasicTypes.MATERIALIZED_CLOB.getName() );
		registerHibernateType( Types.BLOB, StandardBasicTypes.MATERIALIZED_BLOB.getName() );
		registerHibernateType( Types.NVARCHAR, StandardBasicTypes.NSTRING.getName() );

		registerFunction( "to_date", new StandardSQLFunction( "to_date", StandardBasicTypes.DATE ) );
		registerFunction( "to_seconddate", new StandardSQLFunction( "to_seconddate", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "to_time", new StandardSQLFunction( "to_time", StandardBasicTypes.TIME ) );
		registerFunction( "to_timestamp", new StandardSQLFunction( "to_timestamp", StandardBasicTypes.TIMESTAMP ) );

		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE, false ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME, false ) );
		registerFunction( "current_timestamp",
				new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP, false ) );
		registerFunction( "current_utcdate", new NoArgSQLFunction( "current_utcdate", StandardBasicTypes.DATE, false ) );
		registerFunction( "current_utctime", new NoArgSQLFunction( "current_utctime", StandardBasicTypes.TIME, false ) );
		registerFunction( "current_utctimestamp",
				new NoArgSQLFunction( "current_utctimestamp", StandardBasicTypes.TIMESTAMP, false ) );

		registerFunction( "add_days", new StandardSQLFunction( "add_days" ) );
		registerFunction( "add_months", new StandardSQLFunction( "add_months" ) );
		registerFunction( "add_seconds", new StandardSQLFunction( "add_seconds" ) );
		registerFunction( "add_years", new StandardSQLFunction( "add_years" ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
		registerFunction( "days_between", new StandardSQLFunction( "days_between", StandardBasicTypes.INTEGER ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", StandardBasicTypes.INTEGER ) );
		registerFunction( "isoweek", new StandardSQLFunction( "isoweek", StandardBasicTypes.STRING ) );
		registerFunction( "last_day", new StandardSQLFunction( "last_day", StandardBasicTypes.DATE ) );
		registerFunction( "localtoutc", new StandardSQLFunction( "localtoutc", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", StandardBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", StandardBasicTypes.INTEGER ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardBasicTypes.STRING ) );
		registerFunction( "next_day", new StandardSQLFunction( "next_day", StandardBasicTypes.DATE ) );
		registerFunction( "now", new NoArgSQLFunction( "now", StandardBasicTypes.TIMESTAMP, true ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardBasicTypes.STRING ) );
		registerFunction( "second", new StandardSQLFunction( "second", StandardBasicTypes.INTEGER ) );
		registerFunction( "seconds_between", new StandardSQLFunction( "seconds_between", StandardBasicTypes.LONG ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardBasicTypes.INTEGER ) );
		registerFunction( "weekday", new StandardSQLFunction( "weekday", StandardBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSQLFunction( "year", StandardBasicTypes.INTEGER ) );
		registerFunction( "utctolocal", new StandardSQLFunction( "utctolocal", StandardBasicTypes.TIMESTAMP ) );

		registerFunction( "to_bigint", new StandardSQLFunction( "to_bigint", StandardBasicTypes.LONG ) );
		registerFunction( "to_binary", new StandardSQLFunction( "to_binary", StandardBasicTypes.BINARY ) );
		registerFunction( "to_decimal", new StandardSQLFunction( "to_decimal", StandardBasicTypes.BIG_DECIMAL ) );
		registerFunction( "to_double", new StandardSQLFunction( "to_double", StandardBasicTypes.DOUBLE ) );
		registerFunction( "to_int", new StandardSQLFunction( "to_int", StandardBasicTypes.INTEGER ) );
		registerFunction( "to_integer", new StandardSQLFunction( "to_integer", StandardBasicTypes.INTEGER ) );
		registerFunction( "to_real", new StandardSQLFunction( "to_real", StandardBasicTypes.FLOAT ) );
		registerFunction( "to_smalldecimal", new StandardSQLFunction( "to_smalldecimal", StandardBasicTypes.BIG_DECIMAL ) );
		registerFunction( "to_smallint", new StandardSQLFunction( "to_smallint", StandardBasicTypes.SHORT ) );
		registerFunction( "to_tinyint", new StandardSQLFunction( "to_tinyint", StandardBasicTypes.BYTE ) );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "acos", new StandardSQLFunction( "acos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new StandardSQLFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "bin2hex", new StandardSQLFunction( "bin2hex", StandardBasicTypes.STRING ) );
		registerFunction( "bitand", new StandardSQLFunction( "bitand", StandardBasicTypes.LONG ) );
		registerFunction( "ceil", new StandardSQLFunction( "ceil" ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cosh", new StandardSQLFunction( "cosh", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "floor", new StandardSQLFunction( "floor" ) );
		registerFunction( "greatest", new StandardSQLFunction( "greatest" ) );
		registerFunction( "hex2bin", new StandardSQLFunction( "hex2bin", StandardBasicTypes.BINARY ) );
		registerFunction( "least", new StandardSQLFunction( "least" ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
		registerFunction( "power", new StandardSQLFunction( "power" ) );
		registerFunction( "round", new StandardSQLFunction( "round" ) );
		registerFunction( "mod", new StandardSQLFunction( "mod", StandardBasicTypes.INTEGER ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sinh", new StandardSQLFunction( "sinh", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tanh", new StandardSQLFunction( "tanh", StandardBasicTypes.DOUBLE ) );
		registerFunction( "uminus", new StandardSQLFunction( "uminus" ) );

		registerFunction( "to_alphanum", new StandardSQLFunction( "to_alphanum", StandardBasicTypes.STRING ) );
		registerFunction( "to_nvarchar", new StandardSQLFunction( "to_nvarchar", StandardBasicTypes.STRING ) );
		registerFunction( "to_varchar", new StandardSQLFunction( "to_varchar", StandardBasicTypes.STRING ) );

		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardBasicTypes.INTEGER ) );
		registerFunction( "char", new StandardSQLFunction( "char", StandardBasicTypes.CHARACTER ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase", StandardBasicTypes.STRING ) );
		registerFunction( "left", new StandardSQLFunction( "left", StandardBasicTypes.STRING ) );
		registerFunction( "length", new StandardSQLFunction( "length", StandardBasicTypes.INTEGER ) );
		registerFunction( "locate", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "locate(?2, ?1, ?3)" ) );
		registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim", StandardBasicTypes.STRING ) );
		registerFunction( "nchar", new StandardSQLFunction( "nchar", StandardBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
		registerFunction( "right", new StandardSQLFunction( "right", StandardBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardBasicTypes.STRING ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", StandardBasicTypes.STRING ) );
		registerFunction( "substr_after", new StandardSQLFunction( "substr_after", StandardBasicTypes.STRING ) );
		registerFunction( "substr_before", new StandardSQLFunction( "substr_before", StandardBasicTypes.STRING ) );
		registerFunction( "substring", new StandardSQLFunction( "substring", StandardBasicTypes.STRING ) );
		registerFunction( "trim", new AnsiTrimFunction() );
		registerFunction( "ucase", new StandardSQLFunction( "ucase", StandardBasicTypes.STRING ) );
		registerFunction( "unicode", new StandardSQLFunction( "unicode", StandardBasicTypes.INTEGER ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "length(to_binary(?1))*8" ) );

		registerFunction( "to_blob", new StandardSQLFunction( "to_blob", StandardBasicTypes.BLOB ) );
		registerFunction( "to_clob", new StandardSQLFunction( "to_clob", StandardBasicTypes.CLOB ) );
		registerFunction( "to_nclob", new StandardSQLFunction( "to_nclob", StandardBasicTypes.NCLOB ) );

		registerFunction( "coalesce", new StandardSQLFunction( "coalesce" ) );
		registerFunction( "current_connection",
				new NoArgSQLFunction( "current_connection", StandardBasicTypes.INTEGER, false ) );
		registerFunction( "current_schema", new NoArgSQLFunction( "current_schema", StandardBasicTypes.STRING, false ) );
		registerFunction( "current_user", new NoArgSQLFunction( "current_user", StandardBasicTypes.STRING, false ) );
		registerFunction( "grouping_id", new VarArgsSQLFunction( StandardBasicTypes.INTEGER, "(", ",", ")" ) );
		registerFunction( "ifnull", new StandardSQLFunction( "ifnull" ) );
		registerFunction( "map", new StandardSQLFunction( "map" ) );
		registerFunction( "nullif", new StandardSQLFunction( "nullif" ) );
		registerFunction( "session_context", new StandardSQLFunction( "session_context" ) );
		registerFunction( "session_user", new NoArgSQLFunction( "session_user", StandardBasicTypes.STRING, false ) );
		registerFunction( "sysuuid", new NoArgSQLFunction( "sysuuid", StandardBasicTypes.STRING, false ) );

		registerHanaKeywords();

		// createBlob() and createClob() are not supported by the HANA JDBC
		// driver
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );

		// getGeneratedKeys() is not supported by the HANA JDBC driver
		getDefaultProperties().setProperty( AvailableSettings.USE_GET_GENERATED_KEYS, "false" );
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
				// 462 - failed on update or delete by foreign key constraint
				// violation
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
		else {
			if ( initialValue > -1 ) {
				// default maxvalue for a descending sequence is -1
				createSequenceString += " maxvalue " + initialValue;
			}
		}
		return createSequenceString;
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
			case Types.CLOB:
				return this.clobTypeDescriptor;
			case Types.NCLOB:
				return this.nClobTypeDescriptor;
			case Types.BLOB:
				return this.blobTypeDescriptor;
			case Types.TINYINT:
				// tinyint is unsigned on HANA
				return SmallIntTypeDescriptor.INSTANCE;
			case Types.BOOLEAN:
				return this.useLegacyBooleanType ? BitTypeDescriptor.INSTANCE : BooleanTypeDescriptor.INSTANCE;
			case Types.VARCHAR:
				return this.isUseUnicodeStringTypes() ? NVarcharTypeDescriptor.INSTANCE : VarcharTypeDescriptor.INSTANCE;
			case Types.CHAR:
				return this.isUseUnicodeStringTypes() ? NCharTypeDescriptor.INSTANCE : CharTypeDescriptor.INSTANCE;
			case Types.DOUBLE:
				return this.treatDoubleTypedFieldsAsDecimal ? DecimalTypeDescriptor.INSTANCE : DoubleTypeDescriptor.INSTANCE;
			default:
				return super.getSqlTypeDescriptorOverride( sqlCode );
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
		long timeoutInSeconds = getLockWaitTimeoutInSeconds( timeout );
		if ( timeoutInSeconds > 0 ) {
			return getForUpdateString() + " wait " + timeoutInSeconds;
		}
		else if ( timeoutInSeconds == 0 ) {
			return getForUpdateNowaitString();
		}
		else {
			return getForUpdateString();
		}
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		if ( timeout > 0 ) {
			return getForUpdateString( aliases ) + " wait " + getLockWaitTimeoutInSeconds( timeout );
		}
		else if ( timeout == 0 ) {
			return getForUpdateNowaitString( aliases );
		}
		else {
			return getForUpdateString( aliases );
		}
	}

	private long getLockWaitTimeoutInSeconds(int timeoutInMilliseconds) {
		Duration duration = Duration.ofMillis( timeoutInMilliseconds );
		long timeoutInSeconds = duration.getSeconds();
		if ( duration.getNano() != 0 ) {
			LOG.info( "Changing the query timeout from " + timeoutInMilliseconds + " ms to " + timeoutInSeconds
					+ " s, because HANA requires the timeout in seconds" );
		}
		return timeoutInSeconds;
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
			this.nClobTypeDescriptor = new HANANClobTypeDescriptor( maxLobPrefetchSize );
		}

		if ( this.blobTypeDescriptor.getMaxLobPrefetchSize() != maxLobPrefetchSize ) {
			this.blobTypeDescriptor = new HANABlobTypeDescriptor( maxLobPrefetchSize );
		}

		if ( supportsAsciiStringTypes() ) {
			this.useUnicodeStringTypes = configurationService.getSetting( USE_UNICODE_STRING_TYPES_PARAMETER_NAME, StandardConverters.BOOLEAN,
					useUnicodeStringTypesDefault() ).booleanValue();

			if ( this.isUseUnicodeStringTypes() ) {
				registerColumnType( Types.CHAR, "nvarchar(1)" );
				registerColumnType( Types.VARCHAR, 5000, "nvarchar($l)" );
				registerColumnType( Types.LONGVARCHAR, 5000, "nvarchar($l)" );

				// for longer values map to clob/nclob
				registerColumnType( Types.LONGVARCHAR, "nclob" );
				registerColumnType( Types.VARCHAR, "nclob" );
				registerColumnType( Types.CLOB, "nclob" );
			}

			if ( this.clobTypeDescriptor.getMaxLobPrefetchSize() != maxLobPrefetchSize
					|| this.clobTypeDescriptor.isUseUnicodeStringTypes() != this.isUseUnicodeStringTypes() ) {
				this.clobTypeDescriptor = new HANAClobTypeDescriptor( maxLobPrefetchSize, this.isUseUnicodeStringTypes() );
			}
		}

		this.useLegacyBooleanType = configurationService.getSetting( USE_LEGACY_BOOLEAN_TYPE_PARAMETER_NAME, StandardConverters.BOOLEAN,
				USE_LEGACY_BOOLEAN_TYPE_DEFAULT_VALUE ).booleanValue();

		if ( this.useLegacyBooleanType ) {
			registerColumnType( Types.BOOLEAN, "tinyint" );
		}

		this.treatDoubleTypedFieldsAsDecimal = configurationService.getSetting( TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_PARAMETER_NAME, StandardConverters.BOOLEAN,
				TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_DEFAULT_VALUE ).booleanValue();

		if ( this.treatDoubleTypedFieldsAsDecimal ) {
			registerHibernateType( Types.DOUBLE, StandardBasicTypes.BIG_DECIMAL.getName() );
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
	public Exporter<Table> getTableExporter() {
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

	@Override
	public boolean supportsNoColumnsInsert() {
		return false;
	}

	public boolean isUseUnicodeStringTypes() {
		return this.useUnicodeStringTypes;
	}

	protected abstract boolean supportsAsciiStringTypes();

	protected abstract Boolean useUnicodeStringTypesDefault();
}
