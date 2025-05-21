import asyncio
import chromadb

async def main():
    client = chromadb.AsyncHttpClient(base_url="http://localhost:9000")
    client._api._base_url = "http://localhost:9000"  # wymuszenie, jeśli trzeba

    await client.heartbeat()

    collection = await client.get_or_create_collection("async_demo")
    await collection.add(documents=["Async test!"], ids=["doc1"])
    
    results = await collection.query(query_texts=["test"], n_results=1)
    print("Wynik:", results["documents"][0][0])

asyncio.run(main())
