/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.processor.util.Constants;

import java.util.List;

import static org.hibernate.processor.util.Constants.LIST;

/**
 * @author Gavin King
 */
public class CriteriaFinderMethod extends AbstractCriteriaMethod {

	private final @Nullable String containerType;
	private final List<Boolean> paramNullability;

	CriteriaFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName, String entity,
			@Nullable String containerType,
			List<String> paramNames, List<String> paramTypes,
			List<Boolean> paramNullability,
			List<Boolean> multivalued,
			List<Boolean> paramPatterns,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean dataRepository) {
		super( annotationMetaEntity, methodName, entity, belongsToDao, sessionType, sessionName, fetchProfiles,
				paramNames, paramTypes, orderBys, addNonnullAnnotation, dataRepository, multivalued, paramPatterns );
		this.containerType = containerType;
		this.paramNullability = paramNullability;
	}

	@Override
	public boolean isNullable(int index) {
		return paramNullability.get(index);
	}

	@Override
	boolean singleResult() {
		return containerType == null;
	}

	@Override
	void executeQuery(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append('\n');
		collectOrdering( declaration, paramTypes );
		tryReturn( declaration, paramTypes, containerType );
		castResult( declaration );
		createQuery( declaration );
		handlePageParameters( declaration, paramTypes, containerType );
		boolean unwrapped = specialNeeds( declaration );
		unwrapped = applyOrder( declaration, paramTypes, containerType, unwrapped );
		execute( declaration, paramTypes, unwrapped );
	}

	private void castResult(StringBuilder declaration) {
		if ( containerType == null && !fetchProfiles.isEmpty()
				&& isUsingEntityManager() ) {
			declaration
					.append("(")
					.append(annotationMetaEntity.importType(entity))
					.append(") ");
		}
	}

	private boolean specialNeeds(StringBuilder declaration) {
		boolean unwrapped = !isUsingEntityManager();
		unwrapped = enableFetchProfile( declaration, unwrapped );
		unwrapped = unwrapIfNecessary( declaration, containerType, unwrapped );
		return unwrapped;
	}

	private void execute(StringBuilder declaration, List<String> paramTypes, boolean unwrapped) {
		final boolean mustUnwrap =
				containerType != null && containerType.startsWith("org.hibernate");
		executeSelect( declaration, paramTypes, containerType, unwrapped, mustUnwrap );
	}

	@Override
	void createCriteriaQuery(StringBuilder declaration) {
		declaration
				.append("\tvar _query = _builder.createQuery(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class);\n")
				.append("\tvar _entity = _query.from(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class);\n");
	}

	@Override
	String returnType() {
		final StringBuilder type = new StringBuilder();
		if ( "[]".equals(containerType) ) {
			if ( returnTypeName == null ) {
				throw new AssertionFailure("array return type, but no type name");
			}
			type.append(annotationMetaEntity.importType(returnTypeName)).append("[]");
		}
		else {
			boolean returnsUni = isReactive()
					&& (containerType == null || LIST.equals(containerType));
			if ( returnsUni ) {
				type.append(annotationMetaEntity.importType(Constants.UNI)).append('<');
			}
			if ( containerType != null ) {
				type.append(annotationMetaEntity.importType(containerType)).append('<');
			}
			type.append(annotationMetaEntity.importType(entity));
			if ( containerType != null ) {
				type.append('>');
			}
			if ( returnsUni ) {
				type.append('>');
			}
		}
		return type.toString();
	}
}
