/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

/// Projects Dialect-specific migration facts from the aggregate Jandex index,
/// canonical classification model, and compiled bytecode linkage.
///
/// @author Steve Ebersole
final class DialectExtensionInventoryAnalyzer {
	private static final String DIALECT_CLASS = "org.hibernate.dialect.Dialect";
	private static final String DIALECT_TYPE_ID = "type:" + DIALECT_CLASS;
	private static final String COMMUNITY_PACKAGE = "org.hibernate.community.dialect.";
	private static final Set<String> SELECTION_TYPES = Set.of(
			"type:org.hibernate.boot.registry.selector.internal.DefaultDialectSelector",
			"type:org.hibernate.engine.jdbc.dialect.spi.DialectResolver"
	);

	private static final Map<String, String> EXTENSION_FAMILIES = extensionFamilies();

	DialectExtensionInventory analyze(
			IndexView index,
			ClassificationModel classifications,
			Collection<BytecodeLinkageAnalyzer.Link> bytecodeLinks,
			ClassLoader classLoader,
			Collection<File> supportDocumentationFiles) {
		final List<DialectExtensionInventory.SurfaceDeclaration> surface = dialectSurface( classifications );
		final Map<String, String> surfaceBySignature = surfaceBySignature( surface );
		final Set<String> dialectTypes = dialectTypes( index );
		final List<DialectExtensionInventory.Relationship> hierarchy = hierarchy( index, dialectTypes );
		final List<DialectExtensionInventory.Relationship> overrides = overrides(
				index,
				dialectTypes,
				surfaceBySignature
		);
		final List<BytecodeLinkageAnalyzer.Link> calls = new ArrayList<>();
		final List<DialectExtensionInventory.Dependency> internalDependencies = new ArrayList<>();
		final List<DialectExtensionInventory.ExtensionUse> extensionUses = new ArrayList<>();

		for ( BytecodeLinkageAnalyzer.Link link : bytecodeLinks ) {
			final String targetOwner = ownerType( link.getTargetElementId() );
			if ( isDialectCall( link, targetOwner, dialectTypes, surfaceBySignature ) ) {
				calls.add( link );
			}
			if ( !link.getSourceClass().startsWith( COMMUNITY_PACKAGE ) ) {
				continue;
			}

			final ClassificationModel.Element target = resolveTarget( classifications, link.getTargetElementId() );
			if ( !targetOwner.startsWith( "org.hibernate.community." )
					&& target != null
					&& target.getCategory() == ClassificationModel.Category.INTERNAL ) {
				internalDependencies.add(
						new DialectExtensionInventory.Dependency( link, target.getId(), target.getCategory().name() )
				);
			}
			final String family = extensionFamily( targetOwner );
			if ( family != null ) {
				extensionUses.add( new DialectExtensionInventory.ExtensionUse( family, link ) );
			}
		}

		final DialectSelectionInventoryAnalyzer.SelectionFacts selectionFacts =
				new DialectSelectionInventoryAnalyzer().analyze(
						index,
						classifications,
						bytecodeLinks,
						classLoader,
						supportDocumentationFiles
				);
		return new DialectExtensionInventory(
				surface,
				overrides,
				hierarchy,
				calls,
				internalDependencies,
				extensionUses,
				selectionMetadata( classifications, index ),
				selectionFacts.getMechanisms(),
				selectionFacts.getDialectSelections()
		);
	}

	private static List<DialectExtensionInventory.SurfaceDeclaration> dialectSurface(
			ClassificationModel classifications) {
		final List<DialectExtensionInventory.SurfaceDeclaration> surface = new ArrayList<>();
		for ( ClassificationModel.Element element : classifications.getElements() ) {
			if ( DIALECT_TYPE_ID.equals( element.getOwnerId() )
					&& element.getStructure().isExternallyAccessible() ) {
				surface.add(
						new DialectExtensionInventory.SurfaceDeclaration(
								element,
								reviewGroup( element ),
								reachableSignatureElements( classifications, element )
						)
				);
			}
		}
		return surface;
	}

	private static Set<String> reachableSignatureElements(
			ClassificationModel model,
			ClassificationModel.Element source) {
		final Set<String> reachable = new HashSet<>();
		final ArrayDeque<String> pending = new ArrayDeque<>();
		for ( ClassificationModel.Reference reference : source.getReferences() ) {
			if ( reference.getTarget() == ClassificationModel.ReferenceTarget.HIBERNATE
					&& reachable.add( reference.getTargetElementId() ) ) {
				pending.addLast( reference.getTargetElementId() );
			}
		}
		while ( !pending.isEmpty() ) {
			final ClassificationModel.Element current = model.getElement( pending.removeFirst() );
			if ( current == null ) {
				continue;
			}
			for ( ClassificationModel.Reference reference : current.getReferences() ) {
				if ( reference.getTarget() == ClassificationModel.ReferenceTarget.HIBERNATE
						&& reachable.add( reference.getTargetElementId() ) ) {
					pending.addLast( reference.getTargetElementId() );
				}
			}
		}
		return reachable;
	}

	private static Map<String, String> surfaceBySignature(
			Collection<DialectExtensionInventory.SurfaceDeclaration> surface) {
		final Map<String, String> result = new HashMap<>();
		for ( DialectExtensionInventory.SurfaceDeclaration declaration : surface ) {
			if ( declaration.getElementId().startsWith( "method:" ) ) {
				result.put( memberSignature( declaration.getElementId() ), declaration.getElementId() );
			}
		}
		return result;
	}

	private static Set<String> dialectTypes(IndexView index) {
		final Set<String> types = new HashSet<>();
		types.add( DIALECT_CLASS );
		for ( ClassInfo subclass : index.getAllKnownSubclasses( DotName.createSimple( DIALECT_CLASS ) ) ) {
			types.add( subclass.name().toString() );
		}
		return types;
	}

	private static List<DialectExtensionInventory.Relationship> hierarchy(
			IndexView index,
			Set<String> dialectTypes) {
		final List<DialectExtensionInventory.Relationship> result = new ArrayList<>();
		for ( String type : dialectTypes ) {
			if ( DIALECT_CLASS.equals( type ) ) {
				continue;
			}
			final ClassInfo classInfo = index.getClassByName( DotName.createSimple( type ) );
			if ( classInfo != null && classInfo.superName() != null ) {
				result.add(
						new DialectExtensionInventory.Relationship(
								"type:" + type,
								"type:" + classInfo.superName()
						)
				);
			}
		}
		return result;
	}

	private static List<DialectExtensionInventory.Relationship> overrides(
			IndexView index,
			Set<String> dialectTypes,
			Map<String, String> surfaceBySignature) {
		final List<DialectExtensionInventory.Relationship> result = new ArrayList<>();
		for ( String type : dialectTypes ) {
			if ( DIALECT_CLASS.equals( type ) ) {
				continue;
			}
			final ClassInfo classInfo = index.getClassByName( DotName.createSimple( type ) );
			if ( classInfo == null ) {
				continue;
			}
			for ( MethodInfo method : classInfo.methods() ) {
				if ( method.isConstructor() || method.isStaticInitializer() || method.isSynthetic() || method.isBridge() ) {
					continue;
				}
				final String methodId = JandexClassificationClassifier.methodId( method );
				final String overridden = surfaceBySignature.get( memberSignature( methodId ) );
				if ( overridden != null ) {
					result.add( new DialectExtensionInventory.Relationship( methodId, overridden ) );
				}
			}
		}
		return result;
	}

	private static boolean isDialectCall(
			BytecodeLinkageAnalyzer.Link link,
			String targetOwner,
			Set<String> dialectTypes,
			Map<String, String> surfaceBySignature) {
		return "METHOD_CALL".equals( link.getKind() )
				&& dialectTypes.contains( targetOwner )
				&& surfaceBySignature.containsKey( memberSignature( link.getTargetElementId() ) );
	}

	private static ClassificationModel.Element resolveTarget(ClassificationModel model, String targetId) {
		final ClassificationModel.Element exact = model.getElement( targetId );
		return exact == null ? model.getElement( "type:" + ownerType( targetId ) ) : exact;
	}

	private static List<String> selectionMetadata(ClassificationModel model, IndexView index) {
		final Set<String> relevantTypes = new HashSet<>( SELECTION_TYPES );
		for ( String typeId : SELECTION_TYPES ) {
			final DotName name = DotName.createSimple( typeId.substring( "type:".length() ) );
			for ( ClassInfo implementation : index.getAllKnownImplementations( name ) ) {
				relevantTypes.add( "type:" + implementation.name() );
			}
			for ( ClassInfo subclass : index.getAllKnownSubclasses( name ) ) {
				relevantTypes.add( "type:" + subclass.name() );
			}
		}
		final List<String> result = new ArrayList<>();
		for ( ClassificationModel.Element element : model.getElements() ) {
			if ( relevantTypes.contains( element.getId() ) || relevantTypes.contains( element.getOwnerId() ) ) {
				result.add( element.getId() );
			}
		}
		return result;
	}

	private static String extensionFamily(String targetOwner) {
		for ( Map.Entry<String, String> entry : EXTENSION_FAMILIES.entrySet() ) {
			if ( targetOwner.startsWith( entry.getKey() ) ) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static Map<String, String> extensionFamilies() {
		final Map<String, String> families = new LinkedHashMap<>();
		families.put( "org.hibernate.sql.ast.", "SQL_AST" );
		families.put( "org.hibernate.sql.model.", "MODEL_MUTATION" );
		families.put( "org.hibernate.query.sqm.", "SQM" );
		families.put( "org.hibernate.query.hql.", "HQL" );
		families.put( "org.hibernate.dialect.lock.", "LOCKING" );
		families.put( "org.hibernate.tool.schema.", "SCHEMA" );
		families.put( "org.hibernate.dialect.sequence.", "SEQUENCE" );
		families.put( "org.hibernate.dialect.function.", "FUNCTION" );
		families.put( "org.hibernate.type.", "TYPE" );
		families.put( "org.hibernate.procedure.", "CALLABLE" );
		return families;
	}

	private static String reviewGroup(ClassificationModel.Element element) {
		if ( element.getKind() == ClassificationModel.ElementKind.CONSTRUCTOR ) {
			return "CONSTRUCTION";
		}
		final String id = element.getId().toLowerCase();
		if ( id.contains( "translator" ) || id.contains( "sqlast" ) || id.contains( "sqm" ) || id.contains( "hql" ) ) {
			return "TRANSLATION";
		}
		if ( id.contains( "type" ) || id.contains( "function" ) ) {
			return "TYPE_AND_FUNCTION";
		}
		if ( id.contains( "schema" ) || id.contains( "ddl" ) || id.contains( "sequence" ) ) {
			return "DDL_AND_SCHEMA";
		}
		if ( id.contains( "#supports" ) ) {
			return "GENERAL_CAPABILITY";
		}
		if ( id.contains( "support" ) || id.contains( "strategy" ) || id.contains( "factory" ) ) {
			return "SUPPLIED_STRATEGY";
		}
		return "GENERAL_CAPABILITY";
	}

	private static String memberSignature(String elementId) {
		final int separator = elementId.indexOf( '#' );
		return separator < 0 ? elementId : elementId.substring( separator + 1 );
	}

	private static String ownerType(String elementId) {
		final int colon = elementId.indexOf( ':' );
		final int hash = elementId.indexOf( '#', colon + 1 );
		return elementId.substring( colon + 1, hash < 0 ? elementId.length() : hash );
	}
}
