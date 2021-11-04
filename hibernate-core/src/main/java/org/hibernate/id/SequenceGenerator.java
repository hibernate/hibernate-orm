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
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * <b>sequence</b><br>
 * <br>
 * Generates <tt>long</tt> values using an oracle-style sequence. A higher
 * performance algorithm is <tt>SequenceHiLoGenerator</tt>.<br>
 * <br>
 * Mapping parameters supported: sequence, parameters.
 *
 * @see SequenceHiLoGenerator
 * @author Gavin King
 *
 * @deprecated Use {@link org.hibernate.id.enhanced.SequenceStyleGenerator} instead
 */
@Deprecated
public class SequenceGenerator
		implements PersistentIdentifierGenerator, BulkInsertionCapableIdentifierGenerator {

	private static final Logger LOG = Logger.getLogger( SequenceGenerator.class.getName() );

	/**
	 * The sequence parameter
	 */
	public static final String SEQUENCE = "sequence";

	/**
	 * The parameters parameter, appended to the create sequence DDL.
	 * For example (Oracle): <tt>INCREMENT BY 1 START WITH 1 MAXVALUE 100 NOCACHE</tt>.
	 *
	 * @deprecated No longer supported.  To specify initial-value or increment use the
	 * org.hibernate.id.enhanced.SequenceStyleGenerator generator instead.
	 */
	@Deprecated
	public static final String PARAMETERS = "parameters";

	private String contributor;

	private QualifiedName logicalQualifiedSequenceName;
	private QualifiedName physicalSequenceName;
	private Type identifierType;
	private String sql;

	protected Type getIdentifierType() {
		return identifierType;
	}

	public QualifiedName getPhysicalSequenceName() {
		return physicalSequenceName;
	}

	@Override
	@SuppressWarnings("StatementWithEmptyBody")
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		DeprecationLogger.DEPRECATION_LOGGER.deprecatedSequenceGenerator( getClass().getName() );

		identifierType = type;

		final ObjectNameNormalizer normalizer = (ObjectNameNormalizer) params.get( IDENTIFIER_NORMALIZER );
		logicalQualifiedSequenceName = QualifiedNameParser.INSTANCE.parse(
				ConfigurationHelper.getString( SEQUENCE, params, "hibernate_sequence" ),
				normalizer.normalizeIdentifierQuoting( params.getProperty( CATALOG ) ),
				normalizer.normalizeIdentifierQuoting( params.getProperty( SCHEMA ) )
		);

		if ( params.containsKey( PARAMETERS ) ) {
			LOG.warn(
					"Use of 'parameters' config setting is no longer supported; " +
							"to specify initial-value or increment use the " +
							"org.hibernate.id.enhanced.SequenceStyleGenerator generator instead."
			);
		}

		contributor = determineContributor( params );
	}

	private String determineContributor(Properties params) {
		final String contributor = params.getProperty( CONTRIBUTOR_NAME );
		return contributor == null ? "orm" : contributor;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object obj) {
		return generateHolder( session ).makeValue();
	}

	protected IntegralDataTypeHolder generateHolder(SharedSessionContractImplementor session) {
		try {
			final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
			PreparedStatement st = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
			final LogicalConnectionImplementor logicalConnection = jdbcCoordinator.getLogicalConnection();
			try {
				ResultSet rs = jdbcCoordinator.getResultSetReturn().extract( st );
				try {
					rs.next();
					IntegralDataTypeHolder result = buildHolder();
					result.initialize( rs, 1 );
					LOG.debugf( "Sequence identifier generated: %s", result );
					return result;
				}
				finally {
					logicalConnection.getResourceRegistry().release( rs, st );
				}
			}
			finally {
				logicalConnection.getResourceRegistry().release( st );
				jdbcCoordinator.afterStatementExecution();
			}

		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not get next sequence value",
					sql
			);
		}
	}

	protected IntegralDataTypeHolder buildHolder() {
		return IdentifierGeneratorHelper.getIntegralDataTypeHolder( identifierType.getReturnedClass() );
	}

	@Override
	public boolean supportsBulkInsertionIdentifierGeneration() {
		return true;
	}

	@Override
	public String determineBulkInsertionIdentifierGenerationSelectFragment(SqlStringGenerationContext context) {
		return context.getDialect().getSequenceSupport().getSelectSequenceNextValString( context.format( getPhysicalSequenceName() ) );
	}

	@Override
	public void registerExportables(Database database) {
		final Namespace namespace = database.locateNamespace(
				logicalQualifiedSequenceName.getCatalogName(),
				logicalQualifiedSequenceName.getSchemaName()
		);
		Sequence sequence = namespace.locateSequence( logicalQualifiedSequenceName.getObjectName() );
		if ( sequence != null ) {
			sequence.validate( 1, 1 );
		}
		else {
			sequence = namespace.createSequence(
					logicalQualifiedSequenceName.getObjectName(),
					(physicalName) -> new Sequence(
							contributor,
							namespace.getPhysicalName().getCatalog(),
							namespace.getPhysicalName().getSchema(),
							physicalName,
							1,
							1
					)
			);
		}
		this.physicalSequenceName = sequence.getName();
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		String formattedPhysicalSequenceName = context.format( physicalSequenceName );
		this.sql = context.getDialect().getSequenceSupport().getSequenceNextValString( formattedPhysicalSequenceName );
	}
}
