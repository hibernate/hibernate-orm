@ElementCollection
@OrderColumn(name = "order_id")
private List<String> phones = new ArrayList<>();

person.getPhones().remove(0);