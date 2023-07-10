/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.model.ImportContext;
import org.hibernate.jpamodelgen.model.MetaAttribute;

import javax.lang.model.element.PackageElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to collect meta information about an annotated package.
 *
 * @author Gavin King
 */
public class AnnotationMetaPackage extends AnnotationMeta {

	private final ImportContext importContext;
	private final PackageElement element;
	private final Map<String, MetaAttribute> members;
	private final Context context;

	/**
	 * Whether the members of this type have already been initialized or not.
	 */
	private boolean initialized;

	public AnnotationMetaPackage(PackageElement element, Context context) {
		this.element = element;
		this.context = context;
		this.members = new HashMap<>();
		this.importContext = new ImportContextImpl( getPackageName( context, element ) );
	}

	public static AnnotationMetaPackage create(PackageElement element, Context context) {
		return new AnnotationMetaPackage( element, context );
	}

	@Override
	public final Context getContext() {
		return context;
	}

	@Override
	public boolean isImplementation() {
		return false;
	}

	@Override
	public final String getSimpleName() {
		return element.getSimpleName().toString();
	}

	@Override
	public final String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	@Override
	public final String getPackageName() {
		return getPackageName( context, element );
	}

	private static String getPackageName(Context context, PackageElement packageOf) {
		return context.getElementUtils().getName( packageOf.getQualifiedName() ).toString();
	}

	@Override
	public List<MetaAttribute> getMembers() {
		if ( !initialized ) {
			init();
		}

		return new ArrayList<>( members.values() );
	}

	@Override
	public boolean isMetaComplete() {
		return false;
	}

	@Override
	public final String generateImports() {
		return importContext.generateImports();
	}

	@Override
	public final String importType(String fqcn) {
		return importContext.importType( fqcn );
	}

	@Override
	public final String staticImport(String fqcn, String member) {
		return importContext.staticImport( fqcn, member );
	}

	@Override
	public final PackageElement getElement() {
		return element;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( "AnnotationMetaPackage" )
				.append( "{element=" )
				.append( element )
				.append( '}' )
				.toString();
	}

	protected final void init() {
		getContext().logMessage( Diagnostic.Kind.OTHER, "Initializing type " + getQualifiedName() + "." );

		addAuxiliaryMembers();

		checkNamedQueries();

		initialized = true;
	}

	@Override
	void putMember(String name, MetaAttribute nameMetaAttribute) {
		members.put( name, nameMetaAttribute );
	}

	@Override
	boolean belongsToDao() {
		return false;
	}

	@Override
	@Nullable String getSessionType() {
		return null;
	}

	@Override
	public boolean isInjectable() {
		return false;
	}
}
