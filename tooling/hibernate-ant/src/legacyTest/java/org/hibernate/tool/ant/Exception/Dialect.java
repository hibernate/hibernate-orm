package org.hibernate.tool.ant.Exception;


import org.hibernate.dialect.DatabaseVersion;

public class Dialect extends org.hibernate.dialect.Dialect {
	public Dialect() {
		super((DatabaseVersion)null);
	}
}
