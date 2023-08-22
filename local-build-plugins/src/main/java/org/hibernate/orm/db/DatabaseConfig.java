/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.db;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

/**
 * Configuration for access to a database for testing
 *
 * @author Steve Ebersole
 */
public class DatabaseConfig implements Named, Serializable {
	public static final String EMPTY = "";

	private final String name;

	private final Property<String> dialect;
	private final Property<String> driver;
	private final Property<String> url;
	private final Property<String> username;
	private final Property<String> password;
	private final Property<String> initSql;
	private final Property<Boolean> useNativeParamMarkers;

	@Inject
	public DatabaseConfig(String name, Project project) {
		this.name = name;

		this.dialect = project.getObjects().property( String.class );

		this.driver = project.getObjects().property( String.class );
		this.url = project.getObjects().property( String.class );
		this.username = project.getObjects().property( String.class );
		this.password = project.getObjects().property( String.class );
		this.password.convention( EMPTY );

		this.initSql = project.getObjects().property( String.class );
		this.initSql.convention( EMPTY );

		this.useNativeParamMarkers = project.getObjects().property( Boolean.class );
		this.useNativeParamMarkers.convention( false );


	}

	@Input
	@Override
	public String getName() {
		return name;
	}


	@Input
	public Property<String> getDialect() {
		return dialect;
	}

	public void setDialect(String dialect) {
		this.dialect.set( dialect );
	}

	public void setDialect(Provider<String> dialect) {
		this.dialect.set( dialect );
	}

	@Input
	public Property<String> getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver.set( driver );
	}

	public void setDriver(Provider<String> driver) {
		this.driver.set( driver );
	}

	@Input
	public Property<String> getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url.set( url );
	}

	public void setUrl(Provider<String> url) {
		this.url.set( url );
	}

	@Input
	public Property<String> getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username.set( username );
	}

	public void setUsername(Provider<String> username) {
		this.username.set( username );
	}

	@Input
	public Property<String> getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password.set( password );
	}

	public void setPassword(Provider<String> password) {
		this.password.set( password );
	}

	@Input
	public Property<String> getInitSql() {
		return initSql;
	}

	public void setInitSql(String initSql) {
		this.initSql.set( initSql );
	}

	public void setInitSql(Provider<String> initSql) {
		this.initSql.set( initSql );
	}

	@Input
	public Property<Boolean> getUseNativeParamMarkers() {
		return useNativeParamMarkers;
	}

	public void setUseNativeParamMarkers(boolean enable) {
		this.useNativeParamMarkers.set( enable );
	}

	public void setUseNativeParamMarkers(Provider<Boolean> enable) {
		this.useNativeParamMarkers.set( enable );
	}

	@Internal
	public final Map<String,String> getSettings() {
		final HashMap<String, String> result = new HashMap<>();
		result.put( "db.dialect", dialect.get() );
		result.put( "jdbc.driver", driver.get() );
		result.put( "jdbc.url", url.get() );
		result.put( "jdbc.user", username.get() );
		result.put( "jdbc.pass", password.get() );
		result.put( "connection.init_sql", initSql.get() );
		result.put( "hibernate.dialect.native_param_markers", useNativeParamMarkers.get().toString() );
		return result;
	}
}
