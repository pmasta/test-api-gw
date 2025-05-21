import chromadb

# Połączenie z lokalnym serwerem ChromaDB
client = chromadb.HttpClient(host="localhost", port=8000)

# Utwórz kolekcję (lub pobierz, jeśli już istnieje)
collection = client.get_or_create_collection("demo_collection")

# Dodaj dokumenty z metadanymi
collection.add(
    documents=[
        "Bankomat wypłaca gotówkę po wpisaniu poprawnego PIN-u.",
        "Transakcje są logowane i widoczne w historii konta.",
        "System obsługuje wiele typów kont, w tym oszczędnościowe."
    ],
    ids=["doc1", "doc2", "doc3"],
    metadatas=[
        {"plik": "atm.py"},
        {"plik": "transactions.py"},
        {"plik": "accounts.py"}
    ]
)

print("✅ Dokumenty dodane!")


  # Zapytanie do kolekcji
results = collection.query(
    query_texts=["Jak działa wypłata pieniędzy z bankomatu?"],
    n_results=2
)

print("\n📄 Wyniki wyszukiwania:")
for i, doc in enumerate(results["documents"][0]):
    print(f"{i+1}. {doc}")
