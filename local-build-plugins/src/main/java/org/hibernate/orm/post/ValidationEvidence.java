/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Evidence supplied by source-consumer, Javadoc, and compatibility adapters.
///
/// Compiled category and structure rules are derived directly from
/// [ClassificationModel]; this type is only for evidence that the compiled
/// classification document does not contain.
///
/// @author Steve Ebersole
public final class ValidationEvidence {
	public static final ValidationEvidence NONE = builder().build();

	private final Map<ValidationRule, List<Item>> items;

	private ValidationEvidence(Map<ValidationRule, List<Item>> items) {
		this.items = items;
	}

	public List<Item> get(ValidationRule rule) {
		return items.get( rule );
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final Map<ValidationRule, List<Item>> items = new EnumMap<>( ValidationRule.class );

		private Builder() {
			for ( ValidationRule rule : ValidationRule.values() ) {
				items.put( rule, new ArrayList<>() );
			}
		}

		public Builder add(
				ValidationRule rule,
				String sourceElementId,
				String targetElementId,
				ClassificationModel.Category sourceCategory,
				ClassificationModel.Category targetCategory,
				String edgeKind,
				Collection<ClassificationModel.Role> roles,
				Collection<String> path,
				String message) {
			if ( rule != ValidationRule.CLS005
					&& rule != ValidationRule.SPI003
					&& rule != ValidationRule.SPI004
					&& rule != ValidationRule.SPI005 ) {
				throw new IllegalArgumentException( "Rule " + rule + " is derived from compiled classification metadata" );
			}
			items.get( rule ).add(
					new Item(
							rule,
							sourceElementId,
							targetElementId,
							sourceCategory,
							targetCategory,
							edgeKind,
							roles,
							path,
							message
					)
			);
			return this;
		}

		public ValidationEvidence build() {
			final Map<ValidationRule, List<Item>> copy = new EnumMap<>( ValidationRule.class );
			for ( Map.Entry<ValidationRule, List<Item>> entry : items.entrySet() ) {
				final List<Item> values = new ArrayList<>( entry.getValue() );
				values.sort( Item.ORDERING );
				copy.put( entry.getKey(), Collections.unmodifiableList( values ) );
			}
			return new ValidationEvidence( Collections.unmodifiableMap( copy ) );
		}
	}

	public static final class Item {
		private static final Comparator<Item> ORDERING = Comparator
				.comparing( (Item item) -> item.sourceElementId )
				.thenComparing( (item) -> item.targetElementId )
				.thenComparing( (item) -> item.edgeKind )
				.thenComparing( (item) -> item.message );

		private final ValidationRule rule;
		private final String sourceElementId;
		private final String targetElementId;
		private final ClassificationModel.Category sourceCategory;
		private final ClassificationModel.Category targetCategory;
		private final String edgeKind;
		private final Set<ClassificationModel.Role> roles;
		private final List<String> path;
		private final String message;

		private Item(
				ValidationRule rule,
				String sourceElementId,
				String targetElementId,
				ClassificationModel.Category sourceCategory,
				ClassificationModel.Category targetCategory,
				String edgeKind,
				Collection<ClassificationModel.Role> roles,
				Collection<String> path,
				String message) {
			this.rule = rule;
			this.sourceElementId = required( sourceElementId, "sourceElementId" );
			this.targetElementId = required( targetElementId, "targetElementId" );
			this.sourceCategory = sourceCategory;
			this.targetCategory = targetCategory;
			this.edgeKind = required( edgeKind, "edgeKind" );
			this.roles = roles.isEmpty()
					? Collections.emptySet()
					: Collections.unmodifiableSet( EnumSet.copyOf( roles ) );
			this.path = Collections.unmodifiableList( new ArrayList<>( path ) );
			this.message = required( message, "message" );
		}

		private static String required(String value, String name) {
			if ( value == null || value.isBlank() ) {
				throw new IllegalArgumentException( name + " must not be empty" );
			}
			return value;
		}

		public ValidationRule getRule() {
			return rule;
		}

		public String getSourceElementId() {
			return sourceElementId;
		}

		public String getTargetElementId() {
			return targetElementId;
		}

		public ClassificationModel.Category getSourceCategory() {
			return sourceCategory;
		}

		public ClassificationModel.Category getTargetCategory() {
			return targetCategory;
		}

		public String getEdgeKind() {
			return edgeKind;
		}

		public Set<ClassificationModel.Role> getRoles() {
			return roles;
		}

		public List<String> getPath() {
			return path;
		}

		public String getMessage() {
			return message;
		}
	}
}
