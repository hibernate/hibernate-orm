package org.hibernate.jpamodelgen.test.hrPanache;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.List;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;

@Entity
public class PanacheBook extends PanacheEntity {
	public @Id String isbn;
    public @NaturalId String title;
    public @NaturalId String author;
    public String text;
    public int pages;
    
    @Find
    public static native Uni<List<PanacheBook>> findBook(String isbn);

    @HQL("WHERE isbn = :isbn")
    public static native Uni<List<PanacheBook>> hqlBook(String isbn);
}
