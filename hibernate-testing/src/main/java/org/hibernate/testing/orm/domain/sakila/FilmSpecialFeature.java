/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.sakila;

import java.util.Locale;

/**
 * @author Nathan Xu
 */
public enum FilmSpecialFeature {
	TRAILERS("Trailers"),
	COMMENTARIES("Commentaries"),
	DELETED_SCENES("Deleted Scenes"),
	BEHIND_THE_SCENES("Behind the Scenes");

	private final String text;
	FilmSpecialFeature(String text) {
		this.text = text;
	}

	public static FilmSpecialFeature fromText(String text) {
		switch ( text.toLowerCase( Locale.ROOT ) ) {
			case "trailers":
				return TRAILERS;
			case "commentaries":
				return COMMENTARIES;
			case "deleted scenes":
				return DELETED_SCENES;
			case "behind the scenes":
				return BEHIND_THE_SCENES;
			default:
				throw new IllegalArgumentException( "unknown FilmSpecialFeature text: " + text);
		}
	}

	@Override
	public String toString() {
		return text;
	}
}
