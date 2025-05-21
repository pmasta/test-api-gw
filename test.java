import asyncio
import chromadb
from chromadb.config import Settings

async def main():
    client = await chromadb.AsyncHttpClient(
        host="localhost",
        port=9000,
        ssl=False,
        settings=Settings()
    )

    # Create or get collection (works in latest versions)
    collection = await client.get_or_create_collection(name="test_collection")

    # Add documents
    await collection.add(
        documents=["This is a test document."],
        ids=["doc1"]
    )

    # Query the collection
    results = await collection.query(
        query_texts=["test"],
        n_results=1
    )

    print("Query result:", results["documents"][0][0])

asyncio.run(main())
