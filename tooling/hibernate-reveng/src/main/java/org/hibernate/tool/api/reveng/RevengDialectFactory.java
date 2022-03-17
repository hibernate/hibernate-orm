package org.hibernate.tool.api.reveng;

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.tool.internal.reveng.dialect.H2MetaDataDialect;
import org.hibernate.tool.internal.reveng.dialect.HSQLMetaDataDialect;
import org.hibernate.tool.internal.reveng.dialect.JDBCMetaDataDialect;
import org.hibernate.tool.internal.reveng.dialect.MySQLMetaDataDialect;
import org.hibernate.tool.internal.reveng.dialect.OracleMetaDataDialect;
import org.hibernate.tool.internal.reveng.dialect.SQLServerMetaDataDialect;

public class RevengDialectFactory {
	
	private RevengDialectFactory() {}

	public static RevengDialect createMetaDataDialect(Dialect dialect, Properties cfg) {
		String property = cfg.getProperty( "hibernatetool.metadatadialect" );
		RevengDialect mdd = fromClassName(property);
		if(mdd==null) {
			mdd = fromDialect(dialect);
		}
		if(mdd==null) {
			mdd = fromDialectName(dialect.getClass().getName());
		}
		if(mdd==null) {
			mdd = new JDBCMetaDataDialect();
		}
		return mdd;
	}

	public static RevengDialect fromClassName(String property) {
		if ( property != null ) {
			try {
				Class<?> revengDialectClass = ReflectHelper.classForName( 
						property,
						RevengDialectFactory.class );
				Constructor<?> revengDialectConstructor = revengDialectClass.getConstructor(
						new Class[] {});
				return (RevengDialect)revengDialectConstructor.newInstance();
			}
			catch (Throwable e) {
				throw new RuntimeException(
						"Could not load MetaDataDialect: " + property, e );
			}
		} else {
			return null;
		}
	}
	
	public static RevengDialect fromDialect(Dialect dialect) {
		if(dialect!=null) {  
			if(dialect instanceof OracleDialect) {
				return new OracleMetaDataDialect();
			} else if (dialect instanceof H2Dialect) {
				return new H2MetaDataDialect();
			} else if (dialect instanceof MySQLDialect) {
				return new MySQLMetaDataDialect();
			} else if (dialect instanceof HSQLDialect) {
				return new HSQLMetaDataDialect();
			}else if (dialect instanceof SQLServerDialect) {
				return new SQLServerMetaDataDialect();
			}			
		}
		return null;
	}
	
	public static RevengDialect fromDialectName(String dialect) {
		if (dialect.toLowerCase().contains("oracle")) {
			return new OracleMetaDataDialect();
		}
		if (dialect.toLowerCase().contains("mysql")) {
			return new MySQLMetaDataDialect();
		}
		if (dialect.toLowerCase().contains("h2")) {
			return new H2MetaDataDialect();
		}
		if (dialect.toLowerCase().contains("hsql")) {
			return new HSQLMetaDataDialect();
		}
		if (dialect.toLowerCase().contains("sqlserver")) {
			return new SQLServerMetaDataDialect();
		}
		return null;
	}


}
