/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import static org.hibernate.id.IdentifierGeneratorHelper.getIntegralDataTypeHolder;
import static org.hibernate.id.PersistentIdentifierGenerator.CATALOG;
import static org.hibernate.id.PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER;
import static org.hibernate.id.PersistentIdentifierGenerator.PK;
import static org.hibernate.id.PersistentIdentifierGenerator.SCHEMA;
import static org.hibernate.internal.util.StringHelper.split;

/**
 * An {@link IdentifierGenerator} that returns a {@code long}, constructed by counting
 * from the maximum primary key value obtained by querying the table or tables at startup.
 * <p>
 * This id generator is not safe unless a single VM has exclusive access to the database.
 * <p>
 * Mapping parameters supported, but not usually needed: {@value #TABLES}, {@value #COLUMN}.
 * (The {@value #TABLES} parameter specifies a comma-separated list of table names.)
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Brett Meyer
 *
 * @implNote This also implements the {@code increment} generation type in {@code hbm.xml} mappings.
 */
public class IncrementGenerator implements IdentifierGenerator, StandardGenerator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( IncrementGenerator.class );

	/**
	 * A parameter identifying the column holding the id.
	 */
	public static final String COLUMN = "column";
	/**
	 * A parameter specifying a list of tables over which the generated id should be unique.
	 */
	public static final String TABLES = "tables";

	private Class<?> returnClass;
	private String column;
	private List<QualifiedTableName> physicalTableNames;
	private String sql;

	private IntegralDataTypeHolder previousValueHolder;

	/**
	 * @deprecated Exposed for tests only.
	 */
	@Deprecated
	public String[] getAllSqlForTests() {
		return new String[] { sql };
	}

	@Override
	public synchronized Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		if ( sql != null ) {
			initializePreviousValueHolder( session );
		}
		return previousValueHolder.makeValueThenIncrement();
	}

	@Override
	public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) throws MappingException {
		returnClass = type.getReturnedClass();

		final JdbcEnvironment jdbcEnvironment = serviceRegistry.requireService( JdbcEnvironment.class );
		final ObjectNameNormalizer normalizer =
				(ObjectNameNormalizer) parameters.get( IDENTIFIER_NORMALIZER );

		column = parameters.getProperty( COLUMN );
		if ( column == null ) {
			column = parameters.getProperty( PK );
		}
		column = normalizer.normalizeIdentifierQuoting( column ).render( jdbcEnvironment.getDialect() );

		IdentifierHelper identifierHelper = jdbcEnvironment.getIdentifierHelper();

		final String schema = normalizer.toDatabaseIdentifierText(
				parameters.getProperty( SCHEMA )
		);
		final String catalog = normalizer.toDatabaseIdentifierText(
				parameters.getProperty( CATALOG )
		);

		String tableList = parameters.getProperty( TABLES );
		if ( tableList == null ) {
			tableList = parameters.getProperty( PersistentIdentifierGenerator.TABLES );
		}
		physicalTableNames = new ArrayList<>();
		for ( String tableName : split( ", ", tableList ) ) {
			physicalTableNames.add( new QualifiedTableName( identifierHelper.toIdentifier( catalog ),
					identifierHelper.toIdentifier( schema ), identifierHelper.toIdentifier( tableName ) ) );
		}
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		StringBuilder union = new StringBuilder();
		for ( int i = 0; i < physicalTableNames.size(); i++ ) {
			final String tableName = context.format( physicalTableNames.get( i ) );
			if ( physicalTableNames.size() > 1 ) {
				union.append( "select max(" ).append( column ).append( ") as mx from " );
			}
			union.append( tableName );
			final Dialect dialect = context.getDialect();
			if ( i < physicalTableNames.size() - 1 ) {
				union.append( " union " );
				if ( dialect.supportsUnionAll() ) {
					union.append( "all " );
				}
			}
		}
		String maxColumn;
		if ( physicalTableNames.size() > 1 ) {
			union.insert( 0, "( " ).append( " ) ids_" );
			maxColumn = "ids_.mx";
		}
		else {
			maxColumn = column;
		}

		sql = "select max(" + maxColumn + ") from " + union;
	}

	private void initializePreviousValueHolder(SharedSessionContractImplementor session) {
		previousValueHolder = getIntegralDataTypeHolder( returnClass );

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Fetching initial value: %s", sql );
		}
		try {
			PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
			try {
				ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st, sql );
				try {
					if ( rs.next() ) {
						previousValueHolder.initialize( rs, 0L ).increment();
					}
					else {
						previousValueHolder.initialize( 1L );
					}
					sql = null;
					if ( LOG.isDebugEnabled() ) {
						LOG.debugf( "First free id: %s", previousValueHolder.makeValue() );
					}
				}
				finally {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not fetch initial value for increment generator",
					sql
			);
		}
	}
}
