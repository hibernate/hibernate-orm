package org.hibernate;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class TestingDatabaseInfo {
	public static volatile String DRIVER = "org.h2.Driver";
	public static volatile String URL = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE";
	public static volatile String USER = "sa";
	public static volatile String PASS = "";

	public static final Dialect DIALECT = new H2Dialect();

	public static Configuration buildBaseConfiguration() {
		return new Configuration()
				.setProperty( Environment.DRIVER, DRIVER )
				.setProperty( Environment.URL, URL )
				.setProperty( Environment.USER, USER )
				.setProperty( Environment.PASS, PASS )
				.setProperty( Environment.DIALECT, DIALECT.getClass().getName() );
	}
}
