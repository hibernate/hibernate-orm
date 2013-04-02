package org.hibernate.ejb.criteria.jpaMapMode;

/**
 * A primitive representation of a relationship. The role names are critical, as they are realized by
 * properties for Hibernate to query and/or join.
 */
public class Relationship {
    private final String name;
    private final Document from;
    private final Multiplicity fromMultiplicity;
    private final String fromDocumentRoleName;
    private final Document to;
    private final Multiplicity toMultiplicity;
    private final String toDocumentRoleName;

    public static enum Side {
        FROM, TO;

        public Side oppositeSide() {
            return this == FROM ? TO : FROM;
        }
    }

    public Relationship(String name, Document from,
                        Multiplicity fromMultiplicity, String fromDocumentRoleName,
                        Document to, Multiplicity toMultiplicity, String toDocumentRoleName) {
        super();
        this.name = name;
        this.from = from;
        this.fromMultiplicity = fromMultiplicity;
        this.fromDocumentRoleName = fromDocumentRoleName;
        this.to = to;
        this.toMultiplicity = toMultiplicity;
        this.toDocumentRoleName = toDocumentRoleName;
    }

    public String getName() {
        return name;
    }

    public Document getDocument(Side side) {
        return side == Side.FROM ? from : to;
    }

    public Document getFrom() {
        return from;
    }

    public Multiplicity getMultiplicity(Side side) {
        return side == Side.FROM ? fromMultiplicity : toMultiplicity;
    }

    public Multiplicity getFromMultiplicity() {
        return fromMultiplicity;
    }

    public String getRoleName(Side side) {
        return side == Side.FROM ? fromDocumentRoleName : toDocumentRoleName;
    }

    public String getFromDocumentRoleName() {
        return fromDocumentRoleName;
    }

    public Document getTo() {
        return to;
    }

    public Multiplicity getToMultiplicity() {
        return toMultiplicity;
    }

    public String getToDocumentRoleName() {
        return toDocumentRoleName;
    }
}