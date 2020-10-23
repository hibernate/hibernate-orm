/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;

import static java.lang.Character.LINE_SEPARATOR;

/**
 * @author Steve Ebersole
 */
public abstract class AttributeSupport implements MetamodelAttribute {
	private final MetamodelClass metamodelClass;
	private final String name;
	private final Class javaType;

	public AttributeSupport(
			MetamodelClass metamodelClass,
			String name,
			Class javaType) {
		this.metamodelClass = metamodelClass;
		this.name = name;
		this.javaType = javaType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<?> getAttributeJavaType() {
		return javaType;
	}

	public String getOwnerDomainClassName() {
		return metamodelClass.getMetamodelClassName();
	}

	@Override
	public void renderJpaMembers(BufferedWriter writer) {
		try {
			writer.write( "    public static volatile " );
			renderAttributeType( writer );
			writer.write( " " + name );
			writer.write( ';' );
			writer.write( LINE_SEPARATOR );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Problem writing attribute `" + metamodelClass.getMetamodelClassName() + "#" + name + "` to output stream", e );
		}
	}

	public abstract void renderAttributeType(BufferedWriter writer) throws IOException;

	protected String format(String pattern, Object... args) {
		return String.format( Locale.ROOT, pattern, args );
	}

	@Override
	public void renderNameConstant(BufferedWriter writer) {
		try {
			writer.write( "    public static final String " );
			writer.write( getName().toUpperCase( Locale.ROOT ) );
			writer.write( " = \"" + getName() + "\";" + LINE_SEPARATOR );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Problem writing attribute `" + metamodelClass.getMetamodelClassName() + "#" + name + "` to output stream", e );
		}
	}
}
