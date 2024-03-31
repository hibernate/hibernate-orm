/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.Constants;

/**
 * A general purpose constructor which accepts the session.
 *
 * @author Gavin King
 */
public class RepositoryConstructor implements MetaAttribute {
	private final AnnotationMetaEntity annotationMetaEntity;
	private final String constructorName;
	private final String methodName;
	private final String sessionTypeName;
	private final String sessionVariableName;
	private final @Nullable String dataStore;
	private final boolean addInjectAnnotation;
	private final boolean addNonnullAnnotation;
	private final boolean addOverrideAnnotation;
	private final boolean dataRepository;
	private final boolean quarkusInjection;

	public RepositoryConstructor(
			AnnotationMetaEntity annotationMetaEntity,
			String constructorName,
			String methodName,
			String sessionTypeName,
			String sessionVariableName,
			@Nullable String dataStore,
			boolean addInjectAnnotation,
			boolean addNonnullAnnotation,
			boolean addOverrideAnnotation,
			boolean dataRepository,
			boolean quarkusInjection) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.constructorName = constructorName;
		this.methodName = methodName;
		this.sessionTypeName = sessionTypeName;
		this.sessionVariableName = sessionVariableName;
		this.dataStore = dataStore;
		this.addInjectAnnotation = addInjectAnnotation;
		this.addNonnullAnnotation = addNonnullAnnotation;
		this.addOverrideAnnotation = addOverrideAnnotation;
		this.dataRepository = dataRepository;
		this.quarkusInjection = quarkusInjection;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		declaration
				.append('\n');
		if ( annotationMetaEntity.getSupertypeName() == null ) {
			declaration
					.append("protected ");
			if ( !dataRepository ) {
				// don't mark the field final
				// because it will be initialized
				// in @PostConstruct
				declaration
						.append("final ");
			}
			notNull( declaration );
			declaration
					.append(annotationMetaEntity.importType(sessionTypeName))
					.append(" ")
					.append(sessionVariableName)
					.append(";\n\n");
		}
		inject( declaration );
		declaration
				.append("public ")
				.append(constructorName)
				.append("(");
		notNull( declaration );
		qualifier( declaration );
		declaration
				.append(annotationMetaEntity.importType(sessionTypeName))
				.append(" ")
				.append(sessionVariableName)
				.append(") {\n");
		if ( annotationMetaEntity.getSupertypeName() != null ) {
			declaration
					.append("\tsuper(")
					.append(sessionVariableName)
					.append(");\n");
		}
		else {
			declaration
					.append("\tthis.")
					.append(sessionVariableName)
					.append(" = ")
					.append(sessionVariableName)
					.append(";\n");
		}
		declaration
				.append("}");
		if ( annotationMetaEntity.getSupertypeName() == null ) {
			declaration
					.append("\n\n");
			if (addOverrideAnnotation) {
				declaration.append("@Override\n");
			}
			declaration
					.append("public ");
			notNull( declaration );
			declaration
					.append(annotationMetaEntity.importType(sessionTypeName))
					.append(" ")
					.append(methodName)
					.append("() {")
					.append("\n\treturn ")
					.append(sessionVariableName)
					.append(";\n}");
		}
		return declaration.toString();
	}

	/**
	 * In Quarkus we use the Quarkus-specific {@code @PersistenceUnit}
	 * CDI qualifier annotation to inject the {@code StatelessSession}
	 * directly.
	 */
	private void qualifier(StringBuilder declaration) {
		if ( addInjectAnnotation && quarkusInjection && dataStore != null ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType("io.quarkus.hibernate.orm.PersistenceUnit"))
					.append("(\"")
					.append(dataStore)
					.append("\") ");
		}
	}

	/**
	 * In Quarkus we inject the {@code StatelessSession}
	 * directly via the constructor. But this doesn't work
	 * in other CDI implementations, where we need to use
	 * the JPA {@code @PersistenceUnit} annotation for
	 * field injection of an {@code EntityManager}. In
	 * that case, CDI will instantiate the repository via
	 * a {@link DefaultConstructor default constructor},
	 * so we don't need to mark this one {@code @Inject}.
	 */
	private void inject(StringBuilder declaration) {
		// Jakarta Data repositories are instantiated
		// via the default constructor, so in that
		// case, this one is just for testing, unless
		// we are in Quarkus where we can use
		// constructor injection
		if ( addInjectAnnotation && !annotationMetaEntity.needsDefaultConstructor() ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType("jakarta.inject.Inject"))
					.append('\n');
		}
	}

	private void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType("jakarta.annotation.Nonnull"))
					.append(' ');
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	@Override
	public String getTypeDeclaration() {
		return Constants.ENTITY_MANAGER;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
