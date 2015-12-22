@OneToMany(
    mappedBy = "person",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
private List<Phone> phones = new ArrayList<>();