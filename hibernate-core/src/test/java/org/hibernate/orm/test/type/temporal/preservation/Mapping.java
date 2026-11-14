package org.hibernate.orm.test.type.temporal.preservation;

/// Independent mapping expectations, never derived from a preservation report.
///
/// @author Steve Ebersole
record Mapping<T>(String id, Class<? extends PreservationRow<T>> entity,
		Representation representation, String adjustment, int precision,
		boolean explicitFallback, boolean unknown) {
	boolean column() { return representation == Representation.COLUMN; }
	boolean adjusted() { return adjustment != null && !adjustment.equals( "<none>" ); }
}

enum Representation { DEFAULT, INLINE, COLUMN }
enum Handling { NATURAL, DIRECT_PLAIN, DIRECT_ZONED, FALLBACK_PLAIN, FALLBACK_ZONED }
