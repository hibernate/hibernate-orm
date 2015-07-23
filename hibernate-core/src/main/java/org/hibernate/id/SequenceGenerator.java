/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
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
		implements PersistentIdentifierGenerator, BulkInsertionCapableIdentifierGenerator, Configurable {

	private static final Logger LOG = Logger.getLogger( SequenceGenerator.class.getName() );

	/**
	 * The sequence parameter
	 */
	public static final String SEQUENCE = "sequence";

	/**
	 * The parameters parameter, appended to the create sequence DDL.
	 * For example (Oracle): <tt>INCREMENT BY 1 START WITH 1 MAXVALUE 100 NOCACHE</tt>.
	 */
	public static final String PARAMETERS = "parameters";

	private QualifiedName qualifiedSequenceName;
	private String sequenceName;
	private String parameters;
	private Type identifierType;
	private String sql;

	protected Type getIdentifierType() {
		return identifierType;
	}

	public Object generatorKey() {
		return getSequenceName();
	}

	public String getSequenceName() {
		return sequenceName;
	}

	@Override
	@SuppressWarnings("StatementWithEmptyBody")
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		identifierType = type;
		parameters = params.getProperty( PARAMETERS );

		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		final Dialect dialect = jdbcEnvironment.getDialect();
		final ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );
		qualifiedSequenceName = QualifiedNameParser.INSTANCE.parse(
				ConfigurationHelper.getString( SEQUENCE, params, "hibernate_sequence" ),
				normalizer.normalizeIdentifierQuoting( params.getProperty( CATALOG ) ),
				normalizer.normalizeIdentifierQuoting( params.getProperty( SCHEMA ) )
		);
		sequenceName = jdbcEnvironment.getQualifiedObjectNameFormatter().format( qualifiedSequenceName, dialect );

		sql = dialect.getSequenceNextValString( sequenceName );
	}

	@Override
	public Serializable generate(SessionImplementor session, Object obj) {
		return generateHolder( session ).makeValue();
	}

	protected IntegralDataTypeHolder generateHolder(SessionImplementor session) {
		try {
			PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
			try {
				ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st );
				try {
					rs.next();
					IntegralDataTypeHolder result = buildHolder();
					result.initialize( rs, 1 );
					LOG.debugf( "Sequence identifier generated: %s", result );
					return result;
				}
				finally {
					session.getJdbcCoordinator().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				session.getJdbcCoordinator().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}

		}
		catch (SQLException sqle) {
			throw session.getFactory().getSQLExceptionHelper().convert(
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
	@SuppressWarnings( {"deprecation"})
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		String[] ddl = dialect.getCreateSequenceStrings( sequenceName );
		if ( parameters != null ) {
			ddl[ddl.length - 1] += ' ' + parameters;
		}
		return ddl;
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return dialect.getDropSequenceStrings(sequenceName);
	}

	@Override
	public boolean supportsBulkInsertionIdentifierGeneration() {
		return true;
	}

	@Override
	public String determineBulkInsertionIdentifierGenerationSelectFragment(Dialect dialect) {
		return dialect.getSelectSequenceNextValString( getSequenceName() );
	}

	@Override
	public void registerExportables(Database database) {
		// we cannot register a proper Sequence object here because of the free-form
		//'parameters' as opposed to specific initialValue/increment values

		final Namespace namespace = database.locateNamespace(
				qualifiedSequenceName.getCatalogName(),
				qualifiedSequenceName.getSchemaName()
		);

		database.addAuxiliaryDatabaseObject(
				new SimpleAuxiliaryDatabaseObject(
						namespace,
						sqlCreateStrings( database.getDialect() ),
						sqlDropStrings( database.getDialect() ),
						Collections.<String>emptySet()
				)
		);
	}
}
